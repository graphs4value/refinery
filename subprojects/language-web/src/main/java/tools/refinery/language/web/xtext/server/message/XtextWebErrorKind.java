/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.message;

import com.google.gson.annotations.SerializedName;

public enum XtextWebErrorKind {
	@SerializedName("request")
	REQUEST_ERROR,

	@SerializedName("server")
	SERVER_ERROR,
}
