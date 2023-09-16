/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.rewriter.DnfRewriter;
import tools.refinery.interpreter.api.InterpreterEngineOptions;
import tools.refinery.interpreter.matchers.backend.IQueryBackendFactory;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;

import java.util.Collection;
import java.util.function.Function;

@SuppressWarnings("UnusedReturnValue")
public interface QueryInterpreterBuilder extends ModelQueryBuilder {
	QueryInterpreterBuilder engineOptions(InterpreterEngineOptions engineOptions);

	QueryInterpreterBuilder defaultHint(QueryEvaluationHint queryEvaluationHint);

	QueryInterpreterBuilder backend(IQueryBackendFactory queryBackendFactory);

	QueryInterpreterBuilder cachingBackend(IQueryBackendFactory queryBackendFactory);

	QueryInterpreterBuilder searchBackend(IQueryBackendFactory queryBackendFactory);

	@Override
	default QueryInterpreterBuilder queries(AnyQuery... queries) {
		ModelQueryBuilder.super.queries(queries);
		return this;
	}

	@Override
	default QueryInterpreterBuilder queries(Collection<? extends AnyQuery> queries) {
		ModelQueryBuilder.super.queries(queries);
		return this;
	}

	@Override
	QueryInterpreterBuilder query(AnyQuery query);

	@Override
	QueryInterpreterBuilder rewriter(DnfRewriter rewriter);

	QueryInterpreterBuilder computeHint(Function<Dnf, QueryEvaluationHint> computeHint);

	@Override
	QueryInterpreterStoreAdapter build(ModelStore store);
}
