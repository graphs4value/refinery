package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.context.common.BaseInputKeyWrapper;
import tools.refinery.store.query.view.RelationView;

public class RelationViewWrapper extends BaseInputKeyWrapper<RelationView<?>> {
	public RelationViewWrapper(RelationView<?> wrappedKey) {
		super(wrappedKey);
	}

	@Override
	public String getPrettyPrintableName() {
		return wrappedKey.getName();
	}

	@Override
	public String getStringID() {
		return getPrettyPrintableName();
	}

	@Override
	public int getArity() {
		return wrappedKey.getArity();
	}

	@Override
	public boolean isEnumerable() {
		return true;
	}
}
