package tools.refinery.language.model.tests.utils;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EcoreUtil;

import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.EnumDeclaration;
import tools.refinery.language.model.problem.IndividualDeclaration;
import tools.refinery.language.model.problem.NamedElement;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeValueAssertion;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.RuleDefinition;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.language.utils.ProblemDesugarer;

public record WrappedProblem(Problem problem) {
	public Problem get() {
		return problem;
	}

	public List<Diagnostic> errors() {
		EcoreUtil.resolveAll(problem);
		return problem.eResource().getErrors();
	}

	public WrappedProblem builtin() {
		return new WrappedProblem(new ProblemDesugarer().getBuiltinProblem(problem).orElseThrow());
	}

	public BuiltinSymbols builtinSymbols() {
		return new ProblemDesugarer().getBuiltinSymbols(problem).orElseThrow();
	}

	public List<String> nodeNames() {
		return problem.getNodes().stream().map(Node::getName).toList();
	}

	public WrappedPredicateDefinition pred(String name) {
		return new WrappedPredicateDefinition(namedStatementOfType(PredicateDefinition.class, name));
	}

	public WrappedRuleDefinition rule(String name) {
		return new WrappedRuleDefinition(namedStatementOfType(RuleDefinition.class, name));
	}

	public WrappedClassDeclaration findClass(String name) {
		return new WrappedClassDeclaration(namedStatementOfType(ClassDeclaration.class, name));
	}

	public WrappedEnumDeclaration findEnum(String name) {
		return new WrappedEnumDeclaration(namedStatementOfType(EnumDeclaration.class, name));
	}

	public WrappedAssertion assertion(int i) {
		return new WrappedAssertion(nthStatementOfType(Assertion.class, i));
	}

	public Node node(String name) {
		return ProblemNavigationUtil.named(problem.getNodes(), name);
	}

	public Node individualNode(String name) {
		var uniqueNodes = statementsOfType(IndividualDeclaration.class)
				.flatMap(declaration -> declaration.getNodes().stream());
		return ProblemNavigationUtil.named(uniqueNodes, name);
	}

	public NodeValueAssertion nodeValueAssertion(int i) {
		return nthStatementOfType(NodeValueAssertion.class, i);
	}

	private <T extends Statement> Stream<T> statementsOfType(Class<? extends T> type) {
		return problem.getStatements().stream().filter(type::isInstance).map(type::cast);
	}

	private <T extends Statement & NamedElement> T namedStatementOfType(Class<? extends T> type, String name) {
		return ProblemNavigationUtil.named(statementsOfType(type), name);
	}

	private <T extends Statement> T nthStatementOfType(Class<? extends T> type, int n) {
		return statementsOfType(type).skip(n).findFirst().orElseThrow();
	}
}
