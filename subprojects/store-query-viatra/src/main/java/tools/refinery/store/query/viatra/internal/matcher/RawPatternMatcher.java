/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.matcher;

import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryResultProvider;

public class RawPatternMatcher extends GenericPatternMatcher {
    public RawPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
        super(specification);
    }

	IQueryResultProvider getBackend() {
		return backend;
	}
}
