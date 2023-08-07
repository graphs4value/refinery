/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighbourhood;

import tools.refinery.store.statecoding.ObjectCode;

import java.util.Arrays;

public class ObjectCodeImpl implements ObjectCode {
	private long[] vector;
	private int size;

	public ObjectCodeImpl() {
		vector = new long[10];
		size = 0;
	}

	public ObjectCodeImpl(ObjectCodeImpl sameSize) {
		this.vector = new long[sameSize.size];
		this.size = sameSize.size;
	}

	public void ensureSize(int object) {
		if(object >= size) {
			size = object+1;
		}

		if(object >= vector.length) {
			int newLength = vector.length*2;
			while(object >= newLength) {
				newLength*=2;
			}

			long[] newVector = new long[newLength];
			System.arraycopy(vector, 0, newVector, 0, vector.length);
			this.vector = newVector;
		}
	}

	public long get(int object) {
		if(object < vector.length) {
			return vector[object];
		} else {
			return 0;
		}
	}

	public void set(int object, long value) {
		ensureSize(object);
		final long valueToPut = value == 0 ? 1 : value;
		vector[object]=valueToPut;
	}

	public int getSize() {
		return this.size;
	}

	@Override
	public String toString() {
		return "ObjectCodeImpl{" +
				"vector=" + Arrays.toString(Arrays.copyOf(vector,this.size)) +
				'}';
	}
}
