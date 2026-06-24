/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import com.google.inject.Provider;
import tools.refinery.generator.ConsistencyCheckResult;
import tools.refinery.generator.ModelFacade;
import tools.refinery.generator.ModelFacadeResult;
import tools.refinery.generator.RefineryDiagnostics;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.language.semantics.metadata.NodesMetadata;
import tools.refinery.language.semantics.metadata.RelationMetadata;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationRejectedException;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.transition.ExclusionPropagator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.seed.PropagatedModel;
import tools.refinery.store.reasoning.translator.TranslationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import tools.refinery.generator.dto.*;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.reasoning.representation.PartialRelation;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;


public abstract class ModelFacadeImpl implements ModelFacade {
	private final ProblemTrace problemTrace;
	private final ModelStore store;
	private final ModelFacadeResult initializationResult;
	private final Model model;
	private final ReasoningAdapter reasoningAdapter;
	private final Provider<MetadataCreator> metadataCreatorProvider;
	private final RefineryDiagnostics diagnostics;

	protected ModelFacadeImpl(Args args) {
		problemTrace = args.problemTrace();
		store = args.store();
		metadataCreatorProvider = args.metadataCreatorProvider();
		diagnostics = args.diagnostics();
		var propagatedModel = getPropagatedModel(args);
		var theModel = propagatedModel.model();
		try {
			var propagationResult = propagatedModel.propagationResult();
			var createInitialModelResult = propagationResult instanceof PropagationRejectedResult rejectedResult ?
					new ModelFacadeResult.PropagationRejected(rejectedResult) : ModelFacadeResult.SUCCESS;
			initializationResult = afterPropagation(theModel, createInitialModelResult);
			reasoningAdapter = theModel.getAdapter(ReasoningAdapter.class);
		} catch (RuntimeException e) {
			theModel.close();
			model = null;
			throw e;
		}
		model = theModel;
	}

	private PropagatedModel getPropagatedModel(Args args) {
		PropagatedModel propagatedModel;
		try {
			propagatedModel = store.getAdapter(ReasoningStoreAdapter.class).tryCreateInitialModel(args.modelSeed());
		} catch (TranslationException e) {
			throw diagnostics.wrapTranslationException(e, problemTrace);
		} catch (TracedException e) {
			throw diagnostics.wrapTracedException(e, problemTrace);
		} catch (PropagationRejectedException e) {
			throw diagnostics.wrapPropagationRejectedException(e, problemTrace);
		}
		if (propagatedModel.propagationResult() instanceof PropagationRejectedResult rejectedResult &&
				rejectedResult.reason() instanceof ExclusionPropagator) {
			// Return models with errors as if they were correctly propagated. Callers can check for consistency with
			// {@link #checkConsistency()} later.
			return new PropagatedModel(propagatedModel.model(), PropagationResult.PROPAGATED);
		}
		return propagatedModel;
	}

	@Override
	public ProblemTrace getProblemTrace() {
		return problemTrace;
	}

	@Override
	public ModelStore getModelStore() {
		return store;
	}

	@Override
	public Model getModel() {
		return model;
	}

	/**
	 * Post-processes the propagation result on this model.
	 * <p>
	 * Inheriting classes may use this method to perform other operations, such as concretization, on the model.
	 * </p>
	 * <p>
	 * Implementations should <b>never</b> call {@link #getModel()}, as it hasn't been set yet.
	 * Use the {@code model} argument instead.
	 * </p>
	 *
	 * @param model                    The {@link Model} after propagation.
	 * @param createInitialModelResult The result of the propagation on the model.
	 * @return The post-processed propagation result.
	 */
	// The {@code model} argument is unused in the default implementation, but used in inheriting implementations.
	@SuppressWarnings("squid:S1172")
	protected ModelFacadeResult afterPropagation(Model model, ModelFacadeResult createInitialModelResult) {
		return createInitialModelResult;
	}

	@Override
	public ModelFacadeResult getInitializationResult() {
		return initializationResult;
	}

	@Override
	public void throwIfInitializationFailed() {
		if (getInitializationResult() instanceof ModelFacadeResult.Rejected rejectedResult) {
			// Make sure to release native resources if the model is unusable.
			close();
			throw diagnostics.createModelFacadeResultException(rejectedResult, problemTrace);
		}
	}

	@Override
	public <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> getPartialInterpretation(
			PartialSymbol<A, C> partialSymbol) {
		return reasoningAdapter.getPartialInterpretation(getConcreteness(), partialSymbol);
	}

	@Override
	public NodesMetadata getNodesMetadata() {
		return getMetadataCreator().getNodesMetadata(model, getConcreteness());
	}

	@Override
	public List<RelationMetadata> getRelationsMetadata() {
		return getMetadataCreator().getRelationsMetadata();
	}

	protected MetadataCreator getMetadataCreator() {
		var metadataCreator = metadataCreatorProvider.get();
		metadataCreator.setProblemTrace(problemTrace);
		return metadataCreator;
	}

	protected RefineryDiagnostics getDiagnostics() {
		return diagnostics;
	}

	@Override
	public ConsistencyCheckResult checkConsistency() {
		var errors = new ArrayList<ConsistencyCheckResult.AnyError>();
		var existsInterpretation = getPartialInterpretation(ReasoningAdapter.EXISTS_SYMBOL);
		for (var entry : problemTrace.getRelationTrace().entrySet()) {
			var relation = entry.getKey();
			if (ProblemUtil.isShadow(relation)) {
				continue;
			}
			var partialSymbol = (PartialSymbol<? extends AbstractValue<?, ?>, ?>) entry.getValue();
			checkConsistency(partialSymbol, existsInterpretation, errors);
		}
		return new ConsistencyCheckResult(this, List.copyOf(errors));
	}

