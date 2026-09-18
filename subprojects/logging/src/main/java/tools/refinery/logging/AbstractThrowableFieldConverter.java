/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;

abstract class AbstractThrowableFieldConverter extends ClassicConverter {
	@Override
	public final String convert(ILoggingEvent event) {
		IThrowableProxy proxy = event.getThrowableProxy();
		if (proxy == null) {
			return CoreConstants.EMPTY_STRING;
		}
		String value = extract(proxy);
		return value == null ? CoreConstants.EMPTY_STRING : value;
	}

	protected abstract String extract(IThrowableProxy proxy);
}
