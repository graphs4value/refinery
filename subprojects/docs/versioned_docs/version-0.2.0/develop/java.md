---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
sidebar_position: 0
---

# Programming guide

This guide is aimed at developers who wish to create Java applications that leverage Refinery as a library.
We also recommend browsing the [Javadoc documentation](../javadoc) associated with Refinery components.
See the [contributor's guide](../contributing) for information on building and modifying Refinery itself.

:::note

Refinery can run as a cloud-based [_Graph Solver as a Service_](https://refinery.services/) without local installation.
You can also run a compiled version as a [Docker container](../../learn/docker).

:::

Below, you can find instructions on using [Gradle](#gradle) or [Apache Maven](#maven) to create applications that use Refinery as a Java library.

## Working with Gradle {#gradle}

We recommend [Gradle](https://gradle.org/) as a build system for creating Java programs that use Refinery as a library.
We created a [Gradle plugin](pathname://../javadoc/refinery-gradle-plugins/) to simplify project configuration.

To find out the configuration for using our artifacts, select whether you use a Kotlin-based (`.gradle.kts`) or a Groovy-based (`.gradle`) configuration format for your Gradle build. You should add this code to your Gradle *settings* file, which is named `settings.gradle.kts` or `settings.gradle`.

import TabItem from '@theme/TabItem';
import Tabs from '@theme/Tabs';

<Tabs groupId="gradleLanguage">
  <TabItem value="kotlin" label="Kotlin">
    ```kotlin title="settings.gradle.kts"
    plugins {
        id("tools.refinery.settings") version "0.2.0"
    }
    ```
  </TabItem>
  <TabItem value="groovy" label="Groovy">
    ```groovy title="settings.gradle"
    plugins {
        id 'tools.refinery.settings' version '0.2.0'
    }
    ```
  </TabItem>
</Tabs>

This plugin will perform the following actions automatically:
* Add a [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sec:sharing-catalogs) to your build to enable easy access to Refinery artifacts and their [dependencies](#declaring-dependencies).
* Lock refinery artifacts and their dependencies to a [platform](https://docs.gradle.org/current/userguide/platforms.html#sub:using-platform-to-control-transitive-deps) (Maven BOM) of tested versions.
* Configure a logger based on [Log4J over SLF4J](https://www.slf4j.org/legacy.html) and [SLF4J Simple](https://www.slf4j.org/apidocs/org/slf4j/simple/SimpleLogger.html) that is free from vulnerabilities and works out of the box for most use-cases.
* Generate [application](#building-applications) artifacts, if any, according to best practices used in the Refinery project.
* Add common dependencies for writing [unit tests](#writing-tests) for your Java code.

See the [multi-module projects](#multi-module-projects) section of this tutorial on how to disable some of these automated actions for a part of your build.

### Declaring dependencies

The Refinery Gradle plugins adds a [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sec:sharing-catalogs) named `refinery` that you can use to quickly access dependencies.
For example, to add a dependency to the [`tools.refinery:refinery-generator`](pathname://../javadoc/refinery-generator/) library, you add the following to your `build.gradle.kts` or `build.gradle` file:

<Tabs groupId="gradleLanguage">
  <TabItem value="kotlin" label="Kotlin">
    ```kotlin title="build.gradle.kts"
    dependencies {
        implementation(refinery.generator)
    }
    ```
  </TabItem>
  <TabItem value="groovy" label="Groovy">
    ```groovy title="build.gradle"
    dependencies {
        implementation refinery.generator
    }
    ```
  </TabItem>
</Tabs>

The version catalog also contains the external dependencies used by the Refinery framework.
For example, you may add [GSON](https://google.github.io/gson/) for JSON parsing and [JCommander](https://jcommander.org/) for command-line argument parsing as follows:

<Tabs groupId="gradleLanguage">
  <TabItem value="kotlin" label="Kotlin">
    ```kotlin title="build.gradle.kts"
    dependencies {
        implementation(refinery.gson)
        implementation(refinery.jcommander)
    }
    ```
  </TabItem>
  <TabItem value="groovy" label="Groovy">
    ```groovy title="build.gradle"
    dependencies {
        implementation refinery.gson
        implementation refinery.jcommander
    }
    ```
  </TabItem>
</Tabs>

### Building applications

You can use the built-in [`application`](https://docs.gradle.org/current/userguide/application_plugin.html) to build stand-alone Java applications.

When developing you main application code in the `src/main/java` directory of you project, you can use the [`StandaloneRefinery`](pathname://../javadoc/refinery-generator/tools/refinery/generator/standalone/StandaloneRefinery.html) class from [`tools.refinery:refinery-generator`](pathname://../javadoc/refinery-generator/) to access Refinery generator components. See the tutorial on Xtext's [dependency injection](https://eclipse.dev/Xtext/documentation/302_configuration.html#dependency-injection) for more advanced use-cases.

```java
package org.example;

import tools.refinery.generator.standalone.StandaloneRefinery;

import java.io.IOException;

public class ExampleMain {
    public static void main(String[] args) throws IOException {
        var problem = StandaloneRefinery.getProblemLoader().loadString("""
            class Filesystem {
                contains Directory[1] root
            }

            class File.

            class Directory extends File {
                contains Directory[] children
            }

            scope Filesystem = 1, File = 20.
            """);
        try (var generator = StandaloneRefinery.getGeneratorFactory().createGenerator(problem)) {
            generator.generate();
            var trace = generator.getProblemTrace();
            var childrenRelation = trace.getPartialRelation("Directory::children");
            var childrenInterpretation = generator.getPartialInterpretation(childrenRelation);
            var cursor = childrenInterpretation.getAll();
            while (cursor.move()) {
                System.out.printf("%s: %s%n", cursor.getKey(), cursor.getValue());
            }
        }
    }
}
```

If you want to produce a "fat JAR" that embeds all dependencies (e.g., for invoking from the command line or from Python with a single command), you should also add the [shadow](https://github.com/Goooler/shadow) plugin.
The recommended version of the shadow plugin is set in our [version catalog](#declaring-dependencies). You can add it to your build script as follows:

<Tabs groupId="gradleLanguage">
  <TabItem value="kotlin" label="Kotlin">
    ```kotlin title="build.gradle.kts"
    plugins {
        application
        alias(refinery.plugins.shadow)
    }

    application {
        mainClass = "org.example.ExampleMain"
    }
    ```
  </TabItem>
  <TabItem value="groovy" label="Groovy">
    ```groovy title="build.gradle"
    plugins {
        application
        alias refinery.plugins.shadow
    }

    application {
        mainClass 'org.example.ExampleMain'
    }
    ```
  </TabItem>
</Tabs>

After building your project with `./gradlew build`, you may find the produced "fat JAR" in the `build/libs` directory.
Its file name will be suffixed with `-all.jar`.
In you have Java 21 installed, you'll be able to run the application with the command

<Tabs groupId="posix2windows">
  <TabItem value="posix" label="Linux or macOS">
    ```bash
    java -jar ./build/libs/example-0.0.0-SNAPSHOT-all.jar
    ```
  </TabItem>
  <TabItem value="windows" label="Windows (PowerShell)">
    ```bash
    java -jar .\build\libs\example-0.0.0-SNAPSHOT-all.jar
    ```
  </TabItem>
</Tabs>

Be sure to replace `example-0.0.0-SNAPSHOT` with the name and version of your project.

### Writing tests

Our Gradle plugin automatically sets up [JUnit 5](https://junit.org/junit5/) for writing tests and [parameterized tests](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests).
It also sets up [Hamcrest](https://hamcrest.org/JavaHamcrest/) for writing assertions.
You should put your test files into the `src/test/java` directory in your projects.
You may run test with the commands `./gradlew test` or `./gradlew build`.

To ensure that your tests are properly isolated, you should *not* rely on the [`StandaloneRefinery`](pathname://../javadoc/refinery-generator/tools/refinery/generator/standalone/StandaloneRefinery.html) class from [`tools.refinery:refinery-generator`](pathname://../javadoc/refinery-generator/) when accessing Refinery generator components.
Instead, you should use Xtext's [dependency injection](https://eclipse.dev/Xtext/documentation/302_configuration.html#dependency-injection) and [unit testing](https://eclipse.dev/Xtext/documentation/103_domainmodelnextsteps.html#tutorial-unit-tests) support to instantiate the components. You'll need to add a dependency to Refinery's Xtext testing support library to your project.

<Tabs groupId="gradleLanguage">
  <TabItem value="kotlin" label="Kotlin">
    ```kotlin title="build.gradle.kts"
    dependencies {
        implementation(refinery.generator)
        // highlight-next-line
        testImplementation(testFixtures(refinery.language))
    }
    ```
  </TabItem>
  <TabItem value="groovy" label="Groovy">
    ```groovy title="build.gradle"
    dependencies {
        implementation refinery.generator
        // highlight-next-line
        testImplementation testFixtures(refinery.language)
    }
    ```
  </TabItem>
</Tabs>

The test fixtures for `refinery-language` include the `@InjectWithRefinery` [composed annotation](https://junit.org/junit5/docs/current/user-guide/#writing-tests-meta-annotations) to simplify Xtext injector configuration.
You can use this annotation in conjunction with `@Inject` annotations to set up your unit test.

```java
package org.example;

import com.google.inject.Inject;
import org.junit.jupiter.api.Test;
import tools.refinery.generator.GeneratorResult;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.tests.InjectWithRefinery;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// highlight-start
@InjectWithRefinery
// highlight-end
class ExampleTest {
    // highlight-start
    @Inject
    private ProblemLoader problemLoader;

    @Inject
    private ModelGeneratorFactory generatorFactory;
    // highlight-end

    @Test
    void testModelGeneration() throws IOException {
        var problem = problemLoader.loadString("""
            class Filesystem {
                contains Directory[1] root
            }

            class File.

            class Directory extends File {
                contains Directory[] children
            }

            scope Filesystem = 1, File = 20.
            """);
        try (var generator = generatorFactory.createGenerator(problem)) {
            var result = generator.tryGenerate();
            assertThat(result, is(GeneratorResult.SUCCESS));
        }
    }
}
```

### Multi-module projects

By default, the `tools.refinery.settings` plugin will apply our `tools.refinery.java` plugin to all Java projects in your build and configure them for use with Refinery. This is sufficient for single-module Java projects, and multi-module projects where all of your Java modules use Refinery.

If you wish to use Refinery in only some modules in your multi-module project, you can disable this behavior by adding

```ini title="gradle.properties"
tools.refinery.gradle.auto-apply=false
```

to the `gradle.properties` file in the root directory of your project.

If you use this setting, you'll need to add the `tools.refinery.java` plugin manually to any Java projects where you want to use Refinery like this:

<Tabs groupId="gradleLanguage">
  <TabItem value="kotlin" label="Kotlin">
    ```kotlin title="build.gradle.kts"
    plugins {
        id("tools.refinery.java")
    }
    ```
  </TabItem>
  <TabItem value="groovy" label="Groovy">
    ```groovy title="build.gradle"
    plugins {
        id 'tools.refinery.java'
    }
    ```
  </TabItem>
</Tabs>

Do *not* attempt to set a `version` for this plugin, because versioning is already managed by the `tools.refinery.settings` plugin. Trying to set a version for the `tools.refinery.java` plugin separately will result in a Gradle error.

## Working with Maven {#maven}

You may also develop applications based on Refinery using [Apache Maven](https://maven.apache.org/) as the build system.
Although we don't provide a Maven plugin for simplified configuration, you can still use our [platform](https://docs.gradle.org/current/userguide/platforms.html#sub:using-platform-to-control-transitive-deps) (Maven BOM) to lock the versions of Refinery and its dependencies to tested versions.

You should add the following configuration to your `pom.xml` file. If you use multi-module projects, we recommend that you add this to your parent POM.

```xml title="pom.xml"
<project>
    ...
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>tools.refinery</groupId>
                <artifactId>refinery-bom</artifactId>
                <version>0.2.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    ...
</project>
```

You'll be able to add dependencies to Refinery components without an explicit reference to the dependency version, since version numbers are managed by the BOM:

```xml title="pom.xml"
<project>
    ...
    <dependencies>
        <dependency>
            <groupId>tools.refinery</groupId>
            <artifactId>refinery-generator</artifactId>
        </dependency>
    </dependencies>
    ...
</project>
```

However, since the Maven BOM doesn't offer additional configuration, you'll have to take care of tasks such as configuring logging and testing, as well as building applications yourself.
