/*******************************************************************************
 * Copyright (c) 2004-2010 Akos Horvath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.exception;

import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;
import tools.refinery.viatra.runtime.matchers.planning.QueryProcessingException;
import tools.refinery.viatra.runtime.matchers.psystem.queries.QueryInitializationException;

/**
 * A general VIATRA Query-related problem during the operation of the VIATRA Query
 * engine, or the loading, manipulation and evaluation of queries.
 * 
 * @author Bergmann Gabor
 * @since 0.9
 * 
 */
public class ViatraQueryException extends ViatraQueryRuntimeException {

    private static final long serialVersionUID = -74252748358355750L;

    public static final String PARAM_NOT_SUITABLE_WITH_NO = "The type of the parameters are not suitable for the operation. Parameter number: ";
    public static final String CONVERSION_FAILED = "Could not convert the term to the designated type";
    public static final String CONVERT_NULL_PARAMETER = "Could not convert null to the designated type";
    public static final String RELATIONAL_PARAM_UNSUITABLE = "The parameters are not acceptable by the operation";
    /**
     * @since 0.9
     */
    public static final String PROCESSING_PROBLEM = "The following error occurred during the processing of a query (e.g. for the preparation of a VIATRA pattern matcher)";
    /**
     * @since 0.9
     */
    public static final String QUERY_INIT_PROBLEM = "The following error occurred during the initialization of a VIATRA query specification";
    public static final String GETNAME_FAILED = "Could not get 'name' attribute of the result";

    public static final String INVALID_EMFROOT = "Incremental EMF query engine can only be attached on the contents of an EMF EObject, Resource, ResourceSet or multiple ResourceSets. Received instead: ";
    public static final String INVALID_EMFROOT_SHORT = "Invalid EMF model root";
    // public static final String EMF_MODEL_PROCESSING_ERROR = "Error while processing the EMF model";

    private final String shortMessage;

    public ViatraQueryException(String s, String shortMessage) {
        super(s);
        this.shortMessage = shortMessage;
    }

    public ViatraQueryException(QueryProcessingException e) {
        super(PROCESSING_PROBLEM + ": " + e.getMessage(), e);
        this.shortMessage = e.getShortMessage();
    }
    
    public ViatraQueryException(QueryInitializationException e) {
        super(QUERY_INIT_PROBLEM + ": " + e.getMessage(), e);
        this.shortMessage = e.getShortMessage();
    }

    public ViatraQueryException(String s, String shortMessage, Throwable e) {
        super(s + ": " + e.getMessage(), e);
        this.shortMessage = shortMessage;
    }

    public String getShortMessage() {
        return shortMessage;
    }

}
