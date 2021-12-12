package tools.refinery.language.model.tests;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EcoreUtil;

import tools.refinery.language.model.ProblemUtil;
import tools.refinery.language.model.problem.ActionLiteral;
import tools.refinery.language.model.problem.Argument;
import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.Conjunction;
import tools.refinery.language.model.problem.DeleteActionLiteral;
import tools.refinery.language.model.problem.EnumDeclaration;
import tools.refinery.language.model.problem.IndividualDeclaration;
import tools.refinery.language.model.problem.Literal;
import tools.refinery.language.model.problem.NamedElement;
import tools.refinery.language.model.problem.NegativeLiteral;
import tools.refinery.language.model.problem.NewActionLiteral;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeAssertionArgument;
import tools.refinery.language.model.problem.NodeValueAssertion;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.ParametricDefinition;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ReferenceDeclaration;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.RuleDefinition;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.language.model.problem.ValueActionLiteral;
import tools.refinery.language.model.problem.ValueLiteral;
import tools.refinery.language.model.problem.Variable;
import tools.refinery.language.model.problem.VariableOrNode;
import tools.refinery.language.model.problem.VariableOrNodeArgument;

public class ProblemTestUtil {
	public Problem builtin(Problem problem) {
		return ProblemUtil.getBuiltInLibrary(problem).get();
	}

	public List<Diagnostic> errors(Problem problem) {
		EcoreUtil.resolveAll(problem);
		return problem.eResource().getErrors();
	}

	public List<String> nodeNames(Problem problem) {
		return problem.getNodes().stream().map(node -> node.getName()).toList();
	}

	public PredicateDefinition pred(Problem problem, String name) {
		return namedStatementOfType(problem, PredicateDefinition.class, name);
	}

	public RuleDefinition rule(Problem problem, String name) {
		return namedStatementOfType(problem, RuleDefinition.class, name);
	}

	public Parameter param(ParametricDefinition definition, int i) {
		return definition.getParameters().get(i);
	}

	public Conjunction conj(ParametricDefinition definition, int i) {
		return definition.getBodies().get(i);
	}

	public Literal lit(Conjunction conjunction, int i) {
		return conjunction.getLiterals().get(i);
	}
	
	public ActionLiteral actionLit(RuleDefinition rule, int i) {
		return rule.getAction().getActionLiterals().get(i);
	}

	public Atom valueAtom(Literal literal) {
		return ((ValueLiteral) literal).getAtom();
	}

	public Atom negated(Literal literal) {
		return ((NegativeLiteral) literal).getAtom();
	}

	public Relation relation(Literal literal) {
		return ((Atom) literal).getRelation();
	}

	public Argument arg(Atom atom, int i) {
		return atom.getArguments().get(i);
	}

	public Argument arg(Literal literal, int i) {
		return arg((Atom) literal, i);
	}

	public VariableOrNode variableOrNode(Argument argument) {
		return ((VariableOrNodeArgument) argument).getVariableOrNode();
	}

	public Variable variable(Argument argument) {
		return (Variable) variableOrNode(argument);
	}

	public Variable variable(ValueActionLiteral valueActionLiteral, int i) {
		return variable(arg(valueActionLiteral.getAtom(), i));
	}
	
	public Variable variable(NewActionLiteral newActionLiteral) {
		return newActionLiteral.getVariable();
	}
	
	public VariableOrNode deleteVar(ActionLiteral actionLiteral) {
		return ((DeleteActionLiteral) actionLiteral).getVariableOrNode();
	}
	
	public VariableOrNode newVar(ActionLiteral actionLiteral) {
		return ((NewActionLiteral) actionLiteral).getVariable();
	}
	
	public Atom valueAtom(ActionLiteral actionLiteral) {
		return ((ValueActionLiteral) actionLiteral).getAtom();
	}
	
	public Variable variable(DeleteActionLiteral deleteActionLiteral) {
		return (Variable) deleteActionLiteral.getVariableOrNode();
	}

	public Node node(Argument argument) {
		return (Node) variableOrNode(argument);
	}

	public Assertion assertion(Problem problem, int i) {
		return nthStatementOfType(problem, Assertion.class, i);
	}

	public AssertionArgument arg(Assertion assertion, int i) {
		return assertion.getArguments().get(i);
	}

	public Node node(AssertionArgument argument) {
		return ((NodeAssertionArgument) argument).getNode();
	}

	public Node node(Problem problem, String name) {
		return named(problem.getNodes(), name);
	}

	public Node individualNode(Problem problem, String name) {
		var uniqueNodes = statementsOfType(problem, IndividualDeclaration.class)
				.flatMap(declaration -> declaration.getNodes().stream());
		return named(uniqueNodes, name);
	}

	public NodeValueAssertion nodeValueAssertion(Problem problem, int i) {
		return nthStatementOfType(problem, NodeValueAssertion.class, i);
	}

	public ClassDeclaration findClass(Problem problem, String name) {
		return namedStatementOfType(problem, ClassDeclaration.class, name);
	}

	public ReferenceDeclaration reference(ClassDeclaration declaration, String name) {
		return named(declaration.getReferenceDeclarations(), name);
	}

	public EnumDeclaration findEnum(Problem problem, String name) {
		return namedStatementOfType(problem, EnumDeclaration.class, name);
	}

	public Node literal(EnumDeclaration declaration, String name) {
		return named(declaration.getLiterals(), name);
	}

	private <T extends NamedElement> T named(Stream<? extends T> stream, String name) {
		return stream.filter(statement -> name.equals(statement.getName())).findAny().get();
	}

	private <T extends NamedElement> T named(List<? extends T> list, String name) {
		return named(list.stream(), name);
	}

	private <T extends Statement> Stream<T> statementsOfType(Problem problem, Class<? extends T> type) {
		return problem.getStatements().stream().filter(type::isInstance).map(type::cast);
	}

	private <T extends Statement & NamedElement> T namedStatementOfType(Problem problem, Class<? extends T> type,
			String name) {
		return named(statementsOfType(problem, type), name);
	}

	private <T extends Statement> T nthStatementOfType(Problem problem, Class<? extends T> type, int n) {
		return statementsOfType(problem, type).skip(n).findFirst().get();
	}
}
