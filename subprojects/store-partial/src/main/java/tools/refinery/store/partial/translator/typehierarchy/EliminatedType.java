package tools.refinery.store.partial.translator.typehierarchy;

import tools.refinery.store.partial.representation.PartialRelation;

record EliminatedType(PartialRelation replacement) implements TypeAnalysisResult {
}
