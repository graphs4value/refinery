/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.statespace.internal;

import tools.refinery.store.map.Version;
import tools.refinery.visualization.statespace.VisualizationStore;

import java.util.HashMap;
import java.util.Map;

public class VisualizationStoreImpl implements VisualizationStore {

	private final Map<Version, Integer> states = new HashMap<>();
	private int transitionCounter = 0;
	private Integer numberOfStates = 0;
	private final StringBuilder designSpaceBuilder = new StringBuilder();

	@Override
	public synchronized void addState(Version state, String label) {
		if (states.containsKey(state)) {
			return;
		}
		states.put(state, numberOfStates++);
		designSpaceBuilder.append(states.get(state)).append(" [label = \"").append(states.get(state)).append(" (");
		designSpaceBuilder.append(label);
		designSpaceBuilder.append(")\"\n").append("URL=\"./").append(states.get(state)).append(".svg\"]\n");
	}

	@Override
	public synchronized void addSolution(Version state) {
		designSpaceBuilder.append(states.get(state)).append(" [peripheries = 2]\n");
	}

	@Override
	public synchronized void addTransition(Version from, Version to, String label) {
		designSpaceBuilder.append(states.get(from)).append(" -> ").append(states.get(to))
				.append(" [label=\"").append(transitionCounter++).append(": ").append(label).append("\"]\n");
	}

	public synchronized StringBuilder getDesignSpaceStringBuilder() {
		return designSpaceBuilder;
	}

	@Override
	public Map<Version, Integer> getStates() {
		return states;
	}
}
