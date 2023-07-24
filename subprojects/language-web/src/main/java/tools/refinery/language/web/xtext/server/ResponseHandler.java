/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server;

import tools.refinery.language.web.xtext.server.message.XtextWebResponse;

@FunctionalInterface
public interface ResponseHandler {
	void onResponse(XtextWebResponse response) throws ResponseHandlerException;
}
