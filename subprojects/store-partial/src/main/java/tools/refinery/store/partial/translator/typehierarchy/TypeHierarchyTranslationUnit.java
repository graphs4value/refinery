package tools.refinery.store.partial.translator.typehierarchy;

import tools.refinery.store.partial.AnyPartialSymbolInterpretation;
import tools.refinery.store.partial.literal.Modality;
import tools.refinery.store.partial.representation.AnyPartialSymbol;
import tools.refinery.store.partial.representation.PartialRelation;
import tools.refinery.store.partial.translator.Advice;
import tools.refinery.store.partial.translator.TranslationUnit;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.representation.Symbol;

import java.util.*;

public class TypeHierarchyTranslationUnit extends TranslationUnit {
	static final Symbol<InferredType> INFERRED_TYPE_SYMBOL = new Symbol<>("inferredType", 1,
			InferredType.class, InferredType.UNTYPED);

	private final Map<PartialRelation, TypeInfo> typeInfoMap;

	public TypeHierarchyTranslationUnit(Map<PartialRelation, TypeInfo> typeInfoMap) {
		this.typeInfoMap = new LinkedHashMap<>(typeInfoMap);
	}

	@Override
	public Collection<AnyPartialSymbol> getTranslatedPartialSymbols() {
		return null;
	}

	@Override
	public void configure(Collection<Advice> advices) {

	}

	@Override
	public List<Literal> call(CallPolarity polarity, Modality modality, PartialRelation target,
							  List<Variable> arguments) {
		return null;
	}

	@Override
	public Map<AnyPartialSymbol, AnyPartialSymbolInterpretation> createPartialInterpretations(Model model) {
		return null;
	}

	@Override
	public void initializeModel(Model model, int nodeCount) {

	}
}
