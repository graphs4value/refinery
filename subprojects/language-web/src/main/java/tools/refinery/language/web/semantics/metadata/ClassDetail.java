/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics.metadata;

public record ClassDetail(boolean abstractClass) implements RelationDetail {
	public static final ClassDetail CONCRETE_CLASS = new ClassDetail(false);

	public static final ClassDetail ABSTRACT_CLASS = new ClassDetail(true);

	public static ClassDetail ofAbstractClass(boolean abstractClass) {
		return abstractClass ? ABSTRACT_CLASS : CONCRETE_CLASS;
	}
}
