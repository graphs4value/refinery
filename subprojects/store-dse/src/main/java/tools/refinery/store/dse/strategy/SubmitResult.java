/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;

public record SubmitResult(boolean include, boolean accepted, ObjectiveValue objective, VersionWithObjectiveValue newVersion) {

	}
