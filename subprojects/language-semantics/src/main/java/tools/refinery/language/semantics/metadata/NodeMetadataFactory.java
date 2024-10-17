/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.metadata;

import com.google.inject.Inject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.documentation.TypeHashProvider;
import tools.refinery.language.semantics.NodeNameProvider;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.typehierarchy.InferredType;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.tuple.Tuple;

public class NodeMetadataFactory {
	@Inject
	private NodeNameProvider nodeNameProvider;

	@Inject
	private TypeHashProvider typeHashProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	private ProblemTrace problemTrace;
	private Concreteness concreteness;
	private Interpretation<InferredType> typeInterpretation;
	private PartialInterpretation<TruthValue, Boolean> existsInterpretation;

	public void initialize(ProblemTrace problemTrace, Concreteness concreteness, Model model) {
		this.problemTrace = problemTrace;
		this.concreteness = concreteness;
		typeInterpretation = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL);
		existsInterpretation = model.getAdapter(ReasoningAdapter.class).getPartialInterpretation(concreteness,
				ReasoningAdapter.EXISTS_SYMBOL);
		nodeNameProvider.setProblem(problemTrace.getProblem());
	}

	public NodeMetadata doCreateMetadata(int nodeId, String name, String simpleName, NodeKind kind) {
		var type = getType(nodeId);
		return doCreateMetadata(name, simpleName, type, kind);
	}

	public NodeMetadata createFreshlyNamedMetadata(int nodeId) {
		var type = getType(nodeId);
		var name = getName(type, nodeId);
		String escapedName;
		if (name == null) {
			escapedName = "::" + nodeId;
		} else {
			escapedName = qualifiedNameConverter.toString(QualifiedName.create(name));
		}
		return doCreateMetadata(escapedName, escapedName, type, NodeKind.IMPLICIT);
	}

	public boolean nodeExists(int nodeId) {
		return existsInterpretation.get(Tuple.of(nodeId)).may();
	}

	private PartialRelation getType(int nodeId) {
		var inferredType = typeInterpretation.get(Tuple.of(nodeId));
		if (inferredType == null) {
			return null;
		}
		if (concreteness == Concreteness.CANDIDATE) {
			return inferredType.candidateType();
		}
		var typeHierarchy = problemTrace.getMetamodel().typeHierarchy();
		var mustTypes = inferredType.mustTypes();
		return mustTypes.stream()
				.filter(mustType -> typeHierarchy.getAnalysisResult(mustType).getDirectSubtypes().stream()
						.noneMatch(mustTypes::contains))
				.findFirst()
				.orElse(null);
	}

	private String getName(PartialRelation type, int nodeId) {
		if (concreteness == Concreteness.CANDIDATE && !nodeExists(nodeId)) {
			// Do not increment the node name counter for non-existent nodes in the candidate interpretation.
			// While non-existent nodes may appear in the partial interpretation, they are never displayed in the
			// candidate interpretation.
			return null;
		}
		if (type == null) {
			return nodeNameProvider.getNextName(null);
		}
		var relation = problemTrace.getRelation(type);
		return nodeNameProvider.getNextName(relation.getName());
	}

	private NodeMetadata doCreateMetadata(String name, String simpleName, PartialRelation type, NodeKind kind) {
		var typeHash = getTypeHash(type);
		return new NodeMetadata(name, simpleName, typeHash, kind);
	}

	private String getTypeHash(PartialRelation type) {
		if (type == null) {
			return null;
		}
		var relation = problemTrace.getRelation(type);
		return typeHashProvider.getTypeHash(relation);
	}
}
