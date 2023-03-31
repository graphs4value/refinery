package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;

public sealed interface Sort permits DataSort, NodeSort {
	boolean isInstance(Variable variable);

	Variable newInstance(@Nullable String name);

	Variable newInstance();
}
