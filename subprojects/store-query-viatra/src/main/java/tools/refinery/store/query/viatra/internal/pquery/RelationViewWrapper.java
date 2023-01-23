package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.context.common.BaseInputKeyWrapper;
import tools.refinery.store.query.view.AnyRelationView;

public class RelationViewWrapper extends BaseInputKeyWrapper<AnyRelationView> {
	public RelationViewWrapper(AnyRelationView wrappedKey) {
		super(wrappedKey);
	}

	@Override
	public String getPrettyPrintableName() {
		return wrappedKey.name();
	}

	@Override
	public String getStringID() {
		return getPrettyPrintableName();
	}

	@Override
	public int getArity() {
		return wrappedKey.arity();
	}

	@Override
	public boolean isEnumerable() {
		return true;
	}
}
