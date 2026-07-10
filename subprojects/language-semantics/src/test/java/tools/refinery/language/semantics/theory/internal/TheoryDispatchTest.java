/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.theory.internal;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.junit.jupiter.api.Test;
import tools.refinery.language.model.problem.ProblemFactory;
import tools.refinery.language.model.problem.TheoryAction;
import tools.refinery.language.model.problem.TheoryDeclaration;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.theory.Theory;
import tools.refinery.store.reasoning.theory.TheoryRule;
import tools.refinery.store.reasoning.theory.TheorySupport;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.reasoning.theory.TheorySupport.EXPLICIT_ONLY;
import static tools.refinery.store.reasoning.theory.TheorySupport.UNSUPPORTED;
import static tools.refinery.store.reasoning.theory.TheorySupport.enabledByDefault;

class TheoryDispatchTest {
	private final TheoryDispatch dispatch = new TheoryDispatch();

	@Test
	void overrideRoutesToListedTheoriesOnly() {
		var a = register("a", enabledByDefault(0));
		var b = register("b", enabledByDefault(0));
		var c = register("c", enabledByDefault(0));
		var rule = rule();

		dispatch.addRule(override(a, b), rule);

		assertThat(rulesRoutedTo(a), contains(rule));
		assertThat(rulesRoutedTo(b), contains(rule));
		assertThat(rulesRoutedTo(c), is(empty()));
	}

	@Test
	void overrideIgnoresPreferenceAndUsesExplicitOnlyTheory() {
		// A theory that would never be picked by default is still usable through the {@code using} keyword.
		var explicit = register("explicit", EXPLICIT_ONLY);
		var rule = rule();

		dispatch.addRule(override(explicit), rule);

		assertThat(rulesRoutedTo(explicit), contains(rule));
	}

	@Test
	void overrideWithUnknownTheoryThrows() {
		register("known", enabledByDefault(0));
		var unregistered = declaration("unregistered");

		var exception = assertThrows(TracedException.class,
				() -> dispatch.addRule(overrideDeclarations(unregistered), rule()));
		assertThat(exception.getMessage(), containsString("Unknown theory 'unregistered'."));
	}

	@Test
	void overrideWithUnsupportedTheoryThrows() {
		var unsupported = register("unsupported", UNSUPPORTED);

		var exception = assertThrows(TracedException.class,
				() -> dispatch.addRule(override(unsupported), rule()));
		assertThat(exception.getMessage(), containsString("Theory 'unsupported' does not support this assertion."));
	}

	@Test
	void overrideWithUnresolvedReferenceThrows() {
		var proxy = declaration("proxy");
		((InternalEObject) proxy).eSetProxyURI(URI.createURI("unresolved#theory"));

		var exception = assertThrows(TracedException.class,
				() -> dispatch.addRule(overrideDeclarations(proxy), rule()));
		assertThat(exception.getMessage(), containsString("Unresolved reference to theory."));
	}

	@Test
	void defaultRoutesToHighestPreferenceTheory() {
		var low = register("low", enabledByDefault(0));
		var high = register("high", enabledByDefault(10));
		var rule = rule();

		dispatch.addRule(defaultAction(), rule);

		assertThat(rulesRoutedTo(high), contains(rule));
		assertThat(rulesRoutedTo(low), is(empty()));
	}

	@Test
	void defaultRoutesToAllTiedTheories() {
		// Because refinement is monotonic, all theories tied at the highest preference should handle the rule so
		// that their refinements are combined.
		var first = register("first", enabledByDefault(5));
		var second = register("second", enabledByDefault(5));
		var loser = register("loser", enabledByDefault(4));
		var rule = rule();

		dispatch.addRule(defaultAction(), rule);

		assertThat(rulesRoutedTo(first), contains(rule));
		assertThat(rulesRoutedTo(second), contains(rule));
		assertThat(rulesRoutedTo(loser), is(empty()));
	}

	@Test
	void defaultSkipsExplicitOnlyTheoriesButReportsThemAsSupported() {
		// An explicit-only theory can handle the rule, so the error must point the user at the {@code using} keyword
		// rather than claiming the assertion is unsupported.
		register("explicit", EXPLICIT_ONLY);

		var exception = assertThrows(TracedException.class,
				() -> dispatch.addRule(defaultAction(), rule()));
		assertThat(exception.getMessage(), containsString("No default theory is available for this assertion."));
		assertThat(exception.getMessage(), containsString("using"));
	}

	@Test
	void defaultWithNoSupportingTheoryThrows() {
		register("unsupported", UNSUPPORTED);

		var exception = assertThrows(TracedException.class,
				() -> dispatch.addRule(defaultAction(), rule()));
		assertThat(exception.getMessage(), containsString("No imported theory supports this assertion."));
	}

