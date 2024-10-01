/*******************************************************************************
 * Copyright (c) 2010-2024, BergmannG, IncQuery Labs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import tools.refinery.interpreter.matchers.psystem.queries.PDisjunction;

/**
 *
 * The identity element of PDisjunctionRewriter composition.
 *
 * @author BergmannG
 * @since 2.9
 *
 */
public class IdentityPDisjunctionRewriter extends PDisjunctionRewriter {

	@Override
	public PDisjunction rewrite(PDisjunction disjunction) {
		return disjunction;
	}

}
