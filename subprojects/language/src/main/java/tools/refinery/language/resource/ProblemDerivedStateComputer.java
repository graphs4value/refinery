package tools.refinery.language.resource;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IDerivedStateComputer;
import org.eclipse.xtext.resource.XtextResource;
import tools.refinery.language.model.problem.*;

import java.util.*;
import java.util.function.Function;

@Singleton
public class ProblemDerivedStateComputer implements IDerivedStateComputer {
	public static final String NEW_NODE = "new";

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	private String languageName;

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
		problem.getNodes().clear();
		for (var statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration classDeclaration) {
				classDeclaration.setNewNode(null);
				classDeclarations.add(classDeclaration);
			}
		}
		adapter.retainAll(classDeclarations);
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
		private final Map<ClassDeclaration, Node> newNodes = new HashMap<>();

		public Node createNewNodeIfAbsent(ClassDeclaration classDeclaration,
				Function<ClassDeclaration, Node> createNode) {
			return newNodes.computeIfAbsent(classDeclaration, createNode);
		}

		public void retainAll(Collection<ClassDeclaration> classDeclarations) {
			newNodes.keySet().retainAll(classDeclarations);
		}

		@Override
		public boolean isAdapterForType(Object type) {
			return Adapter.class == type;
		}
	}
}
