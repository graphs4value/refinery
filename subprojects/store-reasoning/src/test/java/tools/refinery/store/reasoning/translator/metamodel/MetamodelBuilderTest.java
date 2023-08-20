/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import org.junit.jupiter.api.Test;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiplicity.ConstrainedMultiplicity;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MetamodelBuilderTest {
	private final PartialRelation university = new PartialRelation("University", 1);
	private final PartialRelation course = new PartialRelation("Course", 1);
	private final PartialRelation courses = new PartialRelation("courses", 2);
	private final PartialRelation location = new PartialRelation("location", 2);

	@Test
	void missingOppositeTest() {
		var builder = Metamodel.builder()
				.type(university)
				.type(course)
				.reference(courses, university, course, location)
				.reference(location, course, university);

		assertThrows(TranslationException.class, builder::build);
	}

	@Test
	void invalidOppositeTypeTest() {
		var builder = Metamodel.builder()
				.type(university)
				.type(course)
				.reference(courses, university, course, location)
				.reference(location, course, course, courses);

		assertThrows(TranslationException.class, builder::build);
	}

	@Test
	void invalidOppositeMultiplicityTest() {
		var invalidMultiplicity = new PartialRelation("invalidMultiplicity", 1);

		var builder = Metamodel.builder()
				.type(university)
				.type(course)
				.reference(courses, university, true, course, location)
				.reference(location, course,
						ConstrainedMultiplicity.of(CardinalityIntervals.atLeast(2), invalidMultiplicity),
						university,	courses);

		assertThrows(TranslationException.class, builder::build);
	}
}
