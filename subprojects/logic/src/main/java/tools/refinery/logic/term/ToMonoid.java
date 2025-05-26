/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

@FunctionalInterface
public interface ToMonoid<T, R> {
	R apply(int count, T value);
}
