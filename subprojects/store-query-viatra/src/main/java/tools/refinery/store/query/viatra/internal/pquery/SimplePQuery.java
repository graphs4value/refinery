package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.BasePQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PVisibility;
import tools.refinery.store.query.viatra.internal.RawPatternMatcher;
import tools.refinery.store.query.viatra.internal.RelationalScope;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SimplePQuery extends BasePQuery {
	private final String fullyQualifiedName;
	private List<PParameter> parameters;
	private final LinkedHashSet<PBody> bodies = new LinkedHashSet<>();

	public SimplePQuery(String name) {
		super(PVisibility.PUBLIC);
		fullyQualifiedName = name;
		setEvaluationHints(new QueryEvaluationHint(null, QueryEvaluationHint.BackendRequirement.UNSPECIFIED));
	}

	@Override
	public String getFullyQualifiedName() {
		return fullyQualifiedName;
	}

	public void setParameters(List<PParameter> parameters) {
		this.parameters = parameters;
	}

	@Override
	public List<PParameter> getParameters() {
		return parameters;
	}

	public void addBody(PBody body) {
		bodies.add(body);
	}

	@Override
	protected Set<PBody> doGetContainedBodies() {
		return bodies;
	}

	public GenericQuerySpecification<RawPatternMatcher> build() {
		return new GenericQuerySpecification<>(this) {
			@Override
			public Class<? extends QueryScope> getPreferredScopeClass() {
				return RelationalScope.class;
			}

			@Override
			protected RawPatternMatcher instantiate(ViatraQueryEngine engine) {
				RawPatternMatcher matcher = engine.getExistingMatcher(this);
				if (matcher == null) {
					matcher = engine.getMatcher(this);
				}
				return matcher;
			}

			@Override
			public RawPatternMatcher instantiate() {
				return new RawPatternMatcher(this);
			}
		};
	}
}
