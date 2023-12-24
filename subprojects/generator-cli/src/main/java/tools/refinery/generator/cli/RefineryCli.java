/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.Inject;
import tools.refinery.generator.cli.commands.GenerateCommand;
import tools.refinery.generator.standalone.StandaloneRefinery;

import java.io.IOException;

public class RefineryCli {
	private static final String GENERATE_COMMAND = "generate";

	@Inject
	private GenerateCommand generateCommand;

	private JCommander jCommander;

	public String parseArguments(String... args) {
		var jc = getJCommander();
		jc.parse(args);
		return jc.getParsedCommand();
	}

	public void run(String command) throws IOException {
		switch (command) {
		case GENERATE_COMMAND -> generateCommand.run();
		case null, default -> showUsageAndExit();
		}
	}

	public void showUsageAndExit() {
		getJCommander().usage();
		System.exit(1);
	}

	private JCommander getJCommander() {
		if (jCommander == null) {
			jCommander = JCommander.newBuilder()
					.programName("refinery")
					.addObject(this)
					.addCommand(GENERATE_COMMAND, generateCommand)
					.build();
		}
		return jCommander;
	}

	public static void main(String[] args) throws IOException {
		var cli = StandaloneRefinery.getInjector().getInstance(RefineryCli.class);
		String command = null;
		try {
			command = cli.parseArguments(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			cli.showUsageAndExit();
		}
		cli.run(command);
	}
}
