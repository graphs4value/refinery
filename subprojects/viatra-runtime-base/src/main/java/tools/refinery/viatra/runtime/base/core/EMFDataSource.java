/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.base.itc.igraph.IGraphDataSource;
import tools.refinery.viatra.runtime.base.itc.igraph.IGraphObserver;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.IMemoryView;
import tools.refinery.viatra.runtime.matchers.util.IMultiset;

// TODO IBiDirectionalGraphDataSource
public class EMFDataSource implements IGraphDataSource<EObject> {

    private List<IGraphObserver<EObject>> observers;
    private Set<EReference> references;
    private Set<EClass> classes;
    private NavigationHelper navigationHelper;
    private IMultiset<EObject> allEObjects; // contains objects even if only appearing as sources or targets

    /**
     * @param navigationHelper
     * @param references
     * @param classes
     *            additional classes to treat as nodes. Source and target classes of references need not be added.
     */
    public EMFDataSource(NavigationHelper navigationHelper, Set<EReference> references, Set<EClass> classes) {
        this.references = references;
        this.classes = classes;
        this.observers = new LinkedList<IGraphObserver<EObject>>();
        this.navigationHelper = navigationHelper;
    }

    @Override
    public Set<EObject> getAllNodes() {
        return getAllEObjects().distinctValues();
    }

    @Override
    public IMemoryView<EObject> getTargetNodes(EObject source) {
        IMultiset<EObject> targetNodes = CollectionsFactory.createMultiset();

        for (EReference ref : references) {
            final Set<EObject> referenceValues = navigationHelper.getReferenceValues(source, ref);
            for (EObject referenceValue : referenceValues) {
                targetNodes.addOne(referenceValue);
            }
        }

        return targetNodes;
    }

    @Override
    public void attachObserver(IGraphObserver<EObject> go) {
        observers.add(go);
    }

    @Override
    public void attachAsFirstObserver(IGraphObserver<EObject> observer) {
        observers.add(0, observer);
    }

    @Override
    public void detachObserver(IGraphObserver<EObject> go) {
        observers.remove(go);
    }

    public void notifyEdgeInserted(EObject source, EObject target) {
        nodeAdditionInternal(source);
        nodeAdditionInternal(target);
        for (IGraphObserver<EObject> o : observers) {
            o.edgeInserted(source, target);
        }
    }

    public void notifyEdgeDeleted(EObject source, EObject target) {
        for (IGraphObserver<EObject> o : observers) {
            o.edgeDeleted(source, target);
        }
        nodeRemovalInternal(source);
        nodeRemovalInternal(target);
    }

    public void notifyNodeInserted(EObject node) {
        nodeAdditionInternal(node);
    }

    public void notifyNodeDeleted(EObject node) {
        nodeRemovalInternal(node);
    }

    private void nodeAdditionInternal(EObject node) {
        if (allEObjects.addOne(node))
            for (IGraphObserver<EObject> o : observers) {
                o.nodeInserted(node);
            }
    }

    private void nodeRemovalInternal(EObject node) {
        if (getAllEObjects().removeOne(node))
            for (IGraphObserver<EObject> o : observers) {
                o.nodeDeleted(node);
            }
    }

    protected IMultiset<EObject> getAllEObjects() {
        if (allEObjects == null) {
            allEObjects = CollectionsFactory.createMultiset();
            for (EClass clazz : classes) {
                for (EObject obj : navigationHelper.getAllInstances(clazz)) {                    
                    allEObjects.addOne(obj);
                }
            }
            for (EReference ref : references) {
                navigationHelper.processAllFeatureInstances(ref, (source, target) -> {
                    allEObjects.addOne(source);
                    allEObjects.addOne((EObject) target);
                });
            }
        }
        return allEObjects;
    }
}
