/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.theory.internal;

import tools.refinery.language.model.problem.TheoryAction;
import tools.refinery.language.model.problem.TheoryDeclaration;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.theory.Theory;
import tools.refinery.store.reasoning.theory.TheoryRule;
import tools.refinery.store.reasoning.theory.TheorySupport;

import java.util.*;

/**
 * Decides which theories a {@link TheoryRule} is routed to and in which order the theories create their propagators.
 * <p>
 *     This holds the theory-dispatch logic of {@link TheoryManager} that does not depend on the Guice
 *     {@code Injector} or the {@link tools.refinery.language.semantics.theory.TheoryProvider} service loader. The
 *     {@link TheoryManager} turns {@link TheoryDeclaration} statements into {@link Theory} instances and
 *     {@link #register(TheoryDeclaration, Theory) registers} them here; this class then owns all decisions about
 *     routing rules to theories and the order in which they should be configured.
 * </p>
 */
class TheoryDispatch {
	private final Map<TheoryDeclaration, ManagedTheory> theories = new LinkedHashMap<>();

	/**
	 * Registers a theory instantiated for a theory declaration so that rules may be routed to it.
	 *
	 * @param declaration The theory declaration.
	 * @param theory The instantiated theory.
	 */
	public void register(TheoryDeclaration declaration, Theory theory) {
		theories.put(declaration, new ManagedTheory(declaration, theory));
	}

	public ManagedTheory getManagedTheory(TheoryDeclaration theoryDeclaration) {
		return Optional.ofNullable(theories.get(theoryDeclaration))
				.orElseThrow(() -> new IllegalArgumentException("Theory was not registered"));
	}

	/**
	 * Routes a rule to the theories that should handle it according to the {@code action}.
	 * <p>
	 *     If the action uses the {@code using} keyword ({@link TheoryAction#isTheoryOverride()}), the rule is routed
	 *     to exactly the listed theories, each of which must support it. Otherwise, the rule is routed to the theories
	 *     that wish to handle it by default with the highest {@link TheorySupport.EnabledByDefault#preference()
	 *     preference}, routing to all of them when several are tied.
	 * </p>
	 *
	 * @param action The theory action that produced the rule.
	 * @param rule The rule to route.
	 */
	public void addRule(TheoryAction action, TheoryRule rule) {
		if (action.isTheoryOverride()) {
			addRuleWithTheoryOverride(action, rule);
		} else {
			addRuleWithDefaultTheories(action, rule);
		}
	}

	private void addRuleWithTheoryOverride(TheoryAction action, TheoryRule rule) {
		for (var theory : action.getTheories()) {
			if (theory == null || theory.eIsProxy()) {
				throw new TracedException(action, "Unresolved reference to theory.");
			}
			var managedTheory = theories.get(theory);
			if (managedTheory == null) {
				throw new TracedException(action, "Unknown theory '%s'.".formatted(theory.getName()));
			}
			var support = managedTheory.checkSupport(rule);
			if (!support.isSupported()) {
				throw new TracedException(action, "Theory '%s' does not support this assertion."
						.formatted(theory.getName()));
			}
			managedTheory.addRule(rule);
		}
	}

	private void addRuleWithDefaultTheories(TheoryAction action, TheoryRule rule) {
		int maxPreference = Integer.MIN_VALUE;
		var matchedTheories = new ArrayList<ManagedTheory>();
		boolean supported = false;
		for (var managedTheory : theories.values()) {
			var support = managedTheory.checkSupport(rule);
			supported = supported || support.isSupported();
			if (support instanceof TheorySupport.EnabledByDefault(int preference)) {
				if (preference < maxPreference) {
					continue;
				}
				if (preference > maxPreference) {
					maxPreference = preference;
					matchedTheories.clear();
				}
				matchedTheories.add(managedTheory);
			}
		}
		if (matchedTheories.isEmpty()) {
			if (supported) {
				throw new TracedException(action, "No default theory is available for this assertion. " +
						"Use the 'using' keyword to specify the intended theories.");
			}
			throw new TracedException(action, "No imported theory supports this assertion.");
		}
		for (var managedTheory : matchedTheories) {
			managedTheory.addRule(rule);
		}
	}

	/**
	 * Returns the theories that have collected at least one rule, in the order in which their propagators should be
	 * executed.
	 * <p>
	 *     Theories with a higher {@link Theory#getPriority() priority} run earlier in the sequence of propagators.
	 *     Theories with equal priority keep their registration order. Theories without any rules are omitted, since
	 *     they would not create a propagator.
	 * </p>
	 *
	 * @return The theories to configure, in propagator execution order.
	 */
	public List<ManagedTheory> getTheoriesToConfigure() {
		var result = new ArrayList<ManagedTheory>();
		for (var managedTheory : theories.values()) {
			if (managedTheory.hasRules()) {
				result.add(managedTheory);
			}
		}
		// Larger priority theories should run first.
		result.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
		return result;
	}

	public static final class ManagedTheory {
		private final TheoryDeclaration declaration;
		private final Theory theory;
		private final List<TheoryRule> rules = new ArrayList<>();

		private ManagedTheory(TheoryDeclaration declaration, Theory theory) {
			this.declaration = declaration;
			this.theory = theory;
		}

		public TheoryDeclaration getDeclaration() {
			return declaration;
		}

		public List<TheoryRule> getRules() {
			return Collections.unmodifiableList(rules);
		}

		public int getPriority() {
			return theory.getPriority();
		}

		public boolean hasRules() {
			return !rules.isEmpty();
		}

		private TheorySupport checkSupport(TheoryRule rule) {
			return theory.checkSupport(rule);
		}

		private void addRule(TheoryRule rule) {
			rules.add(rule);
		}

		public void createPropagator(ModelStoreBuilder storeBuilder) {
			try {
				theory.createPropagator(storeBuilder, rules);
			} catch (RuntimeException e) {
				throw new TracedException(declaration, "Error while creating propagator.", e);
			}
		}

		@Override
		public String toString() {
			return declaration.getName();
		}
	}
}
