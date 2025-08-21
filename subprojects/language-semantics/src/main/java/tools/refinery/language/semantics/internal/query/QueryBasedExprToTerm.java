/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.expressions.BuiltInTerms;
import tools.refinery.language.expressions.ExprToTerm;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.typesystem.DataExprType;
import tools.refinery.language.typesystem.SignatureProvider;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.*;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.PartialAggregationTerm;
import tools.refinery.store.reasoning.literal.PartialCountTerm;
import tools.refinery.store.reasoning.literal.ReifyTerm;

import java.util.*;
import java.util.stream.Collectors;

class QueryBasedExprToTerm extends ExprToTerm {
	@Inject
	private Provider<QueryBasedExprToTerm> exprToTermProvider;

	@Inject
	private SignatureProvider signatureProvider;

	private QueryCompiler queryCompiler;

	private ProblemTrace problemTrace;

	private Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope;

	private List<Literal> literals;

	public void setLiterals(List<Literal> literals) {
		this.literals = literals;
	}

	public void setLocalScope(Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope) {
		this.localScope = localScope;
	}

	public void setProblemTrace(ProblemTrace problemTrace) {
		this.problemTrace = problemTrace;
	}

	public void setQueryCompiler(QueryCompiler queryCompiler) {
		this.queryCompiler = queryCompiler;
	}

	public Optional<AnyTerm> toTerm(Expr expr) {
		return switch (expr) {
			case Atom atom -> createPartialFunctionCall(atom);
			case VariableOrNodeExpr variableOrNodeExpr -> createVariableReference(variableOrNodeExpr);
			case ModalExpr modalExpr -> createModalOperator(modalExpr);
			case AggregationExpr aggregationExpr -> createAggregation(aggregationExpr);
			case null, default -> super.toTerm(expr);
		};
	}

	private Optional<AnyTerm> createPartialFunctionCall(Atom atom) {
		var result = createAtom(atom);
		if (result.isPresent()) {
			return result;
		}
		return createPartialFunctionCall(atom, ConcretenessSpecification.UNSPECIFIED);
	}

	private Optional<AnyTerm> createPartialFunctionCall(Atom atom, ConcretenessSpecification concreteness) {
		var argumentList = queryCompiler.toArgumentList(atom, atom.getArguments(), localScope, literals);
		var partialFunction = problemTrace.getPartialFunction(atom.getRelation());
		return Optional.of(partialFunction.call(concreteness, argumentList.arguments()));
	}

	private @NotNull Optional<AnyTerm> createVariableReference(VariableOrNodeExpr variableOrNodeExpr) {
		if (variableOrNodeExpr.getVariableOrNode() instanceof
				tools.refinery.language.model.problem.Variable problemVariable &&
				localScope.get(problemVariable) instanceof AnyDataVariable variable) {
			return Optional.of(variable);
		} else {
			return Optional.empty();
		}
	}

	private Optional<AnyTerm> createModalOperator(ModalExpr expr) {
		var concreteness = expr.getConcreteness();
		if (concreteness == Concreteness.UNSPECIFIED) {
			return super.toTerm(expr);
		}
		var concretenessSpecification = switch (concreteness) {
			case PARTIAL -> ConcretenessSpecification.PARTIAL;
			case CANDIDATE -> ConcretenessSpecification.CANDIDATE;
			default -> throw new IllegalArgumentException("Unsupported concreteness: " + concreteness);
		};
		Optional<AnyTerm> result;
		var body = expr.getBody();
		if (body instanceof Atom atom) {
			result = createPartialFunctionCall(atom, concretenessSpecification);
		} else if (body instanceof NegationExpr negationExpr && negationExpr.getBody() instanceof Atom atom) {
			result = createPartialFunctionCall(atom, concretenessSpecification)
					.map(callTerm -> TruthValueTerms.not(callTerm.asType(TruthValue.class)));
		} else {
			// Concreteness specifications must be applied to partial function calls directly.
			return Optional.empty();
		}
		return result.map(callTerm -> wrapModality(callTerm, expr.getModality()));
	}

