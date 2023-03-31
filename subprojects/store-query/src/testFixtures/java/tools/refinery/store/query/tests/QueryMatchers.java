package tools.refinery.store.query.tests;

import org.hamcrest.Matcher;
import tools.refinery.store.query.dnf.Dnf;

public final class QueryMatchers {
	private QueryMatchers() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static Matcher<Dnf> structurallyEqualTo(Dnf expected) {
		return new StructurallyEqualTo(expected);
	}
}
