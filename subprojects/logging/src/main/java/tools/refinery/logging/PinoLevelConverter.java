/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class PinoLevelConverter extends ClassicConverter {
	@Override public String convert(ILoggingEvent e) {
		return switch (e.getLevel().toInt()) {
			case Level.TRACE_INT -> "10";
			case Level.DEBUG_INT -> "20";
			case Level.WARN_INT  -> "40";
			case Level.ERROR_INT -> "50";
			default -> "30";
		};
	}
}
