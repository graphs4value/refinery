/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.TruthValue;

import java.util.Collection;

public class ModelGenerator extends AbstractRefinery {
	private final Version initialVersion;

	private int randomSeed = 1;

	public ModelGenerator(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		super(problemTrace, store, modelSeed);
		initialVersion = model.commit();
	}

	public int getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(int randomSeed) {
		this.randomSeed = randomSeed;
	}

	public Collection<Version> run(int maxNumberOfSolutions) {
		var bestFirst = new BestFirstStoreManager(store, maxNumberOfSolutions);
		int currentRandomSeed = randomSeed;
		// Increment random seed even if generation is unsuccessful.
		randomSeed++;
		bestFirst.startExploration(initialVersion, currentRandomSeed);
		return bestFirst.getSolutionStore()
				.getSolutions()
				.stream()
				.map(VersionWithObjectiveValue::version)
				.toList();
	}

	public boolean tryRun() {
		var iterator = run(1).iterator();
		if (!iterator.hasNext()) {
			return false;
		}
		model.restore(iterator.next());
		return true;
	}

	public <A, C> PartialInterpretation<A, C> getCandidateInterpretation(PartialSymbol<A, C> partialSymbol) {
		return reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, partialSymbol);
	}

	public PartialInterpretation<TruthValue, Boolean> getCandidateInterpretation(Relation relation) {
		return getCandidateInterpretation(problemTrace.getPartialRelation(relation));
	}

	public PartialInterpretation<TruthValue, Boolean> getCandidateInterpretation(QualifiedName qualifiedName) {
		return getCandidateInterpretation(problemTrace.getPartialRelation(qualifiedName));
	}

	public PartialInterpretation<TruthValue, Boolean> getCandidateInterpretation(String qualifiedName) {
		return getCandidateInterpretation(problemTrace.getPartialRelation(qualifiedName));
	}

	public static ModelGeneratorBuilder standaloneBuilder() {
		var injector = StandaloneInjectorHolder.getInjector();
		return injector.getInstance(ModelGeneratorBuilder.class);
	}
}
