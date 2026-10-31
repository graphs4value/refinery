/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.serializer;

import com.google.inject.Inject;
import org.eclipse.xtext.serializer.ISerializationContext;
import org.eclipse.xtext.serializer.sequencer.ITransientValueService.ValueTransient;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.services.ProblemGrammarAccess;

import static tools.refinery.language.model.problem.ProblemPackage.Literals.ABSTRACT_ASSERTION__RELATION;

public class PreferShortAssertionsProblemSemanticSequencer extends ProblemSemanticSequencer {
	@Inject
	private ProblemGrammarAccess grammarAccess;

	@Override
	protected void sequence_Problem(ISerializationContext context, Problem semanticObject) {
		var feeder = createSequencerFeeder(context, semanticObject);
		var access = grammarAccess.getProblemAccess();
		var statements = semanticObject.getStatements();
		// Completely empty feeders lead to an exception, because they can't transition from the {@code start} state
		// to the {@code stop} state directly. For a completely empty model, we always emit the {@code problem}
		// declaration. Otherwise, we only emit it if needed.
		boolean moduleKindNeeded = transientValues.isValueTransient(
				semanticObject, ProblemPackage.Literals.PROBLEM__KIND) == ValueTransient.NO;
		if (statements.isEmpty() || moduleKindNeeded) {
			feeder.accept(access.getKindModuleKindEnumRuleCall_0_0_0(), semanticObject.getKind());
			var name = semanticObject.getName();
			if (name != null) {
				feeder.accept(access.getNameQualifiedNameParserRuleCall_0_1_0(), name);
			}
		}
		int index = 0;
		for (var statement : statements) {
			feeder.accept(access.getStatementsStatementParserRuleCall_1_0(), statement, index);
			index++;
		}
		feeder.finish();
	}

	@Override
	protected void sequence_Assertion(ISerializationContext context, Assertion semanticObject) {
		if (!(semanticObject.getValue() instanceof LogicConstant logicConstant) ||
				logicConstant.getLogicValue() == LogicValue.ERROR) {
			super.sequence_Assertion(context, semanticObject);
			return;
		}
		if (errorAcceptor != null) {
			if (transientValues.isValueTransient(semanticObject, ABSTRACT_ASSERTION__RELATION) == ValueTransient.YES) {
				errorAcceptor.accept(diagnosticProvider.createFeatureValueMissing(semanticObject,
						ABSTRACT_ASSERTION__RELATION));
			}
		}
		var feeder = createSequencerFeeder(context, semanticObject);
		var access = grammarAccess.getAssertionAccess();
		if (semanticObject.isDefault()) {
			feeder.accept(access.getDefaultDefaultKeyword_0_0());
		}
		feeder.accept(access.getValueShortLogicConstantParserRuleCall_1_1_0_0(), logicConstant);
		feeder.accept(access.getRelationRelationQualifiedNameParserRuleCall_1_1_1_0_1(), semanticObject.getRelation());
		var iterator = semanticObject.getArguments().iterator();
		if (iterator.hasNext()) {
			var firstArgument = iterator.next();
			feeder.accept(access.getArgumentsAssertionArgumentParserRuleCall_1_1_3_0_0(), firstArgument, 0);
			int index = 1;
			while (iterator.hasNext()) {
				var argument = iterator.next();
				feeder.accept(access.getArgumentsAssertionArgumentParserRuleCall_1_1_3_1_1_0(), argument, index);
				index++;
			}
		}
		feeder.finish();
	}
}
