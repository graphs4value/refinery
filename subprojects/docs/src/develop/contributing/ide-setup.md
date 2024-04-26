---
SPDX-FileCopyrightText: 2021-2023 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
sidebar_position: 2
title: IDE setup
---

# Setting up the development environment

## IntelliJ IDEA

We prefer [IntelliJ IDEA](https://www.jetbrains.com/idea/) as a Java development environment.
No special preparations should be necessary for importing the project as a Gradle project into IDEA:

1. See the [required tools](/develop/contributing#required-tools) for compiling Refinery about obtaining the required JDK version. You'll also need a version of IntelliJ IDEA that supports **Java 21** (version **2023.3** or later).

2. Clone the project git repository and open it in IntelliJ IDEA. Make sure to _open_ the project instead of creating a _new_ one in the same directory.

3. IntelliJ IDEA should build and index the project. If there are errors, it is likely that the `JAVA_HOME` was incorrectly set:
    * In _Project Structure > Project settings > Project > SDK_, a Java 21 compatible JDK should be selected.
    * In _Project Structure > Project settings > Project > Language level_, either _SDK default_ or _21_ should be selected.
    * Make sure that each module in _Project Structure > Project settings > Module_ uses the _Project default_ language level in _Sources > Language level_ and the _Project SDK_ in _Dependencies > Module SDK._
    * In _Settings > Gradle settings > Gralde Projects > Gradle_, the _Distribution_ should be set to _Wrapper_ and the _Gradle JVM_ should be set to _Project SDK._

4. We recommend installing the latest _SonarLint_ plugin in _Settings > Plugins_ to get real-time code quality analysis in your IDE.

:::note

You'll need [Eclipse](#eclipse) to edit Xtext (`*.xtext`) and MWE2 (`*.mwe2`) files and Ecore class diagrams (`*.aird`, `*.ecore`, `*.genmodel`).
If you do not plan on making changes to such files, feel free to skip the Eclipse installation steps below.

You'll also need [VS Code](#vs-code) to edit the TypeScript code in Refinery.

:::

## Eclipse

1. See the [required tools](/develop/contributing#required-tools) for compiling Refinery about obtaining the required JDK version.

2. Download and extract the [Eclipse IDE for Java and DSL Developers 2023-12](https://www.eclipse.org/downloads/packages/release/2023-12/r/eclipse-ide-java-and-dsl-developers) package.

3. Launch Eclipse and create a new workspace.

4. Open _Help > Eclipse Marketplace_ and install the following software:
    * _EclEmma Java Code Coverage_
    * _EcoreTools : Ecore Diagram Editor_
    * _Sirius_ (ignore the warning during installation about the solution _Sirius_ not being available)
    * _SonarLint_

5. Open _Window > Preferences_ and set the following preferences:
    * _General > Workspace > Text file encoding_ should be _UTF-8_.
    * _General > Workspace > New text file line delimiter_ should be _Unix_.
    * Add the JDK 21 to _Java > Installed JREs_.
    * Make sure JDK 21 is selected for _JavaSE-21_ at _Java > Installed JREs > Execution Environments_.
    * Set _Gradle > Java home_ to the `JAVA_HOME` directory (the directory which contains the `bin` directory) of JDK 21. Here, Buildship will show a yellow warning sign, which can be safely ignored.
    * Set _Java > Compiler > JDK Compliance > Compiler compliance level_ to _21_.

6. Clone the project Git repository but _do not_ import it into Eclipse yet.

7. Open a new terminal and run
    ```bash posix2windows
    ./gradlew prepareEclipse
    ```
    in the cloned repository.
    * This should complete without any compilation errors.
    * To troubleshoot any error, see the [instructions about compiling Refinery](/develop/contributing#compiling).

8. Select _File > Import... > Gradle > Existing Gradle Project_ and import the cloned repository in Eclipse.
    * Make sure to select the root of the repository (containing this file) as the _Project root directory_ and that the _Gradle distribution_ is _Gradle wrapper_.
    * If you have previously imported the project into Eclipse, this step will likely fail. In that case, you should remove the projects from Eclipse, run `git clean -fxd` in the repository, and start over from step 8.

## VS Code

We recommend [VSCodium](https://github.com/VSCodium/vscodium) or [Visual Studio Code](https://code.visualstudio.com/) to work with the parts of Refinery that are written is TypeScript.

1. See the [required tools](/develop/contributing#required-tools) for compiling Refinery about obtaining the required JDK version. You'll also need a version of IntelliJ IDEA that supports **Java 21** (version **2023.3** or later).

2. Install the following VS Code extensions:
    * _EditorConfig for VS Code_ [[Open VSX](https://open-vsx.org/extension/EditorConfig/EditorConfig)] [[Extension Marketplace](https://marketplace.visualstudio.com/items?itemName=EditorConfig.EditorConfig)]
    * _ZipFS - a zip file system_ [[Open VSX](https://open-vsx.org/extension/arcanis/vscode-zipfs)] [[Extension Marketplace](https://marketplace.visualstudio.com/items?itemName=arcanis.vscode-zipfs)]
    * _ESLint_ [[Open VSX](https://open-vsx.org/extension/dbaeumer/vscode-eslint)] [[Extension Marketplace](https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint)]
    * _XState VSCode_ [[Open VSX](https://open-vsx.org/extension/statelyai/stately-vscode)] [[Extension Marketplace](https://marketplace.visualstudio.com/items?itemName=statelyai.stately-vscode)]

3. Clone the project Git repository but _do not_ import it into VS Code yet.

4. Run
    ```bash posix2windows
    ./gradlew installFrontend
    ```
    to install all required Node.js tooling.

5. Open the repository with _Open Folder&hellip;_ in VS Code.
    * When asked, select that you _Trust_ the folder.
    * When asked, enable using the TypeScript and ESLint tooling specified in the repository.
