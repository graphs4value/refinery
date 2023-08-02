/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.map.Version;

import java.util.Arrays;

public record ModelVersion(Version[] mapVersions) implements Version{

	public static Version getInternalVersion(Version modelVersion, int interpretationIndex) {
		return ((ModelVersion)modelVersion).mapVersions()[interpretationIndex];
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(mapVersions);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ModelVersion that = (ModelVersion) o;

		return Arrays.equals(mapVersions, that.mapVersions);
	}

	@Override
	public String toString() {
		return "ModelVersion{" +
				"mapVersions=" + Arrays.toString(mapVersions) +
				'}';
	}
}
