/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.model.problem.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
public class SignatureProvider {
	private static final String PREFIX = "tools.refinery.language.typesystem.SignatureProvider.";
	private static final String SIGNATURE_CACHE = PREFIX + "SIGNATURE_CACHE";
	private static final String DATATYPE_CACHE = PREFIX + "DATATYPE_CACHE";
	private static final String AGGREGATOR_CACHE = PREFIX + "AGGREGATOR_CACHE";

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IResourceScopeCache cache;

	public Signature getSignature(Relation relation) {
		var signatures = cache.get(SIGNATURE_CACHE, relation.eResource(), () -> new HashMap<Relation, Signature>());
		return signatures.computeIfAbsent(relation, this::computeSignature);
	}

	public int getArity(Relation relation) {
		return getSignature(relation).parameterTypes().size();
	}

	private Signature computeSignature(Relation relation) {
		return new Signature(getParameterTypes(relation), getResultType(relation));
	}

	private List<FixedType> getParameterTypes(Relation relation) {
		return switch (relation) {
			case ClassDeclaration ignored -> List.of(ExprType.NODE);
			case EnumDeclaration ignored -> List.of(ExprType.NODE);
			case DatatypeDeclaration datatypeDeclaration -> List.of(getDataType(datatypeDeclaration));
			case ReferenceDeclaration referenceDeclaration -> {
				if (referenceDeclaration.getReferenceType() instanceof DatatypeDeclaration) {
					yield List.of(ExprType.NODE);
				}
				yield List.of(ExprType.NODE, ExprType.NODE);
			}
			case ParametricDefinition parametricDefinition -> {
				var parameters = parametricDefinition.getParameters();
				var exprTypes = new ArrayList<FixedType>(parameters.size());
				for (var parameter : parameters) {
					if (parameter.getParameterType() instanceof DatatypeDeclaration datatypeDeclaration) {
						exprTypes.add(getDataType(datatypeDeclaration));
					} else {
						exprTypes.add(ExprType.NODE);
					}
				}
				yield List.copyOf(exprTypes);
			}
			default -> throw new IllegalArgumentException("Unknown Relation: " + relation);
		};
	}

	private FixedType getResultType(Relation relation) {
		if (relation instanceof ReferenceDeclaration referenceDeclaration &&
				referenceDeclaration.getReferenceType() instanceof DatatypeDeclaration datatypeDeclaration) {
			return getDataType(datatypeDeclaration);
		}
		return ExprType.LITERAL;
	}

	public DataExprType getDataType(DatatypeDeclaration datatypeDeclaration) {
		var dataTypes = cache.get(DATATYPE_CACHE, datatypeDeclaration.eResource(),
				() -> new HashMap<DatatypeDeclaration, DataExprType>());
		return dataTypes.computeIfAbsent(datatypeDeclaration, this::computeDataType);
	}

	private DataExprType computeDataType(DatatypeDeclaration datatypeDeclaration) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(datatypeDeclaration);
		if (qualifiedName == null) {
			throw new IllegalArgumentException("Datatype declaration has no qualified name: " + datatypeDeclaration);
		}
		return new DataExprType(qualifiedName);
	}

	public AggregatorName getAggregatorName(AggregatorDeclaration aggregatorDeclaration) {
		var dataTypes = cache.get(AGGREGATOR_CACHE, aggregatorDeclaration.eResource(),
				() -> new HashMap<AggregatorDeclaration, AggregatorName>());
		return dataTypes.computeIfAbsent(aggregatorDeclaration, this::computeAggregatorName);
	}

	private AggregatorName computeAggregatorName(AggregatorDeclaration aggregatorDeclaration) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(aggregatorDeclaration);
		if (qualifiedName == null) {
			throw new IllegalArgumentException(
					"Aggregator declaration has no qualified name: " + aggregatorDeclaration);
		}
		return new AggregatorName(qualifiedName);
	}
}
