/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.theory.internal;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import tools.refinery.language.annotations.Annotations;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.TheoryAction;
import tools.refinery.language.model.problem.TheoryDeclaration;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.semantics.theory.TheoryProvider;
import tools.refinery.language.utils.ServiceUtil;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.theory.Theory;
import tools.refinery.store.reasoning.theory.TheoryRule;

import java.util.Collection;
import java.util.List;

public class TheoryManager {
	private static final List<Class<? extends TheoryProvider>> DEFAULT_THEORY_PROVIDERS =
			ServiceUtil.loadServices(TheoryProvider.class);

	private final List<TheoryProvider> theoryProviders;
	private final IQualifiedNameProvider qualifiedNameProvider;
	private Annotations annotations;
	private ProblemTrace trace;
	private TheoryDispatch dispatch = new TheoryDispatch();

	@Inject
	public TheoryManager(Injector injector, IQualifiedNameProvider qualifiedNameProvider) {
		this.qualifiedNameProvider = qualifiedNameProvider;
		theoryProviders = ServiceUtil.instantiate(injector, DEFAULT_THEORY_PROVIDERS);
	}

	public void initialize(Annotations annotations, ProblemTrace trace, Collection<Problem> importedProblems) {
		this.annotations = annotations;
		this.trace = trace;
		dispatch = new TheoryDispatch();
		for (var problem : importedProblems) {
			for (var statement : problem.getStatements()) {
				if (statement instanceof TheoryDeclaration theoryDeclaration) {
					var theory = getTheory(theoryDeclaration);
					dispatch.register(theoryDeclaration, theory);
				}
			}
		}
	}

	private Theory getTheory(TheoryDeclaration theoryDeclaration) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(theoryDeclaration);
		if (qualifiedName == null) {
			throw new TracedException(theoryDeclaration, "No qualified name for theory.");
		}
		try {
			for (var theoryProvider : theoryProviders) {
				var result = theoryProvider.createTheory(qualifiedName, annotations, trace);
				if (result.isPresent()) {
					return result.get();
				}
			}
		} catch (RuntimeException e) {
			throw new TracedException(theoryDeclaration, "Error while instantiating theory.", e);
		}
		throw new TracedException(theoryDeclaration, "No theory registered for theory declaration '%s'."
				.formatted(theoryDeclaration.getName()));
	}

	public void addRule(TheoryAction action, TheoryRule rule) {
		dispatch.addRule(action, rule);
	}

	public void configure(ModelStoreBuilder storeBuilder) {
		// Register propagators in reverse order, because {@code PropagationBuilder} runs the last added propagator
		// first.
		for (var managedTheory : dispatch.getTheoriesToConfigure().reversed()) {
			managedTheory.createPropagator(storeBuilder);
		}
	}
}
