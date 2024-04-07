/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

import java.util.List;

public record Signature(List<FixedType> parameterTypes, FixedType resultType) {
}
