/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.headless;

import java.util.Locale;

enum GraphicalOutputFormat {
	SVG,
	PDF,
	PNG;

	@Override
	public String toString() {
		return super.toString().toLowerCase(Locale.ROOT);
	}
}
