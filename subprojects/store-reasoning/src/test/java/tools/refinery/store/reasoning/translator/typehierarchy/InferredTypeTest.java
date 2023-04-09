/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import org.junit.jupiter.api.Test;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class InferredTypeTest {
	private final PartialRelation c1 = new PartialRelation("C1", 1);
	private final PartialRelation c2 = new PartialRelation("C2", 1);

	@Test
	void untypedIsConsistentTest() {
		var sut = new InferredType(Set.of(), Set.of(c1, c2), null);
		assertThat(sut.isConsistent(), is(true));
	}

	@Test
	void typedIsConsistentTest() {
		var sut = new InferredType(Set.of(c1), Set.of(c1, c2), c1);
		assertThat(sut.isConsistent(), is(true));
	}

	@Test
	void typedIsInconsistentTest() {
		var sut = new InferredType(Set.of(c1), Set.of(), null);
		assertThat(sut.isConsistent(), is(false));
	}
}
