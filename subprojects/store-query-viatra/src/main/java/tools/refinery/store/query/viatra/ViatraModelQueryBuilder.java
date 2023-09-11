/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.rewriter.DnfRewriter;
import tools.refinery.viatra.runtime.api.ViatraQueryEngineOptions;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackendFactory;
import tools.refinery.viatra.runtime.matchers.backend.QueryEvaluationHint;

import java.util.Collection;
import java.util.function.Function;

@SuppressWarnings("UnusedReturnValue")
public interface ViatraModelQueryBuilder extends ModelQueryBuilder {
	ViatraModelQueryBuilder engineOptions(ViatraQueryEngineOptions engineOptions);

	ViatraModelQueryBuilder defaultHint(QueryEvaluationHint queryEvaluationHint);

	ViatraModelQueryBuilder backend(IQueryBackendFactory queryBackendFactory);

	ViatraModelQueryBuilder cachingBackend(IQueryBackendFactory queryBackendFactory);

	ViatraModelQueryBuilder searchBackend(IQueryBackendFactory queryBackendFactory);

	@Override
	default ViatraModelQueryBuilder queries(AnyQuery... queries) {
		ModelQueryBuilder.super.queries(queries);
		return this;
	}

	@Override
	default ViatraModelQueryBuilder queries(Collection<? extends AnyQuery> queries) {
		ModelQueryBuilder.super.queries(queries);
		return this;
	}

	@Override
	ViatraModelQueryBuilder query(AnyQuery query);

	@Override
	ViatraModelQueryBuilder rewriter(DnfRewriter rewriter);

	ViatraModelQueryBuilder computeHint(Function<Dnf, QueryEvaluationHint> computeHint);

	@Override
	ViatraModelQueryStoreAdapter build(ModelStore store);
}
