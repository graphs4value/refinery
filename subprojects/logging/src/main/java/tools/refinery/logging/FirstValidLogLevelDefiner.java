/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.PropertyDefinerBase;

import java.util.ArrayList;
import java.util.List;

public class FirstValidLogLevelDefiner extends PropertyDefinerBase {
	private final List<String> candidates = new ArrayList<>();

	public void addCandidate(String candidate) {
		candidates.add(candidate);
	}

	@Override
	public String getPropertyValue() {
		for (var candidate : candidates) {
			var level = candidate == null ? null : Level.toLevel(candidate, null);
			if (level != null) {
				return level.toString();
			}
		}
		return Level.TRACE.toString();
	}
}
