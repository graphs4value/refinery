/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.Arrays;

public class TypeHierarchyInitializer implements PartialModelInitializer {
	private final TypeHierarchy typeHierarchy;
	private final Symbol<InferredType> typeSymbol;

	public TypeHierarchyInitializer(TypeHierarchy typeHierarchy, Symbol<InferredType> typeSymbol) {
		this.typeHierarchy = typeHierarchy;
		this.typeSymbol = typeSymbol;
	}

	@Override
	public void initialize(Model model, ModelSeed modelSeed) {
		var inferredTypes = new InferredType[modelSeed.getNodeCount()];
		Arrays.fill(inferredTypes, typeHierarchy.getUnknownType());
		for (var type : typeHierarchy.getAllTypes()) {
			initializeType(type, inferredTypes, modelSeed);
		}
		var typeInterpretation = model.getInterpretation(typeSymbol);
		for (int i = 0; i < inferredTypes.length; i++) {
			typeInterpretation.put(Tuple.of(i), inferredTypes[i]);
		}
	}

	private void initializeType(PartialRelation type, InferredType[] inferredTypes, ModelSeed modelSeed) {
		var cursor = modelSeed.getCursor(type, TruthValue.UNKNOWN);
		var analysisResult = typeHierarchy.getAnalysisResult(type);
		while (cursor.move()) {
			var i = cursor.getKey().get(0);
			checkNodeId(inferredTypes, i);
			var value = cursor.getValue();
			inferredTypes[i] = analysisResult.merge(inferredTypes[i], value);
		}
	}

	private void checkNodeId(InferredType[] inferredTypes, int nodeId) {
		if (nodeId < 0 || nodeId >= inferredTypes.length) {
			throw new IllegalArgumentException("Expected node id %d to be lower than model size %d"
					.formatted(nodeId, inferredTypes.length));
		}
	}
}
