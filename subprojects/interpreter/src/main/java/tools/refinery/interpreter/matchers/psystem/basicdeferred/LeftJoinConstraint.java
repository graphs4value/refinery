/*******************************************************************************
 * Copyright (c) 2010-2016, Tamas Szabo, Istvan Rath and Daniel Varro
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.basicdeferred;

import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.ITypeInfoProviderConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.TypeJudgement;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;

import java.util.Collections;
import java.util.Set;

public class LeftJoinConstraint extends PatternCallBasedDeferred implements ITypeInfoProviderConstraint {
	protected PVariable resultVariable;
	protected int optionalColumn;
	protected Object defaultValue;

	public LeftJoinConstraint(PBody pBody, Tuple actualParametersTuple, PQuery query, PVariable resultVariable,
							  int optionalColumn, Object defaultValue) {
		super(pBody, actualParametersTuple, query, Collections.singleton(resultVariable));
		this.resultVariable = resultVariable;
		this.optionalColumn = optionalColumn;
		this.defaultValue = defaultValue;
	}

	public PVariable getResultVariable() {
		return resultVariable;
	}

	public int getOptionalColumn() {
		return optionalColumn;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public Set<PVariable> getDeducedVariables() {
		return Collections.singleton(resultVariable);
	}

	@Override
	protected void doDoReplaceVariables(PVariable obsolete, PVariable replacement) {
		if (resultVariable.equals(obsolete)) {
			resultVariable = replacement;
		}
	}

	@Override
	protected Set<PVariable> getCandidateQuantifiedVariables() {
		return actualParametersTuple.getDistinctElements();
	}

	@Override
	protected String toStringRest() {
		return query.getFullyQualifiedName() + "@" + actualParametersTuple.toString() + "->"
				+ resultVariable.toString();
	}

	@Override
	public Set<TypeJudgement> getImpliedJudgements(IQueryMetaContext context) {
		var optionalParameter = getReferredQuery().getParameters().get(optionalColumn);
		var unaryType = optionalParameter.getDeclaredUnaryType();
		if (unaryType != null && !context.isEnumerable(unaryType)) {
			// The outer join makes the result variable non-enumerable, since the default value might not be present in
			// the model.
			return Set.of(new TypeJudgement(unaryType, Tuples.staticArityFlatTupleOf(resultVariable)));
		}
		return Set.of();
	}
}
