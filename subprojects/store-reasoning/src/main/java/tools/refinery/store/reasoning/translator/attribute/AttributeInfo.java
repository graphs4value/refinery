/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.attribute;

import tools.refinery.store.reasoning.representation.PartialRelation;

public record AttributeInfo(PartialRelation owningType, Object defaultValue) {
	public AttributeInfo(PartialRelation owningType) {
        this(owningType, null);
    }
}
