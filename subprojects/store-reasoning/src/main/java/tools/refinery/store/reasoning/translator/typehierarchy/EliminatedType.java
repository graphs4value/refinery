package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;

record EliminatedType(PartialRelation replacement) implements TypeAnalysisResult {
}
