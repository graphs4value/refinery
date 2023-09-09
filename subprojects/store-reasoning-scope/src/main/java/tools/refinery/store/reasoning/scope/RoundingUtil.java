/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

final class RoundingUtil {
	private static final double TOLERANCE = 0.01;

	private RoundingUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static int roundUp(double value) {
		double ceil = Math.ceil(value - TOLERANCE);
		int intCeil = (int) ceil;
		return Math.max(intCeil, 0);
	}

	public static int roundDown(double value) {
		double floor = Math.floor(value + TOLERANCE);
		int intFloor = (int) floor;
		return Math.max(intFloor, 0);
	}
}
