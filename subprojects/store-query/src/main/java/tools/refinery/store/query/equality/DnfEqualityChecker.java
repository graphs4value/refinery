package tools.refinery.store.query.equality;

import tools.refinery.store.query.Dnf;

@FunctionalInterface
public interface DnfEqualityChecker {
	boolean dnfEqual(Dnf left, Dnf right);
}