	private Optional<AnyTerm> createAggregation(AggregationExpr expr) {
		// Turn the condition into a DNF clause that binds all nodes in the argument list to variables.
		var extracted = ExtractedModalExpr.of(expr.getCondition());
		if (!(extracted.body() instanceof Atom atom)) {
			return Optional.empty();
		}
		var originalConstraint = extracted.modality().wrapConstraint(queryCompiler.getConstraint(atom));
		var quantifiedScope = new LinkedHashMap<tools.refinery.language.model.problem.Variable, Variable>();
		var arguments = new ArrayList<Variable>();
		var parameters = new LinkedHashSet<Variable>();
		var constraintLiterals = new ArrayList<Literal>();
		boolean exactMatch = true;
		for (var problemArgument : atom.getArguments()) {
			if (!(problemArgument instanceof VariableOrNodeExpr variableOrNodeExpr)) {
				return Optional.empty();
			}
			switch (variableOrNodeExpr.getVariableOrNode()) {
			case Node node -> {
				var variable = queryCompiler.createTempVariableForNode(node, constraintLiterals);
				arguments.add(variable);
				exactMatch = false;
			}
			case tools.refinery.language.model.problem.Variable problemVariable -> {
				var localVariable = localScope.get(problemVariable);
				var variable = localVariable == null ? Variable.of(problemVariable.getName()) : localVariable;
				quantifiedScope.put(problemVariable, variable);
				arguments.add(variable);
				if (!parameters.add(variable)) {
					exactMatch = false;
				}
			}
			case null, default -> {
				return Optional.empty();
			}
			}
		}
		constraintLiterals.add(originalConstraint.call(CallPolarity.POSITIVE, arguments));

		// Turn the value into a term and extend the condition DNF clause to bind the nodes occurring in the value
		// expression to variables.
		var value = expr.getValue();
		AnyTerm term;
		if (value == null) {
			term = null;
		} else {
			var exprToTerm = exprToTermProvider.get();
			exprToTerm.setLiterals(constraintLiterals);
			exprToTerm.setLocalScope(quantifiedScope);
			exprToTerm.setProblemTrace(problemTrace);
			exprToTerm.setQueryCompiler(queryCompiler);
			term = exprToTerm.toTerm(value).orElse(null);
			if (term != null) {
				var positiveVariables = literals.stream()
						.flatMap(literal -> literal.getOutputVariables().stream())
						.collect(Collectors.toUnmodifiableSet());
				var inputVariables = term.getInputVariables(positiveVariables);
				// Make all newly bound variables appear in the parameter list of the condition.
				// This preserves match multiplicities, because only atom nodes (with {@code 1..1} count) can be bound.
				if (parameters.addAll(inputVariables)) {
					exactMatch = false;
				}
			}
		}

		// If no new nodes have been bound, and there are no diagonal constraints, use the original condition as-is.
		// Otherwise, we create a new DNF from our literals.
		var constraint = exactMatch ? originalConstraint : Dnf.builder(originalConstraint.name() + "#quantified")
				.parameters(parameters)
				.clause(constraintLiterals)
				.build();
		var parameterList = List.copyOf(parameters);

		// Return the appropriate aggregation term with the condition constraint and the value term.
		var aggregatorDeclaration = expr.getAggregator();
		if (aggregatorDeclaration == null) {
			return Optional.empty();
		}
		var aggregatorName = signatureProvider.getAggregatorName(aggregatorDeclaration);
		if (BuiltInTerms.REIFY_AGGREGATOR.equals(aggregatorName)) {
			return Optional.of(new ReifyTerm(constraint, parameterList));
		}
		if (BuiltInTerms.COUNT_AGGREGATOR.equals(aggregatorName)) {
			return Optional.of(new PartialCountTerm(constraint, parameterList));
		}
		if (term == null) {
			return Optional.empty();
		}
		var valueType = getTypeAnalyzer().getExpressionType(value);
		if (!(valueType instanceof DataExprType dataExprType)) {
			return Optional.empty();
		}
		var aggregatorOption = getImportAdapterProvider().getTermInterpreter(expr)
				.getAggregator(aggregatorName, dataExprType);
		return aggregatorOption.map(aggregator -> createAggregatorTerm(
				(PartialAggregator<?, ?, ?, ?>) aggregator, constraint, parameterList, term));
	}

	private static <A extends AbstractValue<A, C>, C, A2 extends AbstractValue<A2, C2>, C2> AnyTerm createAggregatorTerm(
			PartialAggregator<A, C, A2, C2> aggregator, Constraint target, List<Variable> arguments, AnyTerm term) {
		var typedTerm = term.asType(aggregator.getBodyDomain().abstractType());
		return new PartialAggregationTerm<>(aggregator, target, arguments, typedTerm);
	}
}
