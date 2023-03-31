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
