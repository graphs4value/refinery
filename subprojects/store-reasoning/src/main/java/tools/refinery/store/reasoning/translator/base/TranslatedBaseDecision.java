package tools.refinery.store.reasoning.translator.base;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.reasoning.PartialInterpretation;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.Advice;
import tools.refinery.store.reasoning.translator.TranslatedRelation;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;

import java.util.List;

class TranslatedBaseDecision implements TranslatedRelation {
	private final ReasoningBuilder reasoningBuilder;
	private final PartialRelation partialRelation;
	private final Symbol<TruthValue> symbol;

	public TranslatedBaseDecision(ReasoningBuilder reasoningBuilder, PartialRelation partialRelation,
								  Symbol<TruthValue> symbol) {
		this.reasoningBuilder = reasoningBuilder;
		this.partialRelation = partialRelation;
		this.symbol = symbol;
	}

	@Override
	public PartialRelation getSource() {
		return partialRelation;
	}

	@Override
	public void configure(List<Advice> advices) {

	}

	@Override
	public List<Literal> call(CallPolarity polarity, Modality modality, List<Variable> arguments) {
		return null;
	}

	@Override
	public PartialInterpretation<TruthValue, Boolean> createPartialInterpretation(Model model) {
		return null;
	}
}
