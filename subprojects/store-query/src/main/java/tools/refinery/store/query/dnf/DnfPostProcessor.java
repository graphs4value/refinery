/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.DnfEqualityChecker;
import tools.refinery.store.query.equality.SubstitutingLiteralEqualityHelper;
import tools.refinery.store.query.equality.SubstitutingLiteralHashCodeHelper;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.*;

class DnfPostProcessor {
	private final List<SymbolicParameter> parameters;
	private final List<List<Literal>> clauses;

	public DnfPostProcessor(List<SymbolicParameter> parameters, List<List<Literal>> clauses) {
		this.parameters = parameters;
		this.clauses = clauses;
	}

	public List<DnfClause> postProcessClauses() {
		var parameterInfoMap = getParameterInfoMap();
		var postProcessedClauses = new LinkedHashSet<CanonicalClause>(clauses.size());
		int index = 0;
		for (var literals : clauses) {
			var postProcessor = new ClausePostProcessor(parameterInfoMap, literals);
			ClausePostProcessor.Result result;
			try {
				result = postProcessor.postProcessClause();
			} catch (InvalidQueryException e) {
				throw new InvalidClauseException(index, e);
			}
			if (result instanceof ClausePostProcessor.ClauseResult clauseResult) {
				postProcessedClauses.add(new CanonicalClause(clauseResult.clause()));
			} else if (result instanceof ClausePostProcessor.ConstantResult constantResult) {
				switch (constantResult) {
				case ALWAYS_TRUE -> {
					var inputVariables = getInputVariables();
					return List.of(new DnfClause(inputVariables, List.of()));
				}
				case ALWAYS_FALSE -> {
					// Skip this clause because it can never match.
				}
				default -> throw new IllegalStateException("Unexpected ClausePostProcessor.ConstantResult: " +
						constantResult);
				}
			} else {
				throw new IllegalStateException("Unexpected ClausePostProcessor.Result: " + result);
			}
			index++;
		}
		return postProcessedClauses.stream().map(CanonicalClause::getDnfClause).toList();
	}

	private Map<Variable, ClausePostProcessor.ParameterInfo> getParameterInfoMap() {
		var mutableParameterInfoMap = new LinkedHashMap<Variable, ClausePostProcessor.ParameterInfo>();
		int arity = parameters.size();
		for (int i = 0; i < arity; i++) {
			var parameter = parameters.get(i);
			mutableParameterInfoMap.put(parameter.getVariable(),
					new ClausePostProcessor.ParameterInfo(parameter.getDirection(), i));
		}
		return Collections.unmodifiableMap(mutableParameterInfoMap);
	}

	private Set<Variable> getInputVariables() {
		var inputParameters = new LinkedHashSet<Variable>();
		for (var parameter : parameters) {
			if (parameter.getDirection() == ParameterDirection.IN) {
				inputParameters.add(parameter.getVariable());
			}
		}
		return Collections.unmodifiableSet(inputParameters);
	}

	private class CanonicalClause {
		private final DnfClause dnfClause;

		public CanonicalClause(DnfClause dnfClause) {
			this.dnfClause = dnfClause;
		}

		public DnfClause getDnfClause() {
			return dnfClause;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			var otherCanonicalClause = (CanonicalClause) obj;
			var helper = new SubstitutingLiteralEqualityHelper(DnfEqualityChecker.DEFAULT, parameters, parameters);
			return dnfClause.equalsWithSubstitution(helper, otherCanonicalClause.dnfClause);
		}

		@Override
		public int hashCode() {
			var helper = new SubstitutingLiteralHashCodeHelper(parameters);
			return dnfClause.hashCodeWithSubstitution(helper);
		}
	}
}
