---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
sidebar_position: 1
title: Contributing
---

import TabItem from '@theme/TabItem';
import Tabs from '@theme/Tabs';

# Contributing to Refinery

You can clone the refinery repository from GitHub at https://github.com/graphs4value/refinery.
If you want to contribute code, we recommend [forking](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo) the repository on GitHub so that you can submit a [pull request](https://github.com/graphs4value/refinery/pulls) later.

## Required tools

Refinery is written in Java and TypeScript. To build Refinery, you'll need a **Java 21** compatible **Java Development Kit (JDK).** We recommend the [Adoptium Java 21 JDK](https://adoptium.net/) or the [Amazon Corretto Java 21 JDK](https://aws.amazon.com/corretto/).

## Compiling Refinery {#compiling}

To build Refinery, run the command
```bash posix2windows
./gradlew build
```
in the cloned repository.

This should complete without any compilation errors.

If you get any errors about the JVM version, check whether the `JAVA_HOME` environment variable is set to the location of **JDK 21**. You can query the variable with
<Tabs groupId="posix2windows">
    <TabItem value="posix" label="Linux or macOS">
        ```bash
        echo $JAVA_HOME
        ```
    </TabItem>
    <TabItem value="windows" label="Windows (PowerShell)">
        ```bash
        echo $Env:JAVA_HOME
        ```
    </TabItem>
</Tabs>
To set the `JAVA_HOME` environmental variable, use
<Tabs groupId="posix2windows">
    <TabItem value="posix" label="Linux or macOS">
        ```bash
        export JAVA_HOME=/java/path/here
        ```
    </TabItem>
    <TabItem value="windows" label="Windows (PowerShell)">
        ```bash
        $Env:JAVA_HOME="C:\java\path\here"
        ```
    </TabItem>
</Tabs>

If the build fails with a `Host name must not be empty` error, you [might need to remove the empty proxy configuration from your global `gradle.properties` file](https://stackoverflow.com/a/62128323).

For further information, see the [supported build commands](/develop/contributing/commands) and the [instructions for setting up an IDE](/develop/contributing/ide-setup).
