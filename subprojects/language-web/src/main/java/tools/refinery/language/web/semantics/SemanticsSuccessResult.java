/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.gson.JsonObject;

import java.util.List;

public record SemanticsSuccessResult(List<String> nodes, JsonObject partialInterpretation) implements SemanticsResult {
}
