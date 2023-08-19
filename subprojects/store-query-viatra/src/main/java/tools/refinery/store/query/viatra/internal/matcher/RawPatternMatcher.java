/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.matcher;

import tools.refinery.viatra.runtime.api.GenericPatternMatcher;
import tools.refinery.viatra.runtime.api.GenericQuerySpecification;
import tools.refinery.viatra.runtime.matchers.backend.IQueryResultProvider;

public class RawPatternMatcher extends GenericPatternMatcher {
    public RawPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
        super(specification);
    }

	IQueryResultProvider getBackend() {
		return backend;
	}
}
