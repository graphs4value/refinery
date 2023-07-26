/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.state;

public class OldValueBox<V>{
	V oldValue;
	boolean isSet = false;

	public V getOldValue() {
		if(!isSet) throw new IllegalStateException();
		isSet = false;
		return oldValue;
	}

	public void setOldValue(V ouldValue) {
		if(isSet) throw new IllegalStateException();
		this.oldValue = ouldValue;
		isSet = true;
	}

}
