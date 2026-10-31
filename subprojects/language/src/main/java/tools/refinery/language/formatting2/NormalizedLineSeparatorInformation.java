/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.formatting2;

import org.eclipse.xtext.formatting.ILineSeparatorInformation;

/**
 * Make sure we use a single {@code \n} as a line separator on all platforms.
 * <p>
 *     On the frontend, CodeMirror normalizes line separators, so a discrepancy would lead to incorrect highlight and
 *     error marker offsets between the server and client.
 * </p>
 * <p>
 *     This also ensures the CLI uses normalized line endings on all platforms.
 * </p>
 */
public class NormalizedLineSeparatorInformation implements ILineSeparatorInformation {
	@Override
	public String getLineSeparator() {
		return "\n";
	}
}
