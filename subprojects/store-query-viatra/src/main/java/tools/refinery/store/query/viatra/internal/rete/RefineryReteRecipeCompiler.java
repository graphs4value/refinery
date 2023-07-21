/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.query.viatra.internal.rete;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendHintProvider;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryCacheContext;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.planning.IQueryPlannerStrategy;
import org.eclipse.viatra.query.runtime.matchers.planning.SubPlan;
import org.eclipse.viatra.query.runtime.matchers.planning.operations.PApply;
import org.eclipse.viatra.query.runtime.matchers.planning.operations.PEnumerate;
import org.eclipse.viatra.query.runtime.matchers.psystem.EnumerablePConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.analysis.QueryAnalyzer;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.PDisjunctionRewriterCacher;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.rete.construction.plancompiler.CompilerHelper;
import org.eclipse.viatra.query.runtime.rete.construction.plancompiler.ReteRecipeCompiler;
import org.eclipse.viatra.query.runtime.rete.matcher.TimelyConfiguration;
import org.eclipse.viatra.query.runtime.rete.recipes.ReteNodeRecipe;
import org.eclipse.viatra.query.runtime.rete.traceability.CompiledSubPlan;
import org.eclipse.viatra.query.runtime.rete.traceability.PlanningTrace;
import org.eclipse.viatra.query.runtime.rete.util.ReteHintOptions;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.viatra.internal.pquery.RepresentativeElectionConstraint;
import tools.refinery.store.query.viatra.internal.rete.recipe.RefineryRecipesFactory;
import tools.refinery.store.query.viatra.internal.pquery.rewriter.RefineryPBodyNormalizer;
import tools.refinery.store.query.viatra.internal.pquery.rewriter.RefinerySurrogateQueryRewriter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Map;

// Since we don't modify VIATRA code, this is our last resort.
@SuppressWarnings("squid:S3011")
public class RefineryReteRecipeCompiler extends ReteRecipeCompiler {
	private static final MethodHandle GET_SUB_PLAN_COMPILER_CACHE;
	private static final MethodHandle GET_COMPILER_BACK_TRACE;
	private static final Field NORMALIZER_FIELD;
	private static final MethodHandle DO_COMPILE_DISPATCH;
	private static final MethodHandle COMPILE_TO_NATURAL_JOIN;
	private static final MethodHandle REFER_QUERY;

