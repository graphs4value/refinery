/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.lifting;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.reasoning.literal.Modality;

record ModalDnf(Modality modality, Dnf dnf) {
	@Override
	public String toString() {
		return "%s %s".formatted(modality, dnf.name());
	}
}
