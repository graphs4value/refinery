/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import tools.refinery.generator.cli.output.OutputOptions;

public interface OutputFormatCommand extends Command {
	OutputOptions getOutputOptions();
}
