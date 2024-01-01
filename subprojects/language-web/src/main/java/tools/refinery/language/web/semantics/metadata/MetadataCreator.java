/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics.metadata;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.NodeNameProvider;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;

public class MetadataCreator {
	@Inject
	private IScopeProvider scopeProvider;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private Provider<NodeNameProvider> nodeNameProviderProvider;

	private ProblemTrace problemTrace;
	private IScope nodeScope;
	private IScope relationScope;

	public void setProblemTrace(ProblemTrace problemTrace) {
		if (this.problemTrace != null) {
			throw new IllegalArgumentException("Problem trace was already set");
		}
		this.problemTrace = problemTrace;
		var problem = problemTrace.getProblem();
		nodeScope = scopeProvider.getScope(problem, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE);
		relationScope = scopeProvider.getScope(problem, ProblemPackage.Literals.ASSERTION__RELATION);
	}

	public List<NodeMetadata> getNodesMetadata(Model model, Concreteness concreteness) {
		int nodeCount = model.getAdapter(ReasoningAdapter.class).getNodeCount();
		var nodeTrace = problemTrace.getNodeTrace();
		var nodes = new NodeMetadata[Math.max(nodeTrace.size(), nodeCount)];
		var getName = makeGetName(model, concreteness);
		boolean preserveNewNodes = concreteness == Concreteness.PARTIAL;
		for (var entry : nodeTrace.keyValuesView()) {
			var node = entry.getOne();
			var id = entry.getTwo();
			nodes[id] = getNodeMetadata(id, node, preserveNewNodes, getName);
		}
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i] == null) {
				var nodeName = getName.apply(i);
				nodes[i] = new NodeMetadata(nodeName, nodeName, NodeKind.IMPLICIT);
			}
		}
		return List.of(nodes);
	}

	@NotNull
	private IntFunction<String> makeGetName(Model model, Concreteness concreteness) {
		var nodeNameProvider = nodeNameProviderProvider.get();
		nodeNameProvider.setProblem(problemTrace.getProblem());
		var typeInterpretation = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL);
		var existsInterpretation = model.getAdapter(ReasoningAdapter.class).getPartialInterpretation(concreteness,
				ReasoningAdapter.EXISTS_SYMBOL);
		return nodeId -> {
			var key = Tuple.of(nodeId);
			var inferredType = typeInterpretation.get(key);
			if (inferredType == null || inferredType.candidateType() == null) {
				return nodeNameProvider.getNextName(null);
			}
			if (concreteness == Concreteness.CANDIDATE && !existsInterpretation.get(key).may()) {
				// Do not increment the node name counter for non-existent nodes in the candidate interpretation.
				// While non-existent nodes may appear in the partial interpretation, they are never displayed in the
				// candidate interpretation.
				return "::" + nodeId;
			}
			var relation = problemTrace.getRelation(inferredType.candidateType());
			return nodeNameProvider.getNextName(relation.getName());
		};
	}

	private NodeMetadata getNodeMetadata(int nodeId, Node node, boolean preserveNewNodes,
										 IntFunction<String> getName) {
		var kind = getNodeKind(node);
		if (!preserveNewNodes && kind == NodeKind.NEW) {
			var nodeName = getName.apply(nodeId);
			return new NodeMetadata(nodeName, nodeName, NodeKind.IMPLICIT);
		}
		var qualifiedName = getQualifiedName(node);
		var simpleName = getSimpleName(node, qualifiedName, nodeScope);
		return new NodeMetadata(qualifiedNameConverter.toString(qualifiedName),
				qualifiedNameConverter.toString(simpleName), getNodeKind(node));
	}

	private NodeKind getNodeKind(Node node) {
		if (ProblemUtil.isImplicitNode(node)) {
			return NodeKind.IMPLICIT;
		} else if (ProblemUtil.isIndividualNode(node)) {
			return NodeKind.INDIVIDUAL;
		} else if (ProblemUtil.isNewNode(node)) {
			return NodeKind.NEW;
		} else {
			throw new TracedException(node, "Unknown node type");
		}
	}

	public List<RelationMetadata> getRelationsMetadata() {
		var relationTrace = problemTrace.getRelationTrace();
		var relations = new ArrayList<RelationMetadata>(relationTrace.size());
		for (var entry : relationTrace.entrySet()) {
			var relation = entry.getKey();
			var partialRelation = entry.getValue();
			var metadata = getRelationMetadata(relation, partialRelation);
			relations.add(metadata);
		}
		return Collections.unmodifiableList(relations);
	}

	private RelationMetadata getRelationMetadata(Relation relation, PartialRelation partialRelation) {
		var qualifiedName = getQualifiedName(relation);
		var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
		var simpleName = getSimpleName(relation, qualifiedName, relationScope);
		var simpleNameString = qualifiedNameConverter.toString(simpleName);
		var arity = partialRelation.arity();
		var detail = getRelationDetail(relation, partialRelation);
		return new RelationMetadata(qualifiedNameString, simpleNameString, arity, detail);
	}

	private RelationDetail getRelationDetail(Relation relation, PartialRelation partialRelation) {
		if (ProblemUtil.isBuiltIn(relation) && !ProblemUtil.isError(relation)) {
			return getBuiltInDetail();
		}
		if (relation instanceof ClassDeclaration classDeclaration) {
			return getClassDetail(classDeclaration);
		} else if (relation instanceof ReferenceDeclaration) {
			return getReferenceDetail(partialRelation);
		} else if (relation instanceof EnumDeclaration) {
			return getEnumDetail();
		} else if (relation instanceof PredicateDefinition predicateDefinition) {
			return getPredicateDetail(predicateDefinition);
		} else {
			throw new TracedException(relation, "Unknown relation");
		}
	}

	private RelationDetail getBuiltInDetail() {
		return BuiltInDetail.INSTANCE;
	}

	private RelationDetail getClassDetail(ClassDeclaration classDeclaration) {
		return ClassDetail.ofAbstractClass(classDeclaration.isAbstract());
	}

	private RelationDetail getReferenceDetail(PartialRelation partialRelation) {
		var metamodel = problemTrace.getMetamodel();
		var opposite = metamodel.oppositeReferences().get(partialRelation);
		if (opposite == null) {
			boolean isContainment = metamodel.containmentHierarchy().containsKey(partialRelation);
			return ReferenceDetail.ofContainment(isContainment);
		} else {
			boolean isContainer = metamodel.containmentHierarchy().containsKey(opposite);
			return new OppositeReferenceDetail(isContainer, opposite.name());
		}
	}

	private RelationDetail getEnumDetail() {
		return ClassDetail.CONCRETE_CLASS;
	}

	private RelationDetail getPredicateDetail(PredicateDefinition predicate) {
		return PredicateDetail.ofError(predicate.isError());
	}

	private QualifiedName getQualifiedName(EObject eObject) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(eObject);
		if (qualifiedName == null) {
			throw new TracedException(eObject, "Unknown qualified name");
		}
		return qualifiedName;
	}

	private QualifiedName getSimpleName(EObject eObject, QualifiedName qualifiedName, IScope scope) {
		var descriptions = scope.getElements(eObject);
		var names = new ArrayList<QualifiedName>();
		for (var description : descriptions) {
			// {@code getQualifiedName()} will refer to the full name for objects that are loaded from the global
			// scope, but {@code getName()} returns the qualified name that we set in
			// {@code ProblemResourceDescriptionStrategy}.
			names.add(description.getName());
		}
		names.sort(Comparator.comparingInt(QualifiedName::getSegmentCount));
		for (var simpleName : names) {
			if (names.contains(simpleName) && isUnique(scope, simpleName)) {
				return simpleName;
			}
		}
		throw new TracedException(eObject, "Ambiguous qualified name: " +
				qualifiedNameConverter.toString(qualifiedName));
	}

	private boolean isUnique(IScope scope, QualifiedName name) {
		var iterator = scope.getElements(name).iterator();
		if (!iterator.hasNext()) {
			return false;
		}
		iterator.next();
		return !iterator.hasNext();
	}
}
