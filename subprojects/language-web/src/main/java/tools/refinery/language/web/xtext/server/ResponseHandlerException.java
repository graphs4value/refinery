/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server;

import java.io.Serial;

public class ResponseHandlerException extends Exception {

	@Serial
	private static final long serialVersionUID = 3589866922420268164L;

	public ResponseHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResponseHandlerException(String message) {
		super(message);
	}
}
