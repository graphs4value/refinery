/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import org.eclipse.emf.common.util.URI;

public record TransitiveImport(URI uri) implements Import {
}
