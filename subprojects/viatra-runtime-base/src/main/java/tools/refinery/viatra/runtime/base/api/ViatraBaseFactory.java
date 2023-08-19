/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.api;

import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.base.core.NavigationHelperImpl;
import tools.refinery.viatra.runtime.base.core.TransitiveClosureHelperImpl;

/**
 * Factory class for the utils in the library: <ul>
 * <li>NavigationHelper (automatic and manual) 
 * <li>TransitiveClosureUtil
 * </ul>
 * 
 * @author Tamas Szabo
 * 
 */
public class ViatraBaseFactory {

    private static ViatraBaseFactory instance;

    /**
     * Get the singleton instance of ViatraBaseFactory.
     * 
     * @return the singleton instance
     */
    public static synchronized ViatraBaseFactory getInstance() {
        if (instance == null) {
            instance = new ViatraBaseFactory();
        }

        return instance;
    }

    protected ViatraBaseFactory() {
        super();
    }

    /**
     * The method creates a {@link NavigationHelper} index for the given EMF model root. 
     * A new instance will be created on every call.
     * <p>
     * A NavigationHelper in wildcard mode will process and index all EStructuralFeatures, EClasses and EDatatypes. If
     * wildcard mode is off, the client will have to manually register the interesting aspects of the model.
     * <p>
     * The NavigationHelper will be created without dynamic EMF support by default. 
     * See {@link #createNavigationHelper(Notifier, boolean, boolean, Logger)} for more options.
     * 
     * @see NavigationHelper
     * 
     * @param emfRoot
     *            the root of the EMF tree to be indexed. Recommended: Resource or ResourceSet. Can be null - you can
     *            add a root later using {@link NavigationHelper#addRoot(Notifier)}
     * @param wildcardMode
     *            true if all aspects of the EMF model should be indexed automatically, false if manual registration of
     *            interesting aspects is desirable
     * @param logger
     *            the log output where errors will be logged if encountered during the operation of the
     *            NavigationHelper; if null, the default logger for {@link NavigationHelper} is used.
     * @return the NavigationHelper instance
     * @throws ViatraQueryRuntimeException
     */
    public NavigationHelper createNavigationHelper(Notifier emfRoot, boolean wildcardMode, Logger logger) {
        BaseIndexOptions options = new BaseIndexOptions(false, wildcardMode ? IndexingLevel.FULL : IndexingLevel.NONE);
        return createNavigationHelper(emfRoot, options, logger);
    }
    
    /**
     * The method creates a {@link NavigationHelper} index for the given EMF model root.
     * A new instance will be created on every call.
     * <p>
     * A NavigationHelper in wildcard mode will process and index all EStructuralFeatures, EClasses and EDatatypes. If
     * wildcard mode is off, the client will have to manually register the interesting aspects of the model.
     * <p>
     * If the dynamic model flag is set to true, the index will use String ids to distinguish between the various 
     * {@link EStructuralFeature}, {@link EClass} and {@link EDataType} instances. This way the index is able to 
     * handle dynamic EMF instance models too. 
     * 
     * @see NavigationHelper
     * 
     * @param emfRoot
     *            the root of the EMF tree to be indexed. Recommended: Resource or ResourceSet. Can be null - you can
     *            add a root later using {@link NavigationHelper#addRoot(Notifier)}
     * @param wildcardMode
     *            true if all aspects of the EMF model should be indexed automatically, false if manual registration of
     *            interesting aspects is desirable
     * @param dynamicModel
     *            true if the index should use String ids (nsURIs) for the various EMF types and features, and treat  
     *            multiple EPackages sharing an nsURI as the same. false if dynamic model support is not required
     * @param logger
     *            the log output where errors will be logged if encountered during the operation of the
     *            NavigationHelper; if null, the default logger for {@link NavigationHelper} is used.
     * @return the NavigationHelper instance
     * @throws ViatraQueryRuntimeException
     */
    public NavigationHelper createNavigationHelper(Notifier emfRoot, boolean wildcardMode, boolean dynamicModel, Logger logger) {
        BaseIndexOptions options = new BaseIndexOptions(dynamicModel, wildcardMode ? IndexingLevel.FULL : IndexingLevel.NONE);
        return createNavigationHelper(emfRoot, options, logger);
    }
    
    /**
     * The method creates a {@link NavigationHelper} index for the given EMF model root.
     * A new instance will be created on every call.
     * <p>
     * For details of base index options including wildcard and dynamic EMF mode, see {@link BaseIndexOptions}.
     *
     * @see NavigationHelper
     * 
     * @param emfRoot
     *            the root of the EMF tree to be indexed. Recommended: Resource or ResourceSet. Can be null - you can
     *            add a root later using {@link NavigationHelper#addRoot(Notifier)}
     * @param options the options used by the index 
     * @param logger
     *            the log output where errors will be logged if encountered during the operation of the
     *            NavigationHelper; if null, the default logger for {@link NavigationHelper} is used.
     * @return the NavigationHelper instance
     * @throws ViatraQueryRuntimeException
     */
    public NavigationHelper createNavigationHelper(Notifier emfRoot, BaseIndexOptions options, Logger logger) {
        Logger l = logger;
        if (l == null)
            l = Logger.getLogger(NavigationHelper.class);
        return new NavigationHelperImpl(emfRoot, options, l);
    }
    
    

    /**
     * The method creates a TransitiveClosureHelper instance for the given EMF model root.
     * A new instance will be created on every call.
     * 
     * <p>
     * One must specify the set of EReferences that will be considered as edges. The set can contain multiple elements;
     * this way one can query forward and backward reachability information along heterogenous paths.
     * 
     * @param emfRoot
     *            the root of the EMF tree to be processed. Recommended: Resource or ResourceSet.
     * @param referencesToObserve
     *            the set of references to observe
     * @return the TransitiveClosureHelper instance
     * @throws ViatraQueryRuntimeException if the creation of the internal NavigationHelper failed
     */
    public TransitiveClosureHelper createTransitiveClosureHelper(Notifier emfRoot, Set<EReference> referencesToObserve) {
        return new TransitiveClosureHelperImpl(getInstance().createNavigationHelper(emfRoot, false, null), true, referencesToObserve);
    }

    /**
     * The method creates a TransitiveClosureHelper instance built on an existing NavigationHelper.
     * A new instance will be created on every call.
     * 
     * <p>
     * One must specify the set of EReferences that will be considered as edges. The set can contain multiple elements;
     * this way one can query forward and backward reachability information along heterogenous paths.
     * 
     * @param baseIndex
     *            the already existing NavigationHelper index on the model
     * @param referencesToObserve
     *            the set of references to observe
     * @return the TransitiveClosureHelper instance
     */
    public TransitiveClosureHelper createTransitiveClosureHelper(NavigationHelper baseIndex, Set<EReference> referencesToObserve) {
        return new TransitiveClosureHelperImpl(baseIndex, false, referencesToObserve);
    }
    
    
}
