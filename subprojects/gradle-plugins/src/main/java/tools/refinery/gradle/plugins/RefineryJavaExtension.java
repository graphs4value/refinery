/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.plugins;

import org.gradle.api.provider.Property;

public abstract class RefineryJavaExtension {
	public abstract Property<Boolean> getAddBundleSymbolicName();

	public abstract Property<Boolean> getDistTar();

	public abstract Property<Boolean> getDistZip();

	public abstract Property<Boolean> getEnforcePlatform();

	public abstract Property<String> getRefineryVersion();

	public abstract Property<TestDependencies> getTestDependencies();

	public abstract Property<Boolean> getUseSlf4JLog4J();

	public abstract Property<Boolean> getUseSlf4JSimple();
}
