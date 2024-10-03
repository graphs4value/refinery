---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
sidebar_position: 1
sidebar_label: CLI
---

# Command-line interface

You can run Refinery as a command-line applications via our [Docker container](https://github.com/graphs4value/refinery/pkgs/container/refinery-cli) on either `amd64` or `arm64` machines:

```shell
docker run --rm -it -v ${PWD}:/data ghcr.io/graphs4value/refinery-cli:0.1.1
```

This will let you read input files and generate models in the current directory (`${PWD}`) of your terminal session.
Module imports (e.g., `import some::module.` to import `some/module.refinery`) relative to the current directory are also supported.

For example, to generate a model based on the file named `input.problem` in the current directory and write the results into the file named `output.refinery`, you may run the [`generate` subcommand](#generate) with

```shell
docker run --rm -it -v ${PWD}:/data ghcr.io/graphs4value/refinery-cli:0.1.1 generate -o output.refinery input.problem
```

If you want Refinery CLI to print its documentation, run

```shell
docker run --rm -it -v ${PWD}:/data ghcr.io/graphs4value/refinery-cli:0.1.1 -help
```

## The `generate` subcommand {#generate}

The `generate` subcommand generates a consistent concrete model from a partial model.

```shell
docker run --rm -it -v ${PWD}:/data ghcr.io/graphs4value/refinery-cli:0.1.1 generate [options] input path
```

The `input path` should be a path to a `.problem` file relative to the current directory.
Due to Docker containerization, paths _outside_ the current directory (e.g., `../input.problem`) are not supported.

Passing `-` as the `input path` will read a partial model from the standard input.

By default, the generator is _deterministic_ and always outputs the same concrete model. See the [`-random-seed`](#generate-random-seed) option to customize this behavior.

See below for the list of supported `[options]`.

### `-output`, `-o` {#generate-output}

The output path for the generated model.
Passing `-o -` will write the generated model to the standard output.

When generating multiple models with [`-solution-number`](#generate-solution-number), the value `-` is not supported and individual solutions will be saved to numbered files.
For example, if you pass `-o output.refinery -n 10`, solutions will be saved as `output_001.refinery`, `output_002.refinery`, ..., `output_010.refinery`.

**Default value:** `-`, i.e., the solution is written to the standard output.

### `-random-seed`, `-r` {#generate-random-seed}

Random seed to control the behavior of model generation.

The same random seed value and Refinery release will produce the same output model for an input problem.
Models generated with different values of `-random-seed` are highly likely (but are not guaranteed) to be _substantially_ different.

**Default value:** `1`

### `-scope`, `-s` {#generate-scope}

Add [scope constraints](../../language/logic#type-scopes) to the input problem.

This option is especially useful if you want to generate models of multiple sizes from the same partial model.

For example, the command

```shell
docker run --rm -it -v ${PWD}:/data ghcr.io/graphs4value/refinery-cli:0.1.1 generate -s File=20..25 input.problem
```

is equivalent to appending

```refinery title="input.problem"
scope File = 20..25.
```

to `input.problem`.
The syntax of the argument is equivalent to the [`scope`](../../language/logic#type-scopes) declaration, but you be careful with the handling of spaces in your shell.
Any number of `-s` arguments are supported. For example, the following argument lists are equivalent:

```shell
-scope File=20..25,Directory=3
-s File=20..25,Directory=3
-s File=20..25 -s Directory=3
-s "File = 20..25, Directory = 3"
-s "File = 20..25" -s "Directory = 3"
```

The `*` opeator also has to be quoted to avoid shell expansion:

```shell
-s "File=20..*"
```

### `-scope-override`, `-S` {#generate-scope-override}

Override [scope constraints](../../language/logic#type-scopes) to the input problem.

This argument is similar to [`-scope`](#generate-scope), but has higher precedence than the [`scope`](../../language/logic#type-scopes) declarations already present in the input file.
However, you can't override `scope` declarations in modules imported in the input file using the `import` statement.

For example, if we have

```refinery title="input.problem"
scope File = 20..25, Directory = 3.
```

in the input file, the arguments `-s File=10..12 input.problem` will be interpreted as

```refinery
scope File = 20..25, Directory = 3.
scope File = 10..12.
```

which results in an _unsatisfiable_ problem. If the use `-S File=10..12 input.problem` instead, the type scope for `File` is overridden as

```refinery
scope Directory = 3.
scope File = 10..12.
```

and model generation can proceed as requested. Since we had specified no override for `Directory`, its type scope declared in `input.problem` was preserved.

Scope overrides do not override additional scopes, i.e., `-s File=20..30 -S File=10..25` is interpreted as `-S File=20..25`.

### `-solution-number`, `-n` {#generate-solution-number}

The number of distinct solutions to generate.

Generated solutions are always different, but are frequently not _substantially_ different, i.e., the differences between generated solutions comprise only a few model elements.
You'll likely generate substantially different models by calling the generator multiple times with different [`-random-seed`](#generate-random-seed) values instead.

The generator will create [numbered output files](#generate-output) for each solution found.
The generation is considered successful if it finds at least one solution, but may find less than the requested number of solutions if no more exist.
In this case, there will be fewer output files than requested.

**Default value:** `1`
