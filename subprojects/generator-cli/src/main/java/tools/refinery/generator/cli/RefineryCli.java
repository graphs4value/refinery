/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli;

import com.beust.jcommander.JCommander;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.generator.cli.commands.CheckCommand;
import tools.refinery.generator.cli.commands.Command;
import tools.refinery.generator.cli.commands.ConcretizeCommand;
import tools.refinery.generator.cli.commands.GenerateCommand;
import tools.refinery.generator.standalone.StandaloneRefinery;

import java.io.IOException;

public class RefineryCli {
	public static final int EXIT_SUCCESS = 0;
	public static final int EXIT_FAILURE = 1;
	public static final int EXIT_USAGE = 2;

	private static final Logger LOGGER = LoggerFactory.getLogger(RefineryCli.class);

	@Inject
	private CheckCommand checkCommand;

	@Inject
	private ConcretizeCommand concretizeCommand;

	@Inject
	private GenerateCommand generateCommand;

	private JCommander jCommander;

	public int run(String[] args) {
		Command command = null;
		try {
			command = parseArguments(args);
		} catch (RuntimeException e) {
			LOGGER.error("Error while parsing arguments", e);
		}
		if (command == null) {
			showUsage();
			return EXIT_USAGE;
		}
		try {
			return command.run();
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Error while executing command", e);
			return EXIT_FAILURE;
		}
	}

	private JCommander getjCommander() {
		if (jCommander == null) {
			jCommander = JCommander.newBuilder()
					.programName("refinery")
					.addObject(this)
					.addCommand("generate", generateCommand, "g")
					.addCommand("check", checkCommand, "c")
					.addCommand("concretize", concretizeCommand)
					.build();
		}
		return jCommander;
	}

	private Command parseArguments(String... args) {
		var jc = getjCommander();
		jc.parse(args);
		var parsedCommand = jc.getParsedCommand();
		if (parsedCommand == null) {
			return null;
		}
		var commandParser = jc.getCommands().get(parsedCommand);
		if (commandParser == null) {
			throw new IllegalStateException("Command parsed but not registered: " + parsedCommand);
		}
		return (Command) commandParser.getObjects().stream()
				.filter(Command.class::isInstance)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Not an executable command: " + parsedCommand));
	}

	private void showUsage() {
		getjCommander().usage();
	}

	public static void main(String[] args) {
		int exitValue = EXIT_FAILURE;
		RefineryCli cli = null;
		try {
			cli = StandaloneRefinery.getInjector().getInstance(RefineryCli.class);
		} catch (RuntimeException e) {
			LOGGER.error("Initialization error", e);
		}
		if (cli != null) {
			exitValue = cli.run(args);
		}
		System.exit(exitValue);
	}
}
