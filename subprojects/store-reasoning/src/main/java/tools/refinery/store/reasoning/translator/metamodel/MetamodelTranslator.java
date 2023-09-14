/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceTranslator;
import tools.refinery.store.reasoning.translator.crossreference.UndirectedCrossReferenceTranslator;
import tools.refinery.store.reasoning.translator.opposite.OppositeRelationTranslator;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;

public class MetamodelTranslator implements ModelStoreConfiguration {
	private final Metamodel metamodel;

	public MetamodelTranslator(Metamodel metamodel) {
		this.metamodel = metamodel;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.with(new TypeHierarchyTranslator(metamodel.typeHierarchy()));
		storeBuilder.with(new ContainmentHierarchyTranslator(metamodel.containmentHierarchy()));
		for (var entry : metamodel.directedCrossReferences().entrySet()) {
			storeBuilder.with(new DirectedCrossReferenceTranslator(entry.getKey(), entry.getValue()));
		}
		for (var entry : metamodel.undirectedCrossReferences().entrySet()) {
			storeBuilder.with(new UndirectedCrossReferenceTranslator(entry.getKey(), entry.getValue()));
		}
		for (var entry : metamodel.oppositeReferences().entrySet()) {
			storeBuilder.with(new OppositeRelationTranslator(entry.getKey(), entry.getValue()));
		}
	}
}
