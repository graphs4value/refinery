package tools.refinery.store.partial.lifting;

import tools.refinery.store.query.Dnf;
import tools.refinery.store.partial.literal.Modality;

public record ModalDnf(Modality modality, Dnf dnf) {
	@Override
	public String toString() {
		return "%s %s".formatted(modality, dnf.name());
	}
}
