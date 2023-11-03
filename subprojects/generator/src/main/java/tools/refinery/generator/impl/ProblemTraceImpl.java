/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.IScopeProvider;
import tools.refinery.generator.ProblemTrace;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.language.semantics.metadata.NodeMetadata;
import tools.refinery.language.semantics.metadata.RelationMetadata;
import tools.refinery.language.semantics.model.ModelInitializer;
import tools.refinery.language.semantics.model.TracedException;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ProblemTraceImpl implements ProblemTrace {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IScopeProvider scopeProvider;

	@Inject
	private Provider<MetadataCreator> metadataCreatorProvider;

	private ModelInitializer initializer;
	private Map<Relation, PartialRelation> relationTrace;
	private MetadataCreator metadataCreator;

	public void setInitializer(ModelInitializer initializer) {
		this.initializer = initializer;
		relationTrace = Collections.unmodifiableMap(initializer.getRelationTrace());
	}

	public Problem getProblem() {
		return initializer.getProblem();
	}

	public Map<Relation, PartialRelation> getRelationTrace() {
		return relationTrace;
	}

	@Override
	public Relation getInverseTrace(AnyPartialSymbol partialSymbol) {
		return initializer.getInverseTrace(partialSymbol);
	}

	public PartialRelation getPartialRelation(Relation relation) {
		var partialRelation = relationTrace.get(relation);
		if (partialRelation == null) {
			var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(relation);
			var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
			throw new IllegalArgumentException("No partial relation for relation " + qualifiedNameString);
		}
		return partialRelation;
	}

	public PartialRelation getPartialRelation(QualifiedName qualifiedName) {
		var scope = scopeProvider.getScope(getProblem(), ProblemPackage.Literals.ASSERTION__RELATION);
		var iterator = scope.getElements(qualifiedName).iterator();
		if (!iterator.hasNext()) {
			var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
			throw new IllegalArgumentException("No such relation: " + qualifiedNameString);
		}
		var eObjectDescription = iterator.next();
		if (iterator.hasNext()) {
			var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
			throw new IllegalArgumentException("Ambiguous relation: " + qualifiedNameString);
		}
		var eObject = EcoreUtil.resolve(eObjectDescription.getEObjectOrProxy(), getProblem());
		if (!(eObject instanceof Relation relation)) {
			var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
			throw new IllegalArgumentException("Not a relation: " + qualifiedNameString);
		}
		return getPartialRelation(relation);
	}

	public PartialRelation getPartialRelation(String qualifiedName) {
		var convertedName = qualifiedNameConverter.toQualifiedName(qualifiedName);
		return getPartialRelation(convertedName);
	}

	@Override
	public List<RelationMetadata> getRelationsMetadata() {
		return getMetadataCreator().getRelationsMetadata();
	}

	@Override
	public List<NodeMetadata> getNodesMetadata(int nodeCount, boolean preserveNewNodes) {
		return getMetadataCreator().getNodesMetadata(nodeCount, preserveNewNodes);
	}

	private MetadataCreator getMetadataCreator() {
		if (metadataCreator == null) {
			metadataCreator = metadataCreatorProvider.get();
			metadataCreator.setInitializer(initializer);
		}
		return metadataCreator;
	}

	public static RuntimeException wrapException(ProblemTrace trace, TranslationException translationException) {
		var source = trace.getInverseTrace(translationException.getPartialSymbol());
		if (source == null) {
			return translationException;
		}
		return new TracedException(source, translationException);
	}
}
