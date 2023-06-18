/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.reasoning.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;

import java.util.List;

public interface TranslatedRelation {
	PartialRelation getSource();

	void configure(List<Advice> advices);

	List<Literal> call(CallPolarity polarity, Modality modality, List<Variable> arguments);

	PartialInterpretation<TruthValue, Boolean> createPartialInterpretation(Model model);
}
