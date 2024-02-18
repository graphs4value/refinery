/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class ProblemTraceImpl implements ProblemTrace {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private IScopeProvider scopeProvider;

	private Problem problem;
	private Metamodel metamodel;
	private final MutableObjectIntMap<Node> mutableNodeTrace = ObjectIntMaps.mutable.empty();
	private final ObjectIntMap<Node> nodeTrace = mutableNodeTrace.asUnmodifiable();
	private final Map<Relation, PartialRelation> mutableRelationTrace = new LinkedHashMap<>();
	private final Map<Relation, PartialRelation> relationTrace =
			Collections.unmodifiableMap(mutableRelationTrace);
	private final Map<AnyPartialSymbol, Relation> mutableInverseTrace = new HashMap<>();
	private final Map<AnyPartialSymbol, Relation> inverseTrace = Collections.unmodifiableMap(mutableInverseTrace);

	@Override
	public Problem getProblem() {
		return problem;
	}

	void setProblem(Problem problem) {
		this.problem = problem;
	}

	@Override
	public Metamodel getMetamodel() {
		return metamodel;
	}

	void setMetamodel(Metamodel metamodel) {
		this.metamodel = metamodel;
	}

	@Override
	public ObjectIntMap<Node> getNodeTrace() {
		return nodeTrace;
	}

	void collectNode(Node node) {
		mutableNodeTrace.getIfAbsentPut(node, mutableNodeTrace.size());
	}

	@Override
	public int getNodeId(Node node) {
		try {
			return nodeTrace.getOrThrow(node);
		} catch (IllegalStateException e) {
			var qualifiedName = semanticsUtils.getNameWithoutRootPrefix(node);
			throw new TracedException(node, "No node ID for " + qualifiedName, e);
		}
	}

	@Override
	public int getNodeId(QualifiedName qualifiedName) {
		var nodeScope = scopeProvider.getScope(problem, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE);
		return getNodeId(getElement(nodeScope, qualifiedName, Node.class));
	}

	@Override
	public int getNodeId(String qualifiedName) {
		var convertedName = qualifiedNameConverter.toQualifiedName(qualifiedName);
		return getNodeId(convertedName);
	}

	@Override
	public Map<Relation, PartialRelation> getRelationTrace() {
		return relationTrace;
	}

	void putRelation(Relation relation, PartialRelation partialRelation) {
		var oldPartialRelation = mutableRelationTrace.put(relation, partialRelation);
		if (oldPartialRelation != null) {
			throw new TracedException(relation, "Relation already mapped to partial relation: " + oldPartialRelation);
		}
		var oldRelation = mutableInverseTrace.put(partialRelation, relation);
		if (oldRelation != null) {
			throw new TracedException(oldRelation, "Partial relation %s was already mapped to relation"
					.formatted(partialRelation));
		}
	}

	@Override
	public Map<AnyPartialSymbol, Relation> getInverseRelationTrace() {
		return inverseTrace;
	}

	@Override
	public Relation getRelation(AnyPartialSymbol partialSymbol) {
		var relation = mutableInverseTrace.get(partialSymbol);
		if (relation == null) {
			throw new IllegalArgumentException("No relation for partial symbol: " + partialSymbol);
		}
		return relation;
	}

	@Override
	public RuntimeException wrapException(TranslationException translationException) {
		var partialSymbol = translationException.getPartialSymbol();
		if (partialSymbol == null) {
			return translationException;
		}
		var relation = mutableInverseTrace.get(partialSymbol);
		if (relation == null) {
			return translationException;
		}
		return new TracedException(relation, translationException);
	}

	@Override
	public PartialRelation getPartialRelation(Relation relation) {
		var partialRelation = mutableRelationTrace.get(relation);
		if (partialRelation == null) {
			var qualifiedName = semanticsUtils.getNameWithoutRootPrefix(relation);
			throw new TracedException(relation, "No partial relation for " + qualifiedName);
		}
		return partialRelation;
	}

	@Override
	public PartialRelation getPartialRelation(QualifiedName qualifiedName) {
		var relationScope = scopeProvider.getScope(problem, ProblemPackage.Literals.ASSERTION__RELATION);
		return getPartialRelation(getElement(relationScope, qualifiedName, Relation.class));
	}

	@Override
	public PartialRelation getPartialRelation(String qualifiedName) {
		var convertedName = qualifiedNameConverter.toQualifiedName(qualifiedName);
		return getPartialRelation(convertedName);
	}

	private <T> T getElement(IScope scope, QualifiedName qualifiedName, Class<T> type) {
		return semanticsUtils.getElement(problem, scope, qualifiedName, type);
	}
}
