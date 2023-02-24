package tools.refinery.store.query.equality;

import tools.refinery.store.query.Dnf;
import tools.refinery.store.util.CycleDetectingMapper;

import java.util.List;

public class DeepDnfEqualityChecker implements DnfEqualityChecker {
	private final CycleDetectingMapper<Pair, Boolean> mapper = new CycleDetectingMapper<>(Pair::toString,
			this::doCheckEqual);

	@Override
	public boolean dnfEqual(Dnf left, Dnf right) {
		return mapper.map(new Pair(left, right));
	}

	protected boolean doCheckEqual(Pair pair) {
		return pair.left.equalsWithSubstitution(this, pair.right);
	}

	protected List<Pair> getInProgress() {
		return mapper.getInProgress();
	}

	protected record Pair(Dnf left, Dnf right) {
		@Override
		public String toString() {
			return "(%s, %s)".formatted(left.name(), right.name());
		}
	}
}
