/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import org.eclipse.xtext.web.server.IServiceResult;

public sealed interface SemanticsResult extends IServiceResult permits SemanticsSuccessResult,
		SemanticsInternalErrorResult, SemanticsIssuesResult {
}
