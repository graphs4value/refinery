/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.ide.contentassist;

import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.IdeContentProposalProvider;
import tools.refinery.language.utils.ProblemUtil;

import java.util.Set;

public class ProblemContentProposalProvider extends IdeContentProposalProvider {
	private static final Set<String> SHADOW_KEYWORDS = Set.of("candidate", "partial", "may", "must");

	@Override
	protected boolean filterKeyword(Keyword keyword, ContentAssistContext context) {
		if (SHADOW_KEYWORDS.contains(keyword.getValue())) {
			return ProblemUtil.mayReferToShadow(context.getCurrentModel());
		}
		return super.filterKeyword(keyword, context);
	}
}
