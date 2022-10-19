package tools.refinery.store.query;

import tools.refinery.store.query.atom.DNFAtom;

import java.util.List;
import java.util.Set;

public record DNFAnd(Set<Variable> quantifiedVariables, List<DNFAtom> constraints) {
}
