package tools.refinery.store.reasoning.translator;

import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.AnyPartialInterpretation;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class TranslationUnit {
	private ReasoningBuilder reasoningBuilder;

	protected ReasoningBuilder getPartialInterpretationBuilder() {
		return reasoningBuilder;
	}

	public void setPartialInterpretationBuilder(ReasoningBuilder reasoningBuilder) {
		this.reasoningBuilder = reasoningBuilder;
	}

	protected ModelStoreBuilder getModelStoreBuilder() {
		return reasoningBuilder.getStoreBuilder();
	}

	public abstract Collection<AnyPartialSymbol> getTranslatedPartialSymbols();

	public Collection<Advice> computeAdvices() {
		// No advices to give by default.
		return List.of();
	}

	public abstract void configure(Collection<Advice> advices);

	public abstract List<Literal> call(CallPolarity polarity, Modality modality, PartialRelation target,
									   List<Variable> arguments);

	public abstract Map<AnyPartialSymbol, AnyPartialInterpretation> createPartialInterpretations(Model model);

	public abstract void initializeModel(Model model, int nodeCount);
}
