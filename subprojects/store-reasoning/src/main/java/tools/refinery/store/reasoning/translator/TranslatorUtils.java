/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.ArrayList;
import java.util.Set;

public final class TranslatorUtils {
	private TranslatorUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Dnf createSupersetHelper(PartialRelation linkType, Set<PartialRelation> supersets) {
		return createSupersetHelper(linkType, supersets, Set.of());
	}

	public static Dnf createSupersetHelper(PartialRelation linkType, Set<PartialRelation> supersets,
										   Set<PartialRelation> oppositeSupersets) {
		int supersetCount = supersets.size();
		int oppositeSupersetCount = oppositeSupersets.size();
		int literalCount = supersetCount + oppositeSupersetCount;
		var direction = literalCount >= 1 ? ParameterDirection.OUT : ParameterDirection.IN;
		return Dnf.of(linkType.name() + "#superset", builder -> {
			var p1 = builder.parameter("p1", direction);
			var p2 = builder.parameter("p2", direction);
			var literals = new ArrayList<Literal>(literalCount);
			for (PartialRelation superset : supersets) {
				literals.add(superset.call(p1, p2));
			}
			for (PartialRelation oppositeSuperset : oppositeSupersets) {
				literals.add(oppositeSuperset.call(p2, p1));
			}
			builder.clause(literals);
		});
	}
}
