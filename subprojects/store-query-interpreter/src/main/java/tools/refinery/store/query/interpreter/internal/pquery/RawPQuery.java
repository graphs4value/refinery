/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import tools.refinery.interpreter.api.GenericQuerySpecification;
import tools.refinery.interpreter.api.InterpreterEngine;
import tools.refinery.interpreter.api.scope.QueryScope;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.annotations.PAnnotation;
import tools.refinery.interpreter.matchers.psystem.queries.BasePQuery;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PVisibility;
import tools.refinery.store.query.interpreter.internal.RelationalScope;
import tools.refinery.store.query.interpreter.internal.matcher.RawPatternMatcher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RawPQuery extends BasePQuery {
	private final String fullyQualifiedName;
	private List<PParameter> parameters;
	private final LinkedHashSet<PBody> bodies = new LinkedHashSet<>();

	public RawPQuery(String name, PVisibility visibility) {
		super(visibility);
		fullyQualifiedName = name;
	}

	public RawPQuery(String name) {
		this(name, PVisibility.PUBLIC);
	}

	@Override
	public String getFullyQualifiedName() {
		return fullyQualifiedName;
	}

	public void setParameters(List<PParameter> parameters) {
		this.parameters = parameters;
	}

	@Override
	public void addAnnotation(PAnnotation annotation) {
		super.addAnnotation(annotation);
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
			protected RawPatternMatcher instantiate(InterpreterEngine engine) {
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
