package tools.refinery.language.resource;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IDerivedStateComputer;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.ConstantAssertionArgument;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemFactory;
import tools.refinery.language.model.problem.Statement;

@Singleton
public class ProblemDerivedStateComputer implements IDerivedStateComputer {
	public static final String NEW_NODE = "new";

	public static final String CONSTANT_NODE = "constant";

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	private String languageName;

	@Inject
	@Named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE)
	private IScopeProvider scopeProvider;

	@Inject
	private Provider<NodeNameCollector> nodeNameCollectorProvider;

	@Inject
	private DerivedVariableComputer derivedVariableComputer;

	@Override
	public void installDerivedState(DerivedStateAwareResource resource, boolean preLinkingPhase) {
		var problem = getProblem(resource);
		if (problem != null) {
			var adapter = getOrInstallAdapter(resource);
			installDerivedProblemState(problem, adapter, preLinkingPhase);
		}
	}

	protected Problem getProblem(Resource resource) {
		List<EObject> contents = resource.getContents();
		if (contents.isEmpty()) {
			return null;
		}
		EObject object = contents.get(0);
		if (object instanceof Problem problem) {
			return problem;
		}
		return null;
	}

	protected void installDerivedProblemState(Problem problem, Adapter adapter, boolean preLinkingPhase) {
		installNewNodes(problem, adapter);
		if (preLinkingPhase) {
			return;
		}
		Set<String> nodeNames = installDerivedNodes(problem);
		derivedVariableComputer.installDerivedVariables(problem, nodeNames);
	}

	protected void installNewNodes(Problem problem, Adapter adapter) {
		for (Statement statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration declaration && !declaration.isAbstract()
					&& declaration.getNewNode() == null) {
				var newNode = adapter.createNewNodeIfAbsent(declaration, key -> createNode(NEW_NODE));
				declaration.setNewNode(newNode);
			} else if (statement instanceof Assertion assertion) {
				for (var argument : assertion.getArguments()) {
					if (argument instanceof ConstantAssertionArgument constantAssertionArgument
							&& constantAssertionArgument.getNode() == null) {
						var constantNode = adapter.createConstantNodeIfAbsent(constantAssertionArgument,
								key -> createNode(CONSTANT_NODE));
						constantAssertionArgument.setNode(constantNode);
					}
				}
			}
		}
	}

	protected Set<String> installDerivedNodes(Problem problem) {
		var collector = nodeNameCollectorProvider.get();
		collector.collectNodeNames(problem);
		Set<String> nodeNames = collector.getNodeNames();
		List<Node> grapNodes = problem.getNodes();
		for (String nodeName : nodeNames) {
			var graphNode = createNode(nodeName);
			grapNodes.add(graphNode);
		}
		return nodeNames;
	}

	protected Node createNode(String name) {
		var node = ProblemFactory.eINSTANCE.createNode();
		node.setName(name);
		return node;
	}

	@Override
	public void discardDerivedState(DerivedStateAwareResource resource) {
		var problem = getProblem(resource);
		if (problem != null) {
			var adapter = getOrInstallAdapter(resource);
			discardDerivedProblemState(problem, adapter);
		}
	}

	protected void discardDerivedProblemState(Problem problem, Adapter adapter) {
		Set<ClassDeclaration> classDeclarations = new HashSet<>();
		Set<ConstantAssertionArgument> constantAssertionArguments = new HashSet<>();
		problem.getNodes().clear();
		for (var statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration classDeclaration) {
				classDeclaration.setNewNode(null);
				classDeclarations.add(classDeclaration);
			} else if (statement instanceof Assertion assertion) {
				for (var argument : assertion.getArguments()) {
					if (argument instanceof ConstantAssertionArgument constantAssertionArgument) {
						constantAssertionArgument.setNode(null);
						constantAssertionArguments.add(constantAssertionArgument);
					}
				}
			}
		}
		adapter.retainAll(classDeclarations, constantAssertionArguments);
		derivedVariableComputer.discardDerivedVariables(problem);
	}

	protected Adapter getOrInstallAdapter(Resource resource) {
		if (!(resource instanceof XtextResource)) {
			return new Adapter();
		}
		String resourceLanguageName = ((XtextResource) resource).getLanguageName();
		if (!languageName.equals(resourceLanguageName)) {
			return new Adapter();
		}
		var adapter = (Adapter) EcoreUtil.getAdapter(resource.eAdapters(), Adapter.class);
		if (adapter == null) {
			adapter = new Adapter();
			resource.eAdapters().add(adapter);
		}
		return adapter;
	}

	protected static class Adapter extends AdapterImpl {
		private Map<ClassDeclaration, Node> newNodes = new HashMap<>();

		private Map<ConstantAssertionArgument, Node> constantNodes = new HashMap<>();

		public Node createNewNodeIfAbsent(ClassDeclaration classDeclaration,
				Function<ClassDeclaration, Node> createNode) {
			return newNodes.computeIfAbsent(classDeclaration, createNode);
		}

		public Node createConstantNodeIfAbsent(ConstantAssertionArgument constantAssertionArgument,
				Function<ConstantAssertionArgument, Node> createNode) {
			return constantNodes.computeIfAbsent(constantAssertionArgument, createNode);
		}

		public void retainAll(Collection<ClassDeclaration> classDeclarations,
				Collection<ConstantAssertionArgument> constantAssertionArguments) {
			newNodes.keySet().retainAll(classDeclarations);
			constantNodes.keySet().retainAll(constantAssertionArguments);
		}

		@Override
		public boolean isAdapterForType(Object type) {
			return Adapter.class == type;
		}
	}
}
