package tools.refinery.store.partial.translator.typehierarchy;

sealed interface TypeAnalysisResult permits EliminatedType, PreservedType {
}
