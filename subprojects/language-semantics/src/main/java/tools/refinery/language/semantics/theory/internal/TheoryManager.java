/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.theory.internal;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import tools.refinery.language.annotations.Annotations;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.TheoryAction;
import tools.refinery.language.model.problem.TheoryDeclaration;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.semantics.theory.TheoryProvider;
import tools.refinery.language.utils.BuiltinAnnotationContext;
import tools.refinery.language.utils.ServiceUtil;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.theory.Theory;
import tools.refinery.store.reasoning.theory.TheoryRule;
import tools.refinery.store.reasoning.theory.TheorySupport;

import java.util.*;

public class TheoryManager {
	private static final List<Class<? extends TheoryProvider>> DEFAULT_THEORY_PROVIDERS =
			ServiceUtil.loadServices(TheoryProvider.class);

	private final List<TheoryProvider> theoryProviders;
	private final IQualifiedNameProvider qualifiedNameProvider;
	private final BuiltinAnnotationContext builtinAnnotationContext;
	private Annotations annotations;
	private ProblemTrace trace;
	private final Map<TheoryDeclaration, ManagedTheory> theories = new LinkedHashMap<>();

	@Inject
	public TheoryManager(Injector injector, IQualifiedNameProvider qualifiedNameProvider,
						 BuiltinAnnotationContext builtinAnnotationContext) {
		this.qualifiedNameProvider = qualifiedNameProvider;
		theoryProviders = ServiceUtil.instantiate(injector, DEFAULT_THEORY_PROVIDERS);
		this.builtinAnnotationContext = builtinAnnotationContext;
	}

	public List<TheoryProvider> getTheoryProviders() {
		return theoryProviders;
	}

	public void initialize(Annotations annotations, ProblemTrace trace, Collection<Problem> importedProblems) {
		this.annotations = annotations;
		this.trace = trace;
		theories.clear();
		for (var problem : importedProblems) {
			for (var statement : problem.getStatements()) {
				if (statement instanceof TheoryDeclaration theoryDeclaration) {
					var theory = getTheory(theoryDeclaration);
					int priority = builtinAnnotationContext.getPriority(theoryDeclaration);
					theories.put(theoryDeclaration, new ManagedTheory(theoryDeclaration, theory, priority));
				}
			}
		}
	}

	private Theory getTheory(TheoryDeclaration theoryDeclaration) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(theoryDeclaration);
		if (qualifiedName == null) {
			throw new TracedException(theoryDeclaration, "No qualified name for theory.");
		}
		try {
			for (var theoryProvider : theoryProviders) {
				var result = theoryProvider.createTheory(qualifiedName, annotations, trace);
				if (result.isPresent()) {
					return result.get();
				}
			}
		} catch (RuntimeException e) {
			throw new TracedException(theoryDeclaration, "Error while instantiating theory.", e);
		}
		throw new TracedException(theoryDeclaration, "No theory registered for theory declaration '%s'."
				.formatted(theoryDeclaration.getName()));
	}

	public void addRule(TheoryAction action, TheoryRule rule) {
		if (action.isTheoryOverride()) {
			addRuleWithTheoryOverride(action, rule);
			return;
		}
		addRuleWithDefaultTheories(action, rule);
	}

	private void addRuleWithTheoryOverride(TheoryAction action, TheoryRule rule) {
		for (var theory : action.getTheories()) {
			if (theory == null || theory.eIsProxy()) {
				throw new TracedException(action, "Unresolved reference to theory.");
			}
			var managedTheory = theories.get(theory);
			if (managedTheory == null) {
				throw new TracedException(action, "Unknown theory '%s'.".formatted(theory.getName()));
			}
			var support = managedTheory.checkSupport(rule);
			if (!support.isSupported()) {
				throw new TracedException(action, "Theory '%s' does not support this assertion."
						.formatted(theory.getName()));
			}
			managedTheory.addRule(rule);
		}
	}

	private void addRuleWithDefaultTheories(TheoryAction action, TheoryRule rule) {
		int maxPriority = Integer.MIN_VALUE;
		var matchedTheories = new ArrayList<ManagedTheory>();
		boolean supported = false;
		for (var managedTheory : theories.values()) {
			var support = managedTheory.checkSupport(rule);
			supported = supported || support.isSupported();
			if (support.isEnabledByDefault()) {
				int priority = managedTheory.getPriority();
				if (priority < maxPriority) {
					continue;
				}
				if (priority > maxPriority) {
					maxPriority = priority;
					matchedTheories.clear();
				}
				matchedTheories.add(managedTheory);
			}
		}
		if (matchedTheories.isEmpty()) {
			if (supported) {
				throw new TracedException(action, "No default theory is available for this assertion. " +
						"Use the 'using' keyword to specify the intended theories.");
			}
			throw new TracedException(action, "No imported theory supports this assertion.");
		}
		for (var managedTheory : matchedTheories) {
			managedTheory.addRule(rule);
		}
	}

	public void configure(ModelStoreBuilder storeBuilder) {
		for (var managedTheory : theories.values()) {
			managedTheory.configure(storeBuilder);
		}
	}

	private final static class ManagedTheory {
		private final TheoryDeclaration declaration;
		private final Theory theory;
		private final int priority;
		private final List<TheoryRule> rules;

		public ManagedTheory(TheoryDeclaration declaration, Theory theory, int priority) {
			this.declaration = declaration;
			this.theory = theory;
			this.priority = priority;
			rules = new ArrayList<>();
		}

		public int getPriority() {
			return priority;
		}

		public TheorySupport checkSupport(TheoryRule rule) {
			return theory.checkSupport(rule);
		}

		public void addRule(TheoryRule rule) {
			rules.add(rule);
		}

		public void configure(ModelStoreBuilder storeBuilder) {
			if (rules.isEmpty()) {
				return;
			}
			try {
				theory.createPropagator(storeBuilder, rules);
			} catch (RuntimeException e) {
				throw new TracedException(declaration, "Error while creating propagator.", e);
			}
		}
	}
}
