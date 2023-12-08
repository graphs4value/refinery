<!--
  SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>

  SPDX-License-Identifier: EPL-2.0
-->

# Contributing to Refinery

## Setting up the development environment

### With IntelliJ IDEA

We prefer IntelliJ IDEA as a development environment.
No special preparations should be necessary for importing the project as a Gradle project into IDEA.

However, you will need Eclipse to edit Xtext (`*.xtext`) and MWE2 (`*.mwe2`) files and Ecore class diagrams (`*.aird`, `*.ecore`, `*.genmodel`).
If you do not plan on making changes to such files, feel free to skip the Eclipse installation steps below.

### With Eclipse IDE

1. Download and install a _Java 21_ compatible JDK. For Windows, prefer OpenJDK builds from [Adoptium](https://adoptium.net/).

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

6. Clone the project Git repository but do not import it into Eclipse yet.

7. Open a new terminal an run `./gradlew prepareEclipse` (`.\gradlew prepareEclipse` on Windows) in the cloned repository.
    * This should complete without any compilation errors.
    * If you get any errors about the JVM version, check whether the `JAVA_HOME` environment variable is set to the location of JDK. You can query the variable with `echo $JAVA_HOME` on Linux and `echo $Env:JAVA_HOME` in PowerShell on Windows. To set it, use `export JAVA_HOME=/java/path/here` or `$Env:JAVA_HOME="C:\java\path\here"`, respectively.
    * If the build fails with a `Host name must not be empty` error, you [might need to remove the empty proxy configuration from your global `gradle.properties` file](https://stackoverflow.com/a/62128323).

8. Select _File > Import... > Gradle > Existing Gradle Project_ and import the cloned repository in Eclipse.
    * Make sure to select the root of the repository (containing this file) as the _Project root directory_ and that the _Gradle distribution_ is _Gradle wrapper_.
    * If you have previously imported the project into Eclipse, this step will likely fail. In that case, you should remove the projects from Eclipse, run `git clean -fxd` in the repository, and start over from step 8.

