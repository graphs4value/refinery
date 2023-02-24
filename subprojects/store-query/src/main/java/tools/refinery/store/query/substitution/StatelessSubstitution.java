package tools.refinery.store.query.substitution;

import tools.refinery.store.query.Variable;

public enum StatelessSubstitution implements Substitution {
	FAILING {
		@Override
		public Variable getSubstitute(Variable variable) {
			throw new IllegalArgumentException("No substitute for " + variable);
		}
	},
	IDENTITY {
		@Override
		public Variable getSubstitute(Variable variable) {
			return variable;
		}
	}
}
