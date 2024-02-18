/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.scoping.IScopeProvider;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.utils.ProblemDesugarer;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.typehierarchy.InferredType;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SolutionSerializer {
	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	private IResourceFactory resourceFactory;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private IScopeProvider scopeProvider;

	@Inject
	private ProblemDesugarer desugarer;

	@Inject
	private NodeNameProvider nameProvider;

	private ProblemTrace trace;
	private Model model;
	private ReasoningAdapter reasoningAdapter;
	private PartialInterpretation<TruthValue, Boolean> existsInterpretation;
	private Resource originalResource;
	private Problem originalProblem;
	private QualifiedName originalProblemName;
	private Problem problem;
	private QualifiedName newProblemName;
	private NodeDeclaration nodeDeclaration;
	private final MutableIntObjectMap<Node> nodes = IntObjectMaps.mutable.empty();

	public Problem serializeSolution(ProblemTrace trace, Model model) {
		var uri = URI.createURI("__solution_%s.%s".formatted(UUID.randomUUID().toString().replace('-', '_'),
				ProblemUtil.MODULE_EXTENSION));
		return serializeSolution(trace, model, uri);
	}

	public Problem serializeSolution(ProblemTrace trace, Model model, URI uri) {
		this.trace = trace;
		this.model = model;
		reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		existsInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE,
				ReasoningAdapter.EXISTS_SYMBOL);
		originalProblem = trace.getProblem();
		originalProblemName = qualifiedNameProvider.getFullyQualifiedName(originalProblem);
		problem = copyProblem(originalProblem, uri);
		problem.setKind(ProblemUtil.getDefaultModuleKind(uri));
		problem.setExplicitKind(false);
		problem.setName(null);
		newProblemName = qualifiedNameProvider.getFullyQualifiedName(originalProblem);
		problem.getStatements().removeIf(SolutionSerializer::shouldRemoveStatement);
		removeNonExistentImplicitNodes();
		nodeDeclaration = ProblemFactory.eINSTANCE.createNodeDeclaration();
		nodeDeclaration.setKind(NodeKind.NODE);
		nodeDeclaration.getNodes().addAll(problem.getNodes());
		problem.getStatements().add(nodeDeclaration);
		nameProvider.setProblem(problem);
		addExistsAssertions();
		addClassAssertions();
		addReferenceAssertions();
		if (nodeDeclaration.getNodes().isEmpty()) {
			problem.getStatements().remove(nodeDeclaration);
		}
		return problem;
	}

	private static boolean shouldRemoveStatement(Statement statement) {
		return statement instanceof Assertion || statement instanceof ScopeDeclaration;
	}

	private void removeNonExistentImplicitNodes() {
		var originalImplicitNodes = originalProblem.getNodes();
		var newImplicitNodes = problem.getNodes();
		if (newImplicitNodes.size() != originalImplicitNodes.size()) {
			throw new IllegalStateException("Expected %d implicit nodes in problem, but got %d after copying"
					.formatted(originalImplicitNodes.size(), newImplicitNodes.size()));
		}
		var iterator = newImplicitNodes.iterator();
		for (var originalNode : originalImplicitNodes) {
			if (!iterator.hasNext()) {
				throw new AssertionError("Unexpected end of copied implicit node list");
			}
			var newNode = iterator.next();
			if (!Objects.equals(originalNode.getName(), newNode.getName())) {
				throw new IllegalStateException("Expected copy of '%s' to have the same name, got '%s' instead"
						.formatted(originalNode.getName(), newNode.getName()));
			}
			int nodeId = trace.getNodeId(originalNode);
			if (!isExistingNode(nodeId)) {
				iterator.remove();
			}
		}
	}

	private boolean isExistingNode(int nodeId) {
		var exists = existsInterpretation.get(Tuple.of(nodeId));
		if (!exists.isConcrete()) {
			throw new IllegalStateException("Invalid EXISTS %s for node %d".formatted(exists, nodeId));
		}
		return exists.may();
	}

	private Problem copyProblem(Problem originalProblem, URI uri) {
		originalResource = originalProblem.eResource();
		var resourceSet = originalResource.getResourceSet();
		var newResource = resourceFactory.createResource(uri);
		resourceSet.getResources().add(newResource);
		if (originalResource instanceof XtextResource) {
			byte[] bytes;
			try {
				try (var outputStream = new ByteArrayOutputStream()) {
					originalResource.save(outputStream, Map.of());
					bytes = outputStream.toByteArray();
				}
				try (var inputStream = new ByteArrayInputStream(bytes)) {
					newResource.load(inputStream, Map.of());
				}
			} catch (IOException e) {
				throw new IllegalStateException("Failed to copy problem", e);
			}
			var contents = newResource.getContents();
			EcoreUtil.resolveAll(newResource);
			if (!contents.isEmpty() && contents.getFirst() instanceof Problem newProblem) {
				return newProblem;
			}
			throw new IllegalStateException("Invalid contents of copied problem");
		} else {
			var newProblem = EcoreUtil.copy(originalProblem);
			newResource.getContents().add(newProblem);
			return newProblem;
		}
	}

	private QualifiedName getConvertedName(EObject original) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(original);
		if (originalProblemName != null && qualifiedName.startsWith(originalProblemName)) {
			qualifiedName = qualifiedName.skipFirst(originalProblemName.getSegmentCount());
		}
		if (newProblemName != null) {
			qualifiedName = newProblemName.append(qualifiedName);
		}
		return NamingUtil.addRootPrefix(qualifiedName);
	}

	private Relation findRelation(Relation originalRelation) {
		if (originalRelation.eResource() != originalResource) {
			return originalRelation;
		}
		var qualifiedName = getConvertedName(originalRelation);
		return semanticsUtils.getLocalElement(problem, qualifiedName, Relation.class,
				ProblemPackage.Literals.RELATION);
	}

	private Relation findPartialRelation(PartialRelation partialRelation) {
		return findRelation(trace.getRelation(partialRelation));
	}

	private Node findNode(Node originalNode) {
		if (originalNode.eResource() != originalResource) {
			return originalNode;
		}
		var qualifiedName = getConvertedName(originalNode);
		return semanticsUtils.maybeGetLocalElement(problem, qualifiedName, Node.class, ProblemPackage.Literals.NODE);
	}

	private void addAssertion(Relation relation, LogicValue value, Node... arguments) {
		var assertion = ProblemFactory.eINSTANCE.createAssertion();
		assertion.setRelation(relation);
		for (var node : arguments) {
			var argument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
			argument.setNode(node);
			assertion.getArguments().add(argument);
		}
		var logicConstant = ProblemFactory.eINSTANCE.createLogicConstant();
		logicConstant.setLogicValue(value);
		assertion.setValue(logicConstant);
		problem.getStatements().add(assertion);
	}

	private void addExistsAssertions() {
		var builtinSymbols = desugarer.getBuiltinSymbols(problem).orElseThrow(() -> new IllegalStateException("No " +
				"builtin library in copied problem"));
		// Make sure to output exists assertions in a deterministic order.
		var sortedNewNodes = new TreeMap<Integer, Node>();
		for (var pair : trace.getNodeTrace().keyValuesView()) {
			var originalNode = pair.getOne();
			int nodeId = pair.getTwo();
			var newNode = findNode(originalNode);
			// Since all implicit nodes that do not exist has already been removed in serializeSolution,
			// we only need to add !exists assertions to ::new nodes and explicitly declared nodes that do not exist.
			if (ProblemUtil.isMultiNode(originalNode) || (ProblemUtil.isDeclaredNode(originalNode) && !isExistingNode(nodeId))) {
				sortedNewNodes.put(nodeId, newNode);
			} else {
				nodes.put(nodeId, newNode);
			}
		}
		for (var newNode : sortedNewNodes.values()) {
			// If a node is a new node of the class, we should replace it with a normal node.
			addAssertion(builtinSymbols.exists(), LogicValue.FALSE, newNode);
		}
	}

	private void addClassAssertions() {
		var types =
				trace.getMetamodel().typeHierarchy().getPreservedTypes().keySet().stream().collect(Collectors.toMap(Function.identity(), this::findPartialRelation));
		var cursor = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL).getAll();
		while (cursor.move()) {
			var key = cursor.getKey();
			var nodeId = key.get(0);
			if (isExistingNode(nodeId)) {
				createNodeAndAssertType(nodeId, cursor.getValue(), types);
			}
		}
	}

	private void createNodeAndAssertType(int nodeId, InferredType inferredType, Map<PartialRelation, Relation> types) {
		var candidateTypeSymbol = inferredType.candidateType();
		var candidateRelation = types.get(candidateTypeSymbol);
		if (candidateRelation instanceof EnumDeclaration) {
			// Type assertions for enum literals are added implicitly.
			return;
		}
		Node node = nodes.get(nodeId);
		if (node == null) {
			var typeName = candidateRelation.getName();
			var nodeName = nameProvider.getNextName(typeName);
			node = ProblemFactory.eINSTANCE.createNode();
			node.setName(nodeName);
			nodeDeclaration.getNodes().add(node);
			nodes.put(nodeId, node);
		}
		addAssertion(candidateRelation, LogicValue.TRUE, node);
		var typeAnalysisResult = trace.getMetamodel().typeHierarchy().getPreservedTypes().get(candidateTypeSymbol);
		for (var subtype : typeAnalysisResult.getDirectSubtypes()) {
			var subtypeRelation = types.get(subtype);
			addAssertion(subtypeRelation, LogicValue.FALSE, node);
		}
	}

	private void addReferenceAssertions() {
		var metamodel = trace.getMetamodel();
		for (var partialRelation : metamodel.containmentHierarchy().keySet()) {
			// No need to add a default value, because in a concrete model, each contained node has only a single
			// container.
			addAssertions(partialRelation);
		}
		for (var partialRelation : metamodel.directedCrossReferences().keySet()) {
			addDefaultAssertion(partialRelation);
			addAssertions(partialRelation);
		}
		// No need to add directed opposite references, because their default value is {@code unknown} and their
		// actual value will always be computed from the value of the directed forward reference.
		// However, undirected cross-references have to be serialized in both directions due to the default value of
		// {@code false}.
		for (var partialRelation : metamodel.undirectedCrossReferences().keySet()) {
			addDefaultAssertion(partialRelation);
			addAssertions(partialRelation);
		}
	}

	private void addAssertions(PartialRelation partialRelation) {
		var relation = findPartialRelation(partialRelation);
		var cursor = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, partialRelation).getAll();
		// Make sure to output assertions in a deterministic order.
		var sortedTuples = new TreeSet<Tuple>();
		while (cursor.move()) {
			var tuple = cursor.getKey();
			var from = nodes.get(tuple.get(0));
			var to = nodes.get(tuple.get(1));
			if (from == null || to == null) {
				// One of the endpoints does not exist in the candidate model.
				continue;
			}
			var value = cursor.getValue();
			if (!value.isConcrete()) {
				throw new IllegalStateException("Invalid %s %s for tuple %s".formatted(partialRelation, value, tuple));
			}
			if (value.may()) {
				sortedTuples.add(tuple);
			}
		}
		for (var tuple : sortedTuples) {
			var from = nodes.get(tuple.get(0));
			var to = nodes.get(tuple.get(1));
			addAssertion(relation, LogicValue.TRUE, from, to);
		}
	}

	private void addDefaultAssertion(PartialRelation partialRelation) {
		var relation = findPartialRelation(partialRelation);
		var assertion = ProblemFactory.eINSTANCE.createAssertion();
		assertion.setDefault(true);
		assertion.setRelation(relation);
		int arity = ProblemUtil.getArity(relation);
		for (int i = 0; i < arity; i++) {
			var argument = ProblemFactory.eINSTANCE.createWildcardAssertionArgument();
			assertion.getArguments().add(argument);
		}
		var logicConstant = ProblemFactory.eINSTANCE.createLogicConstant();
		logicConstant.setLogicValue(LogicValue.FALSE);
		assertion.setValue(logicConstant);
		problem.getStatements().add(assertion);
	}
}
