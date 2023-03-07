package tools.refinery.store.reasoning.translator.base;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.reasoning.seed.UniformSeed;
import tools.refinery.store.reasoning.translator.TranslatedRelation;
import tools.refinery.store.reasoning.translator.TranslationUnit;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;

import java.util.Collection;
import java.util.List;

public class BaseDecisionTranslationUnit extends TranslationUnit {
	private final PartialRelation partialRelation;
	private final Seed<TruthValue> seed;
	private final Symbol<TruthValue> symbol;

	public BaseDecisionTranslationUnit(PartialRelation partialRelation, Seed<TruthValue> seed) {
		if (seed.arity() != partialRelation.arity()) {
			throw new IllegalArgumentException("Expected seed with arity %d for %s, got arity %s"
					.formatted(partialRelation.arity(), partialRelation, seed.arity()));
		}
		this.partialRelation = partialRelation;
		this.seed = seed;
		symbol = new Symbol<>(partialRelation.name(), partialRelation.arity(), TruthValue.class, TruthValue.UNKNOWN);
	}

	public BaseDecisionTranslationUnit(PartialRelation partialRelation) {
		this(partialRelation, new UniformSeed<>(partialRelation.arity(), TruthValue.UNKNOWN));
	}

	@Override
	protected void configureReasoningBuilder() {
		getModelStoreBuilder().symbol(symbol);
	}

	@Override
	public Collection<TranslatedRelation> getTranslatedRelations() {
		return List.of(new TranslatedBaseDecision(getReasoningBuilder(), partialRelation, symbol));
	}

	@Override
	public void initializeModel(Model model, int nodeCount) {
		var interpretation = model.getInterpretation(symbol);
		interpretation.putAll(seed.getCursor(TruthValue.UNKNOWN, nodeCount));
	}
}
