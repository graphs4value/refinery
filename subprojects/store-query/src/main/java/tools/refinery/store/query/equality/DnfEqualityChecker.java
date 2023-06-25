/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.equality;

import tools.refinery.store.query.dnf.Dnf;

import java.util.Objects;

@FunctionalInterface
public interface DnfEqualityChecker {
	DnfEqualityChecker DEFAULT = Objects::equals;

	boolean dnfEqual(Dnf left, Dnf right);
}
