/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

public interface ObjectiveValue {
	double get(int index);
	int getSize();

	static ObjectiveValue of(double v1) {
		return new ObjectiveValues.ObjectiveValue1(v1);
	}

	static ObjectiveValue of(double v1, double v2) {
		return new ObjectiveValues.ObjectiveValue2(v1,v2);
	}

	static ObjectiveValue of(double[] v) {
		return new ObjectiveValues.ObjectiveValueN(v);
	}

}
