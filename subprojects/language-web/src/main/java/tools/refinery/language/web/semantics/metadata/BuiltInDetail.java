/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics.metadata;

public record BuiltInDetail() implements RelationDetail {
	public static final BuiltInDetail INSTANCE = new BuiltInDetail();
}
