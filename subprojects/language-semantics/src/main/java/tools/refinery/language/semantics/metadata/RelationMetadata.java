/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.metadata;

import org.jetbrains.annotations.Nullable;
import tools.refinery.language.utils.Visibility;

import java.util.List;

public record RelationMetadata(String name, String simpleName, int arity, @Nullable List<String> parameterNames,
							   RelationDetail detail, @Nullable Visibility visibility) implements Metadata {
}
