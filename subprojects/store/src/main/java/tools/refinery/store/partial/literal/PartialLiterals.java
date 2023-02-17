package tools.refinery.store.partial.literal;

import tools.refinery.store.query.literal.DnfCallLiteral;

public final class PartialLiterals {
	private PartialLiterals() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public ModalRelationLiteral may(PartialRelationLiteral literal) {
		return new ModalRelationLiteral(Modality.MAY, literal);
	}

	public ModalRelationLiteral must(PartialRelationLiteral literal) {
		return new ModalRelationLiteral(Modality.MUST, literal);
	}

	public ModalRelationLiteral current(PartialRelationLiteral literal) {
		return new ModalRelationLiteral(Modality.CURRENT, literal);
	}

	public ModalDnfCallLiteral may(DnfCallLiteral literal) {
		return new ModalDnfCallLiteral(Modality.MAY, literal);
	}

	public ModalDnfCallLiteral must(DnfCallLiteral literal) {
		return new ModalDnfCallLiteral(Modality.MUST, literal);
	}

	public ModalDnfCallLiteral current(DnfCallLiteral literal) {
		return new ModalDnfCallLiteral(Modality.CURRENT, literal);
	}
}
