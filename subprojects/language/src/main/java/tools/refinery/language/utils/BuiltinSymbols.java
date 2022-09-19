package tools.refinery.language.utils;

import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.EnumDeclaration;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ReferenceDeclaration;

public record BuiltinSymbols(Problem problem, ClassDeclaration node, ReferenceDeclaration equals,
		PredicateDefinition exists, ClassDeclaration domain, ClassDeclaration data, EnumDeclaration bool, Node boolTrue,
		Node boolFalse, ClassDeclaration intClass, ClassDeclaration real, ClassDeclaration string,
		PredicateDefinition contained, PredicateDefinition contains, PredicateDefinition root) {
}
