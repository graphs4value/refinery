/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.statecoding.internal.StateCoderBuilderImpl;

public interface StateCoderAdapter extends ModelAdapter {
	StateCoderResult calculateStateCode();
	default int calculateModelCode() {
		return calculateStateCode().modelCode();
	}
	default ObjectCode calculateObjectCode() {
		return calculateStateCode().objectCode();
	}

	static StateCoderBuilder builder() {
		return new StateCoderBuilderImpl();
	}
}
