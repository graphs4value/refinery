/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.query.viatra.internal.pquery.rewriter;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryMetaContext;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PDisjunction;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.PBodyCopier;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.PBodyNormalizer;

import java.util.LinkedHashSet;
import java.util.Set;

public class RefineryPBodyNormalizer extends PBodyNormalizer {
	public RefineryPBodyNormalizer(IQueryMetaContext context) {
		super(context);
	}

	@Override
	public PDisjunction rewrite(PDisjunction disjunction) {
		Set<PBody> normalizedBodies = new LinkedHashSet<>();
		for (PBody body : disjunction.getBodies()) {
			PBodyCopier copier = new RefineryPBodyCopier(body, getTraceCollector());
			PBody modifiedBody = copier.getCopiedBody();
			normalizeBody(modifiedBody);
			normalizedBodies.add(modifiedBody);
			modifiedBody.setStatus(PQuery.PQueryStatus.OK);
		}
		return new PDisjunction(normalizedBodies);
	}
}
