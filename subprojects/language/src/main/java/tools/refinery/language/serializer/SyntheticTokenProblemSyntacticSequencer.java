/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.serializer;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.INode;

public class SyntheticTokenProblemSyntacticSequencer extends ProblemSyntacticSequencer {
	@Override
	protected String getTRANSITIVE_CLOSUREToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		return "+";
	}
}
