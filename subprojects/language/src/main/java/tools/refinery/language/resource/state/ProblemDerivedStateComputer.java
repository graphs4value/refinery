/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource.state;

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
import tools.refinery.language.utils.ProblemUtil;

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
		installDerivedClassDeclarationState(problem, adapter);
		if (preLinkingPhase) {
			return;
		}
		Set<String> nodeNames = installDerivedNodes(problem);
		derivedVariableComputer.installDerivedVariables(problem, nodeNames);
	}

	protected void installDerivedClassDeclarationState(Problem problem, Adapter adapter) {
		for (var statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration classDeclaration) {
				installOrRemoveNewNode(adapter, classDeclaration);
				for (var featureDeclaration : classDeclaration.getFeatureDeclarations()) {
					if (featureDeclaration instanceof ReferenceDeclaration referenceDeclaration) {
						installOrRemoveInvalidMultiplicityPredicate(adapter, classDeclaration, referenceDeclaration);
					}
				}
			}
		}
	}

	protected void installOrRemoveNewNode(Adapter adapter, ClassDeclaration declaration) {
		if (declaration.isAbstract()) {
			var newNode = declaration.getNewNode();
			if (newNode != null) {
				declaration.setNewNode(null);
				adapter.removeNewNode(declaration);
			}
		} else {
			if (declaration.getNewNode() == null) {
				var newNode = adapter.createNewNodeIfAbsent(declaration, key -> createNode(NEW_NODE));
				declaration.setNewNode(newNode);
			}
		}
	}

	protected void installOrRemoveInvalidMultiplicityPredicate(
			Adapter adapter, ClassDeclaration containingClassDeclaration, ReferenceDeclaration declaration) {
		if (ProblemUtil.hasMultiplicityConstraint(declaration)) {
			if (declaration.getInvalidMultiplicity() == null) {
				var invalidMultiplicity = adapter.createInvalidMultiplicityPredicateIfAbsent(declaration, key -> {
					var predicate = ProblemFactory.eINSTANCE.createPredicateDefinition();
					predicate.setError(true);
					predicate.setName("invalidMultiplicity");
					var parameter = ProblemFactory.eINSTANCE.createParameter();
					parameter.setParameterType(containingClassDeclaration);
					parameter.setName("node");
					predicate.getParameters().add(parameter);
					return predicate;
				});
				declaration.setInvalidMultiplicity(invalidMultiplicity);
			}
		} else {
			var invalidMultiplicity = declaration.getInvalidMultiplicity();
			if (invalidMultiplicity != null) {
				declaration.setInvalidMultiplicity(null);
				adapter.removeInvalidMultiplicityPredicate(declaration);
			}
		}
	}

	protected Set<String> installDerivedNodes(Problem problem) {
		var collector = nodeNameCollectorProvider.get();
		collector.collectNodeNames(problem);
		Set<String> nodeNames = collector.getNodeNames();
		List<Node> graphNodes = problem.getNodes();
		for (String nodeName : nodeNames) {
			var graphNode = createNode(nodeName);
			graphNodes.add(graphNode);
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
		var abstractClassDeclarations = new HashSet<ClassDeclaration>();
		var referenceDeclarationsWithMultiplicity = new HashSet<ReferenceDeclaration>();
		problem.getNodes().clear();
		for (var statement : problem.getStatements()) {
			if (statement instanceof ClassDeclaration classDeclaration) {
				classDeclaration.setNewNode(null);
				if (classDeclaration.isAbstract()) {
					abstractClassDeclarations.add(classDeclaration);
				}
				for (var featureDeclaration : classDeclaration.getFeatureDeclarations()) {
					if (featureDeclaration instanceof ReferenceDeclaration referenceDeclaration &&
							ProblemUtil.hasMultiplicityConstraint(referenceDeclaration)) {
						referenceDeclarationsWithMultiplicity.add(referenceDeclaration);
					}
				}
			}
		}
		adapter.retainAll(abstractClassDeclarations, referenceDeclarationsWithMultiplicity);
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
		private final Map<ReferenceDeclaration, PredicateDefinition> invalidMultiplicityPredicates = new HashMap<>();

		public Node createNewNodeIfAbsent(ClassDeclaration classDeclaration,
										  Function<ClassDeclaration, Node> createNode) {
			return newNodes.computeIfAbsent(classDeclaration, createNode);
		}

		public void removeNewNode(ClassDeclaration classDeclaration) {
			newNodes.remove(classDeclaration);
		}

		public PredicateDefinition createInvalidMultiplicityPredicateIfAbsent(
				ReferenceDeclaration referenceDeclaration,
				Function<ReferenceDeclaration, PredicateDefinition> createPredicate) {
			return invalidMultiplicityPredicates.computeIfAbsent(referenceDeclaration, createPredicate);
		}

		public void removeInvalidMultiplicityPredicate(ReferenceDeclaration referenceDeclaration) {
			invalidMultiplicityPredicates.remove(referenceDeclaration);
		}

		public void retainAll(Collection<ClassDeclaration> abstractClassDeclarations,
							  Collection<ReferenceDeclaration> referenceDeclarationsWithMultiplicity) {
			newNodes.keySet().retainAll(abstractClassDeclarations);
			invalidMultiplicityPredicates.keySet().retainAll(referenceDeclarationsWithMultiplicity);
		}

		@Override
		public boolean isAdapterForType(Object type) {
			return Adapter.class == type;
		}
	}
}
