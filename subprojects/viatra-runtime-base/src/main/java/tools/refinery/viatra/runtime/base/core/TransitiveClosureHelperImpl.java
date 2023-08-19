/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EContentAdapter;
import tools.refinery.viatra.runtime.base.api.FeatureListener;
import tools.refinery.viatra.runtime.base.api.IndexingLevel;
import tools.refinery.viatra.runtime.base.api.InstanceListener;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.base.api.TransitiveClosureHelper;
import tools.refinery.viatra.runtime.base.itc.alg.incscc.IncSCCAlg;
import tools.refinery.viatra.runtime.base.itc.alg.misc.IGraphPathFinder;
import tools.refinery.viatra.runtime.base.itc.igraph.ITcObserver;

/**
 * Implementation class for the {@link TransitiveClosureHelper}.
 * It uses a {@link NavigationHelper} instance to wrap an EMF model 
 * and make it suitable for the {@link IncSCCAlg} algorithm. 
 * 
 * @author Tamas Szabo
 * 
 */
public class TransitiveClosureHelperImpl extends EContentAdapter implements TransitiveClosureHelper,
        ITcObserver<EObject>, FeatureListener, InstanceListener {

    private IncSCCAlg<EObject> sccAlg;
    private Set<EStructuralFeature> features;
    private Set<EClass> classes;
    private EMFDataSource dataSource;
    private List<ITcObserver<EObject>> tcObservers;
    private NavigationHelper navigationHelper;
    private boolean disposeBaseIndexWhenDisposed;
    
    public TransitiveClosureHelperImpl(final NavigationHelper navigationHelper, boolean disposeBaseIndexWhenDisposed, Set<EReference> references) {
        this.tcObservers = new ArrayList<ITcObserver<EObject>>();
        this.navigationHelper = navigationHelper;
        this.disposeBaseIndexWhenDisposed = disposeBaseIndexWhenDisposed;
        
        //NavigationHelper only accepts Set<EStructuralFeature> upon registration
        this.features = new HashSet<EStructuralFeature>(references);
        this.classes = collectEClasses();
        /*this.classes = Collections.emptySet();*/
        if (!navigationHelper.isInWildcardMode())
            navigationHelper.registerObservedTypes(classes, null, features, IndexingLevel.FULL);
        
        this.navigationHelper.addFeatureListener(features, this);
        this.navigationHelper.addInstanceListener(classes, this);
        
        this.dataSource = new EMFDataSource(navigationHelper, references, classes);
        
        this.sccAlg = new IncSCCAlg<EObject>(dataSource);
        this.sccAlg.attachObserver(this);
    }
    
    private Set<EClass> collectEClasses() {
        Set<EClass> classes = new HashSet<EClass>();
        for (EStructuralFeature ref : features) {
            classes.add(ref.getEContainingClass());
            classes.add(((EReference) ref).getEReferenceType());
        }
        return classes;
    }

    @Override
    public void attachObserver(ITcObserver<EObject> to) {
        this.tcObservers.add(to);
    }

    @Override
    public void detachObserver(ITcObserver<EObject> to) {
        this.tcObservers.remove(to);
    }

    @Override
    public Set<EObject> getAllReachableTargets(EObject source) {
        return this.sccAlg.getAllReachableTargets(source);
    }

    @Override
    public Set<EObject> getAllReachableSources(EObject target) {
        return this.sccAlg.getAllReachableSources(target);
    }

    @Override
    public boolean isReachable(EObject source, EObject target) {
        return this.sccAlg.isReachable(source, target);
    }

    @Override
    public void tupleInserted(EObject source, EObject target) {
        for (ITcObserver<EObject> to : tcObservers) {
            to.tupleInserted(source, target);
        }
    }

    @Override
    public void tupleDeleted(EObject source, EObject target) {
        for (ITcObserver<EObject> to : tcObservers) {
            to.tupleDeleted(source, target);
        }
    }

    @Override
    public void dispose() {
        this.sccAlg.dispose();
        this.navigationHelper.removeInstanceListener(classes, this);
        this.navigationHelper.removeFeatureListener(features, this);
        
        if (disposeBaseIndexWhenDisposed)
            this.navigationHelper.dispose();
    }

    @Override
    public void featureInserted(EObject host, EStructuralFeature feature, Object value) {
        this.dataSource.notifyEdgeInserted(host, (EObject) value);
    }

    @Override
    public void featureDeleted(EObject host, EStructuralFeature feature, Object value) {
        this.dataSource.notifyEdgeDeleted(host, (EObject) value);
    }

    @Override
    public void instanceInserted(EClass clazz, EObject instance) {
        this.dataSource.notifyNodeInserted(instance);
    }

    @Override
    public void instanceDeleted(EClass clazz, EObject instance) {
        this.dataSource.notifyNodeDeleted(instance);
    }
    
    @Override
    public IGraphPathFinder<EObject> getPathFinder() {
        return this.sccAlg.getPathFinder();
    }
}
