/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.api.GenericPatternMatcher;
import tools.refinery.interpreter.api.GenericQuerySpecification;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;

public class RawPatternMatcher extends GenericPatternMatcher {
    public RawPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
        super(specification);
    }

	IQueryResultProvider getBackend() {
		return backend;
	}
}
