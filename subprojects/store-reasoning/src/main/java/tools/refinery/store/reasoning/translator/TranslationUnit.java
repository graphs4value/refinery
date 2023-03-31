package tools.refinery.store.reasoning.translator;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;

import java.util.Collection;

public abstract class TranslationUnit {
	private ReasoningBuilder reasoningBuilder;

	protected ReasoningBuilder getReasoningBuilder() {
		return reasoningBuilder;
	}

	public void setPartialInterpretationBuilder(ReasoningBuilder reasoningBuilder) {
		this.reasoningBuilder = reasoningBuilder;
		configureReasoningBuilder();
	}

	protected ModelStoreBuilder getModelStoreBuilder() {
		return reasoningBuilder.getStoreBuilder();
	}

	protected void configureReasoningBuilder() {
		// Nothing to configure by default.
	}

	public abstract Collection<TranslatedRelation> getTranslatedRelations();

	public abstract void initializeModel(Model model, int nodeCount);
}
