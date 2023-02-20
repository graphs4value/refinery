package tools.refinery.store.partial.translator;

import tools.refinery.store.partial.PartialInterpretationBuilder;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.partial.AnyPartialSymbolInterpretation;
import tools.refinery.store.partial.literal.Modality;
import tools.refinery.store.partial.representation.AnyPartialSymbol;
import tools.refinery.store.partial.representation.PartialRelation;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class TranslationUnit {
	private PartialInterpretationBuilder partialInterpretationBuilder;

	protected PartialInterpretationBuilder getPartialInterpretationBuilder() {
		return partialInterpretationBuilder;
	}

	public void setPartialInterpretationBuilder(PartialInterpretationBuilder partialInterpretationBuilder) {
		this.partialInterpretationBuilder = partialInterpretationBuilder;
	}

	protected ModelStoreBuilder getModelStoreBuilder() {
		return partialInterpretationBuilder.getStoreBuilder();
	}

	public abstract Collection<AnyPartialSymbol> getTranslatedPartialSymbols();

	public Collection<Advice> computeAdvices() {
		// No advices to give by default.
		return List.of();
	}

	public abstract void configure(Collection<Advice> advices);

	public abstract List<Literal> call(CallPolarity polarity, Modality modality, PartialRelation target,
									   List<Variable> arguments);

	public abstract Map<AnyPartialSymbol, AnyPartialSymbolInterpretation> createPartialInterpretations(Model model);

	public abstract void initializeModel(Model model, int nodeCount);
}
