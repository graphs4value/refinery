/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.formatting2;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.xtext.formatting2.FormatterRequest;
import org.eclipse.xtext.formatting2.IFormatter2;
import org.eclipse.xtext.formatting2.regionaccess.ITextRegionAccess;
import org.eclipse.xtext.formatting2.regionaccess.ITextReplacement;
import org.eclipse.xtext.formatting2.regionaccess.TextRegionAccessBuilder;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ProblemFormatterTest {
	@Inject
	private ParseHelper<Problem> parseHelper;

	@Inject
	private Provider<FormatterRequest> formatterRequestProvider;

	@Inject
	private TextRegionAccessBuilder regionBuilder;

	@Inject
	private IFormatter2 formatter2;

	@Test
	void problemNameTest() {
		testFormatter("  problem  problem  .  ", "problem problem.\n");
	}

	@Test
	void assertionTest() {
		testFormatter("  equals (  a  ,  b  ,  *  )  :  true  .  ", "equals(a, b, *): true.\n");
	}

	@Test
	void defaultAssertionTest() {
		testFormatter("  default  equals (  a  ,  b  ,  *  )  :  true  .  ", "default equals(a, b, *): true.\n");
	}

	@Test
	void assertionShortTrueTest() {
		testFormatter("  equals (  a  ,  b  ,  *  )  .  ", "equals(a, b, *).\n");
	}

	@Test
	void defaultAssertionShortTrueTest() {
		testFormatter("  default  equals (  a  ,  b  ,  *  )  .  ", "default equals(a, b, *).\n");
	}

	@Test
	void assertionShortFalseTest() {
		testFormatter("  !  equals (  a  ,  b  ,  *  )  .  ", "!equals(a, b, *).\n");
	}

	@Test
	void defaultAssertionShortFalseTest() {
		testFormatter("  default  !  equals (  a  ,  b  ,  *  )  .  ", "default !equals(a, b, *).\n");
	}

	@Test
	void assertionShortUnknownTest() {
		testFormatter("  ?  equals (  a  ,  b  ,  *  )  .  ", "?equals(a, b, *).\n");
	}

	@Test
	void defaultAssertionShortUnknownTest() {
		testFormatter("  default  ?  equals (  a  ,  b  ,  *  )  .  ", "default ?equals(a, b, *).\n");
	}

	@Test
	void multipleAssertionsTest() {
		testFormatter("  exists (  a  )  .  ?  equals  (  a  ,  a  ).", """
				exists(a).
				?equals(a, a).
				""");
	}

	@Test
	void multipleAssertionsNamedProblemTest() {
		testFormatter("  problem  foo  .  exists (  a  )  .  ?  equals  (  a  ,  a  ).", """
				problem foo.

				exists(a).
				?equals(a, a).
				""");
	}

	@Test
	void classWithoutBodyTest() {
		testFormatter("  class  Foo  .  ", "class Foo.\n");
	}

	@Test
	void abstractClassWithoutBodyTest() {
		testFormatter("  abstract  class  Foo  .  ", "abstract class Foo.\n");
	}

	@Test
	void classExtendsWithoutBodyTest() {
		testFormatter("  class  Foo.  class  Bar  .  class  Quux  extends  Foo  ,  Bar  .  ", """
				class Foo.

				class Bar.

				class Quux extends Foo, Bar.
				""");
	}

	@Test
	void classWithEmptyBodyTest() {
		testFormatter("  class  Foo  {  }  ", """
				class Foo {
				}
				""");
	}

	@Test
	void classExtendsWithBodyTest() {
		testFormatter("  class  Foo.  class  Bar  .  class  Quux  extends  Foo  ,  Bar  {  }  ", """
				class Foo.

				class Bar.

				class Quux extends Foo, Bar {
				}
				""");
	}

	@Test
	void predicateWithoutBodyTest() {
		testFormatter("  pred  foo  (  node  a  ,  b  )  .  ", "pred foo(node a, b).\n");
	}

	@Test
	void predicateWithBodyTest() {
		testFormatter(
				"  pred  foo  (  node  a  ,  b  )  <->  equal  (a  ,  _c  )  ,  !  equal  (  a  ,  b  )  ;  equal+(  a  ,  b  )  .  ",
				"pred foo(node a, b) <-> equal(a, _c), !equal(a, b); equal+(a, b).\n");
	}

	@Test
	void predicatesWithoutBodyTest() {
		testFormatter("  pred  foo  (  node  a  ,  b  )  .  pred  bar  (  node  c  )  .  ", """
				pred foo(node a, b).

				pred bar(node c).
				""");
	}

	@Test
	void predicateCommentsTest() {
		testFormatter("""
				  % Some foo
				pred  foo  (  node  a  ,  b  )  .
				  % Some bar
				pred  bar  (  node  c  )  .
				""", """
				% Some foo
				pred foo(node a, b).

				% Some bar
				pred bar(node c).
				""");
	}

	@Test
	void atomDeclarationTest() {
		testFormatter("  atom  a  ,  b  .  ", "atom a, b.\n");
	}

	@Test
	void mixedDeclarationsTest() {
		testFormatter("""
				problem test.
				pred foo(node a).
				class Foo.
				foo(n1, n2).
				atom i1.
				!foo(i1, n1).
				pred bar(node a, node b).
				pred quux().
				default !bar(*, *).
				""", """
				problem test.

				pred foo(node a).

				class Foo.

				foo(n1, n2).
				atom i1.
				!foo(i1, n1).

				pred bar(node a, node b).

				pred quux().

				default !bar(*, *).
				""");
	}

	private void testFormatter(String toFormat, String expected) {
		Problem problem;
		try {
			problem = parseHelper.parse(toFormat);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse document", e);
		}
		var resource = (XtextResource) problem.eResource();
		FormatterRequest request = formatterRequestProvider.get();
		request.setAllowIdentityEdits(false);
		request.setFormatUndefinedHiddenRegionsOnly(false);
		ITextRegionAccess regionAccess = regionBuilder.forNodeModel(resource).create();
		request.setTextRegionAccess(regionAccess);
		List<ITextReplacement> replacements = formatter2.format(request);
		var formattedString = regionAccess.getRewriter().renderToString(replacements);
		assertThat(formattedString.replace("\r\n", "\n"), equalTo(expected.replace("\r\n", "\n")));
	}
}
