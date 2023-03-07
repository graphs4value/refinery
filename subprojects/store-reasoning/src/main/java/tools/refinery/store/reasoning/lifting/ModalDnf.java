package tools.refinery.store.reasoning.lifting;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.reasoning.literal.Modality;

record ModalDnf(Modality modality, Dnf dnf) {
	@Override
	public String toString() {
		return "%s %s".formatted(modality, dnf.name());
	}
}