	static {
		MethodHandles.Lookup lookup;
		try {
			lookup = MethodHandles.privateLookupIn(ReteRecipeCompiler.class, MethodHandles.lookup());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Failed to create lookup", e);
		}
		try {
			GET_SUB_PLAN_COMPILER_CACHE = lookup.findGetter(ReteRecipeCompiler.class, "subPlanCompilerCache",
					Map.class);
			GET_COMPILER_BACK_TRACE = lookup.findGetter(ReteRecipeCompiler.class, "compilerBackTrace", Map.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException("Failed to find getter", e);
		}

		try {
			NORMALIZER_FIELD = ReteRecipeCompiler.class.getDeclaredField("normalizer");
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException("Failed to find field", e);
		}
		NORMALIZER_FIELD.setAccessible(true);

		try {
			DO_COMPILE_DISPATCH = lookup.findVirtual(ReteRecipeCompiler.class, "doCompileDispatch",
					MethodType.methodType(CompiledSubPlan.class, SubPlan.class));
			COMPILE_TO_NATURAL_JOIN = lookup.findVirtual(ReteRecipeCompiler.class, "compileToNaturalJoin",
					MethodType.methodType(CompiledSubPlan.class, SubPlan.class, PlanningTrace.class,
							PlanningTrace.class));
			REFER_QUERY = lookup.findVirtual(ReteRecipeCompiler.class, "referQuery",
					MethodType.methodType(PlanningTrace.class, PQuery.class, SubPlan.class, Tuple.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException("Failed to find method", e);
		}
	}

	private final Map<SubPlan, CompiledSubPlan> subPlanCompilerCache;
	private final Map<ReteNodeRecipe, SubPlan> compilerBackTrace;

	public RefineryReteRecipeCompiler(IQueryPlannerStrategy plannerStrategy, Logger logger,
									  IQueryMetaContext metaContext, IQueryCacheContext queryCacheContext,
									  IQueryBackendHintProvider hintProvider, QueryAnalyzer queryAnalyzer,
									  boolean deleteAndReDeriveEvaluation, TimelyConfiguration timelyEvaluation) {
		super(plannerStrategy, logger, metaContext, queryCacheContext, hintProvider, queryAnalyzer,
				deleteAndReDeriveEvaluation, timelyEvaluation);

		var normalizer = new PDisjunctionRewriterCacher(new RefinerySurrogateQueryRewriter(),
				new RefineryPBodyNormalizer(metaContext) {

					@Override
					protected boolean shouldExpandWeakenedAlternatives(PQuery query) {
						var hint = hintProvider.getQueryEvaluationHint(query);
						return ReteHintOptions.expandWeakenedAlternativeConstraints.getValueOrDefault(hint);
					}

				});
		try {
			// https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.5.3
			// "The object should not be made visible to other threads, nor should the final fields be read,
			// until all updates to the final fields of the object are complete."
			// The {@code super} constructor only sets but doesn't read the {@code normalizer} field,
			// therefore this is fine.
			NORMALIZER_FIELD.set(this, normalizer);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Failed to set private final field", e);
		}

		try {
			@SuppressWarnings("unchecked")
			var cache = (Map<SubPlan, CompiledSubPlan>) GET_SUB_PLAN_COMPILER_CACHE.invokeExact(
					(ReteRecipeCompiler) this);
			subPlanCompilerCache = cache;
			@SuppressWarnings("unchecked")
			var backTrace = (Map<ReteNodeRecipe, SubPlan>) GET_COMPILER_BACK_TRACE.invokeExact(
					(ReteRecipeCompiler) this);
			compilerBackTrace = backTrace;
		} catch (Error e) {
			// Fatal JVM errors should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Failed to access private fields", e);
		}
	}

	@Override
	public CompiledSubPlan getCompiledForm(SubPlan plan) {
		CompiledSubPlan compiled = subPlanCompilerCache.get(plan);
		if (compiled == null) {
			compiled = doCompileDispatchExtension(plan);
			if (compiled == null) {
				compiled = superDoCompileDispatch(plan);
			}
			subPlanCompilerCache.put(plan, compiled);
			compilerBackTrace.put(compiled.getRecipe(), plan);
		}
		return compiled;
	}

	@Nullable
	private CompiledSubPlan doCompileDispatchExtension(SubPlan plan) {
		var operation = plan.getOperation();
		if (operation instanceof PEnumerate enumerateOperation) {
			return doCompileEnumerateExtension(enumerateOperation.getEnumerablePConstraint(), plan);
		} else if (operation instanceof PApply applyOperation &&
				applyOperation.getPConstraint() instanceof EnumerablePConstraint constraint) {
			var secondaryParent = doEnumerateDispatchExtension(plan, constraint);
			if (secondaryParent != null) {
				var primaryParent = getCompiledForm(plan.getParentPlans().get(0));
				return superCompileToNaturalJoin(plan, primaryParent, secondaryParent);
			}
		}
		return null;
	}

	@Nullable
	private CompiledSubPlan doCompileEnumerateExtension(EnumerablePConstraint constraint, SubPlan plan) {
		var coreTrace = doEnumerateDispatchExtension(plan, constraint);
		if (coreTrace == null) {
			return null;
		}
		var trimmedTrace = CompilerHelper.checkAndTrimEqualVariables(plan, coreTrace);
		return trimmedTrace.cloneFor(plan);
	}

	@Nullable
	private PlanningTrace doEnumerateDispatchExtension(SubPlan plan, EnumerablePConstraint constraint) {
		if (constraint instanceof RepresentativeElectionConstraint representativeElectionConstraint) {
			return compileEnumerableExtension(plan, representativeElectionConstraint);
		}
		return null;
	}

	private PlanningTrace compileEnumerableExtension(SubPlan plan, RepresentativeElectionConstraint constraint) {
		var referredQuery = constraint.getSupplierKey();
		var callTrace = superReferQuery(referredQuery, plan, constraint.getVariablesTuple());
		var recipe = RefineryRecipesFactory.eINSTANCE.createRepresentativeElectionRecipe();
		recipe.setParent(callTrace.getRecipe());
		recipe.setConnectivity(constraint.getConnectivity());
		return new PlanningTrace(plan, CompilerHelper.convertVariablesTuple(constraint), recipe, callTrace);
	}

	private CompiledSubPlan superDoCompileDispatch(SubPlan plan) {
		try {
			return (CompiledSubPlan) DO_COMPILE_DISPATCH.invokeExact((ReteRecipeCompiler) this, plan);
		} catch (Error | RuntimeException e) {
			// Fatal JVM errors and runtime exceptions should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Failed to call doCompileDispatch", e);
		}
	}

	private CompiledSubPlan superCompileToNaturalJoin(SubPlan plan, PlanningTrace leftCompiled,
													  PlanningTrace rightCompiled) {
		try {
			return (CompiledSubPlan) COMPILE_TO_NATURAL_JOIN.invokeExact((ReteRecipeCompiler) this, plan,
					leftCompiled, rightCompiled);
		} catch (Error | RuntimeException e) {
			// Fatal JVM errors and runtime exceptions should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Failed to call compileToNaturalJoin", e);
		}
	}

	private PlanningTrace superReferQuery(PQuery query, SubPlan plan, Tuple actualParametersTuple) {
		try {
			return (PlanningTrace) REFER_QUERY.invokeExact((ReteRecipeCompiler) this, query, plan,
					actualParametersTuple);
		} catch (Error | RuntimeException e) {
			// Fatal JVM errors and runtime exceptions should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Failed to call referQuery", e);
		}
	}
}
