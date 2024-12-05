/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import tools.refinery.language.annotations.Annotation;

import java.util.List;

record CollectedAnnotations(List<Annotation> instances, boolean repeatable) {
}
