/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

public interface Strategy {

	void initStrategy(DesignSpaceExplorationAdapter designSpaceExplorationAdapter);

	void explore();
}
