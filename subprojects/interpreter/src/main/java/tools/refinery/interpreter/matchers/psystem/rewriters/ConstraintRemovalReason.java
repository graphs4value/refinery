/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

/**
 * Common reasons for removing constraint through rewriters
 *
 * @noreference This enum is not intended to be referenced by clients.
 */
public enum ConstraintRemovalReason implements IDerivativeModificationReason {

    MOOT_EQUALITY,
    WEAK_INEQUALITY_SELF_LOOP,
    TYPE_SUBSUMED,
    DUPLICATE

}
