/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;

public class ThrowableMessageConverter extends AbstractThrowableFieldConverter {
	@Override
	protected String extract(IThrowableProxy proxy) {
		return proxy.getMessage();
	}
}