	@Test
	void defaultWithNoTheoriesAtAllThrows() {
		var exception = assertThrows(TracedException.class,
				() -> dispatch.addRule(defaultAction(), rule()));
		assertThat(exception.getMessage(), containsString("No imported theory supports this assertion."));
	}

	@Test
	void higherPriorityTheoriesAreConfiguredFirst() {
		var diverging = register("diverging", enabledByDefault(0), -100);
		var normal = register("normal", enabledByDefault(0), 0);
		var eager = register("eager", enabledByDefault(0), 100);
		// Route the rule to every theory so that all of them appear in the configuration order.
		dispatch.addRule(override(diverging, normal, eager), rule());

		assertThat(configurationOrder(), contains("eager", "normal", "diverging"));
	}

	@Test
	void equalPriorityTheoriesKeepRegistrationOrder() {
		var first = register("first", enabledByDefault(0), 0);
		var second = register("second", enabledByDefault(0), 0);
		var third = register("third", enabledByDefault(0), 0);
		dispatch.addRule(override(first, second, third), rule());

		assertThat(configurationOrder(), contains("first", "second", "third"));
	}

	@Test
	void theoriesWithoutRulesAreNotConfigured() {
		register("used", enabledByDefault(10));
		register("unused", enabledByDefault(0));
		dispatch.addRule(defaultAction(), rule());

		assertThat(configurationOrder(), contains("used"));
	}

	@Test
	void createPropagatorWrapsErrorsWithTrace() {
		var faulty = register("faulty", enabledByDefault(0));
		faulty.theory.failOnConfigure = true;
		dispatch.addRule(defaultAction(), rule());
		var managedTheory = dispatch.getTheoriesToConfigure().getFirst();

		// The real store builder is enough here; the faulty theory throws before touching it.
		var exception = assertThrows(TracedException.class,
				() -> managedTheory.createPropagator(ModelStore.builder()));
		assertThat(exception.getMessage(), containsString("Error while creating propagator."));
		assertThat(exception.getSourceElement(), is(faulty.declaration));
	}

	private List<String> configurationOrder() {
		return dispatch.getTheoriesToConfigure().stream()
				.map(TheoryDispatch.ManagedTheory::toString)
				.toList();
	}

	private List<TheoryRule> rulesRoutedTo(Registered theory) {
		return dispatch.getManagedTheory(theory.declaration).getRules();
	}

	private Registered register(String name, TheorySupport support) {
		return register(name, support, Theory.DEFAULT_PRIORITY);
	}

	private Registered register(String name, TheorySupport support, int priority) {
		var declaration = declaration(name);
		var theory = new FakeTheory(support, priority);
		dispatch.register(declaration, theory);
		return new Registered(declaration, theory);
	}

	private static TheoryDeclaration declaration(String name) {
		var declaration = ProblemFactory.eINSTANCE.createTheoryDeclaration();
		declaration.setName(name);
		return declaration;
	}

	private static TheoryAction defaultAction() {
		return ProblemFactory.eINSTANCE.createTheoryAction();
	}

	private static TheoryAction override(Registered... theories) {
		var declarations = new TheoryDeclaration[theories.length];
		for (int i = 0; i < theories.length; i++) {
			declarations[i] = theories[i].declaration;
		}
		return overrideDeclarations(declarations);
	}

	private static TheoryAction overrideDeclarations(TheoryDeclaration... theories) {
		var action = ProblemFactory.eINSTANCE.createTheoryAction();
		action.setTheoryOverride(true);
		action.getTheories().addAll(List.of(theories));
		return action;
	}

	// The dispatch logic never inspects the rule contents, so a placeholder rule is enough.
	private static TheoryRule rule() {
		return new TheoryRule(null, null);
	}

	private record Registered(TheoryDeclaration declaration, FakeTheory theory) {
	}

	private static final class FakeTheory implements Theory {
		private final TheorySupport support;
		private final int priority;
		private boolean failOnConfigure;

		private FakeTheory(TheorySupport support, int priority) {
			this.support = support;
			this.priority = priority;
		}

		@Override
		public TheorySupport checkSupport(TheoryRule theoryRule) {
			return support;
		}

		@Override
		public int getPriority() {
			return priority;
		}

		@Override
		public void createPropagator(ModelStoreBuilder storeBuilder, Collection<TheoryRule> collectedRules) {
			if (failOnConfigure) {
				throw new IllegalStateException("Propagator creation failed");
			}
		}
	}
}
