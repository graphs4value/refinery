package tools.refinery.store.query;

import tools.refinery.store.query.literal.Literal;

import java.util.List;
import java.util.Set;

public record DnfClause(Set<Variable> quantifiedVariables, List<Literal> literals) {
}
