/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.cardinality.CardinalityInterval;

import java.util.Map;

public interface ScopePropagatorStoreAdapter extends ModelStoreAdapter {
	Map<PartialRelation, CardinalityInterval> getScopes();
}
