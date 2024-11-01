/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.naming;

import com.google.inject.Inject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.tests.InjectWithRefinery;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@InjectWithRefinery
class QualifiedNameConverterTest {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@ParameterizedTest
	@MethodSource
	void toStringTest(String expected, QualifiedName qualifiedName) {
		var string = qualifiedNameConverter.toString(qualifiedName);
		assertThat(string, is(expected));
	}

	static Stream<Arguments> toStringTest() {
		return Stream.of(
				Arguments.of("a", QualifiedName.create("a")),
				Arguments.of("a1", QualifiedName.create("a1")),
				Arguments.of("_", QualifiedName.create("_")),
				Arguments.of("a::b", QualifiedName.create("a", "b")),
				Arguments.of("'a b'", QualifiedName.create("a b")),
				Arguments.of("'11'", QualifiedName.create("11")),
				Arguments.of("'pred'", QualifiedName.create("pred")),
				Arguments.of("contains", QualifiedName.create("contains")),
				Arguments.of("decision", QualifiedName.create("decision")),
				Arguments.of("'a\\'b'", QualifiedName.create("a'b")),
				Arguments.of("'a\\nb'", QualifiedName.create("a\nb")),
				Arguments.of("'a b'::c", QualifiedName.create("a b", "c")),
				Arguments.of("'a b'::'c d'", QualifiedName.create("a b", "c d")),
				Arguments.of("a::'b c'", QualifiedName.create("a", "b c")),
				Arguments.of("'a b'::c1::_d", QualifiedName.create("a b", "c1", "_d")),
				Arguments.of("::a", QualifiedName.create("", "a")),
				Arguments.of("::'a b'", QualifiedName.create("", "a b")),
				Arguments.of("::'a b'::c1::_d", QualifiedName.create("", "a b", "c1", "_d")),
				Arguments.of("'نور'", QualifiedName.create("نور"))
		);
	}

	@ParameterizedTest
	@MethodSource
	void toQualifiedNameTest(String string, QualifiedName expected) {
		var qualifiedName = qualifiedNameConverter.toQualifiedName(string);
		assertThat(qualifiedName, is(expected));
	}

	static Stream<Arguments> toQualifiedNameTest() {
		return Stream.concat(toStringTest(), Stream.of(
				Arguments.of("'a'", QualifiedName.create("a")),
				Arguments.of("'contains'", QualifiedName.create("contains")),
				Arguments.of("'decision'", QualifiedName.create("decision")),
				Arguments.of("'a\nb'", QualifiedName.create("a\nb"))
		));
	}
}
