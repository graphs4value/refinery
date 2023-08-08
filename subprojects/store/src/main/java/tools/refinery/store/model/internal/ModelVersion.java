/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.map.Version;

import java.util.Arrays;

public class ModelVersion implements Version {
	final Version[] mapVersions;
	final int hash;

	public ModelVersion(Version[] mapVersions) {
		this.mapVersions = mapVersions;
		this.hash = Arrays.hashCode(mapVersions);
	}

	public static Version getInternalVersion(Version modelVersion, int interpretationIndex) {
		return ((ModelVersion) modelVersion).mapVersions[interpretationIndex];
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "ModelVersion{" +
				"mapVersions=" + Arrays.toString(mapVersions) +
				'}';
	}
}
