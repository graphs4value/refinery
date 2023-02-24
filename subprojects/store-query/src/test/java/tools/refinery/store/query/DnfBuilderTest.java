package tools.refinery.store.query;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.literal.BooleanLiteral;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.representation.Symbol;

import static org.hamcrest.MatcherAssert.assertThat;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class DnfBuilderTest {
	@Test
	void eliminateTrueTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(BooleanLiteral.TRUE, friendView.call(p, q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateFalseTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p), BooleanLiteral.FALSE)
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysTrueTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(BooleanLiteral.TRUE)
				.build();
		var expected = Dnf.builder().parameters(p, q).clause().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysFalseTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q), BooleanLiteral.FALSE)
				.build();
		var expected = Dnf.builder().parameters(p, q).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateTrueDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var trueDnf = Dnf.builder().parameter(p).clause().build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(trueDnf.call(q), friendView.call(p, q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateFalseDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var falseDnf = Dnf.builder().parameter(p).build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p), falseDnf.call(q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysTrueDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var trueDnf = Dnf.builder().parameter(p).clause().build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(trueDnf.call(q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysFalseDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var falseDnf = Dnf.builder().parameter(p).build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q), falseDnf.call(q))
				.build();
		var expected = Dnf.builder().parameters(p, q).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateNotFalseDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var falseDnf = Dnf.builder().parameter(p).build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(not(falseDnf.call(q)), friendView.call(p, q))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void eliminateNotTrueDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var trueDnf = Dnf.builder().parameter(p).clause().build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p), not(trueDnf.call(q)))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause(friendView.call(p, q)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysNotFalseDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var falseDnf = Dnf.builder().parameter(p).build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(not(falseDnf.call(q)))
				.build();
		var expected = Dnf.builder().parameters(p, q).clause().build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void alwaysNotTrueDnfTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyRelationView<>(friend);
		var trueDnf = Dnf.builder().parameter(p).clause().build();

		var actual = Dnf.builder()
				.parameters(p, q)
				.clause(friendView.call(p, q), not(trueDnf.call(q)))
				.build();
		var expected = Dnf.builder().parameters(p, q).build();

		assertThat(actual, structurallyEqualTo(expected));
	}
}
