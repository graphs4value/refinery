package tools.refinery.language.utils;

import tools.refinery.language.model.problem.*;

public record BuiltinSymbols(Problem problem, ClassDeclaration node, ReferenceDeclaration equals,
							 PredicateDefinition exists, PredicateDefinition contained, PredicateDefinition contains,
							 PredicateDefinition root) {
}