	private <A extends AbstractValue<A, C>, C> void checkConsistency(
			PartialSymbol<A, C> partialSymbol, PartialInterpretation<TruthValue, Boolean> existsInterpretation,
			List<ConsistencyCheckResult.AnyError> errors) {
		// Filter for non-existing errors even if they are retained by getPartialInterpretation.
		var interpretation = FilteredInterpretation.of(getPartialInterpretation(partialSymbol),
				existsInterpretation);
		var cursor = interpretation.getAll();
		while (cursor.move()) {
			var value = cursor.getValue();
			if (value.isError()) {
				errors.add(new ConsistencyCheckResult.Error<>(partialSymbol, cursor.getKey(), value));
			}
		}
	}

	public List<TypeDto> getAllSuperTypesOfType(TypeDto type) {
		if (type.getSuperTypeObjects().isEmpty()) {
			List<TypeDto> supertypes = new ArrayList<>();
			supertypes.add(type);
			return supertypes;
		} else {
			List<TypeDto> supertypes = new ArrayList<>();
			for (TypeDto superType : type.getSuperTypeObjects()) {
				supertypes.addAll(getAllSuperTypesOfType(superType));
			}
			supertypes.add(type);
			return supertypes;
		}
	}

	@Override
	public ObjectNetDto getObjectNet() {
		Map<PartialRelation, TypeDto> typeMap = new HashMap<>();
		Map<PartialRelation, RelationTypeDto> relationTypeMap = new HashMap<>();
		Map<Integer, InstanceDto> instanceMap = new HashMap<>();
		Map<Tuple, RelationInstanceDto> relationInstanceMap = new HashMap<>();

		var trace = getProblemTrace();
		var metamodel = trace.getMetamodel();

		metamodel.typeHierarchy().getAllTypes().forEach(type -> typeMap.put(type, new TypeDto(type, type.name())));

		typeMap.forEach((key, value) -> {
			for (PartialRelation subType : metamodel.typeHierarchy().getAnalysisResult(key).getDirectSubtypes()) {
				TypeDto subTypeObj = typeMap.get(subType);
				value.addSubType(subTypeObj);
				subTypeObj.addSuperType(value);
			}
		});

		metamodel.containmentHierarchy().forEach((rel, info) -> relationTypeMap.put(rel,
				new RelationTypeDto(rel, RelationTypeDto.Kind.CONTAINMENT, rel.name(), typeMap.get(info.sourceType()),
						typeMap.get(info.targetType()))));
		metamodel.directedCrossReferences().forEach((rel, info) -> relationTypeMap.put(rel,
				new RelationTypeDto(rel, RelationTypeDto.Kind.DIRECTED_CROSS_REF, rel.name(),
						typeMap.get(info.sourceType()), typeMap.get(info.targetType()))));

		var nodeTrace = trace.getNodeTrace();
		var typeInterpretation = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL);
		var typeInterpretaionCursor = typeInterpretation.getAll();
		while (typeInterpretaionCursor.move()) {
			final var cursor = typeInterpretaionCursor;
			typeMap.forEach((key, value) -> {
				if (value.getName().equals(cursor.getValue().candidateType().name()) &&
						cursor.getValue().isMust(cursor.getValue().candidateType())) {
					int nodeId = cursor.getKey().get(0);
					var nodes = nodeTrace.flipUniqueValues().get(nodeId);
					String name = (nodes != null) ? nodes.getName() : "";
					if ("new".equals(name) || "".equals(name)) {
						name = value.getName() + nodeId;
					}
					instanceMap.put(nodeId, new InstanceDto(nodeId, value, name));
				}
			});
		}

		instanceMap.forEach((id, instance) -> {
			TypeDto principalType = instance.getPrincipalTypeObject();
			for (TypeDto superType : principalType.getSuperTypeObjects()) {
				instance.addTypes(getAllSuperTypesOfType(superType));
			}
		});

		for (Map.Entry<PartialRelation, RelationTypeDto> entry : relationTypeMap.entrySet()) {
			var relationInterpretation = getPartialInterpretation(entry.getKey());
			var relationCursor = relationInterpretation.getAll();
			while (relationCursor.move()) {
				if (relationCursor.getValue() == TruthValue.TRUE) {
					Tuple pair = relationCursor.getKey();
					InstanceDto sourceInstance = instanceMap.get(pair.get(0));
					InstanceDto targetInstance = instanceMap.get(pair.get(1));
					if (sourceInstance != null && targetInstance != null) {
						relationInstanceMap.put(pair,
								new RelationInstanceDto(entry.getKey(), entry.getValue(), sourceInstance,
										targetInstance));
					}
				}
			}
		}

		return new ObjectNetDto(
				new ArrayList<>(typeMap.values()),
				new ArrayList<>(relationTypeMap.values()),
				new ArrayList<>(instanceMap.values()),
				new ArrayList<>(relationInstanceMap.values())
		);
	}

	@Override
	public Optional<Problem> trySerialize() {
		return Optional.of(serialize());
	}

	@Override
	public void close() {
		model.close();
	}

	public record Args(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
					   Provider<MetadataCreator> metadataCreatorProvider, RefineryDiagnostics diagnostics) {
	}
}
