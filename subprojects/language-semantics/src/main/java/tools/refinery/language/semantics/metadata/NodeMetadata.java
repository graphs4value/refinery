/*
 * SPDX-FileCopyrightText: 2023-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.metadata;

import org.jetbrains.annotations.Nullable;

public record NodeMetadata(String name, String simpleName, NodeKind kind, @Nullable String color) implements Metadata {
}
