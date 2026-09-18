/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.PropertyDefinerBase;

public class GatedLogLevelDefiner extends PropertyDefinerBase {
	private String level;
	private String minLevel;

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getMinLevel() {
		return minLevel;
	}

	public void setMinLevel(String minLevel) {
		this.minLevel = minLevel;
	}

	@Override
	public String getPropertyValue() {
		var minValue = Level.toLevel(minLevel, Level.TRACE);
		var value = Level.toLevel(level, minValue);
		if (value.toInt() < minValue.toInt()) {
			value = minValue;
		}
		return value.toString();
	}
}
