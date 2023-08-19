/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime;

/**
 * Interface for storing string constants related to VIATRA Query's extension points.
 *
 * @author Istvan Rath
 *
 */
public interface IExtensions {

    public static final String QUERY_SPECIFICATION_EXTENSION_POINT_ID = ViatraQueryRuntimePlugin.PLUGIN_ID + ".queryspecification";

    public static final String INJECTOREXTENSIONID = ViatraQueryRuntimePlugin.PLUGIN_ID + ".injectorprovider";

}
