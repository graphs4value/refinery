/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Note: this file contains methods copied from EContentAdapter.java of the EMF project
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import tools.refinery.viatra.runtime.base.api.BaseIndexOptions;
import tools.refinery.viatra.runtime.base.api.filters.IBaseIndexObjectFilter;
import tools.refinery.viatra.runtime.base.api.filters.IBaseIndexResourceFilter;
import tools.refinery.viatra.runtime.base.comprehension.EMFModelComprehension;
import tools.refinery.viatra.runtime.base.comprehension.EMFVisitor;
import tools.refinery.viatra.runtime.base.core.NavigationHelperVisitor.ChangeVisitor;

/**
 * Content Adapter that recursively attaches itself to the containment hierarchy of an EMF model. 
 * The purpose is to gather the contents of the model, and to subscribe to model change notifications. 
 * 
 * <p> Originally, this was implemented as a subclass of {@link EContentAdapter}. 
 * Because of Bug 490105, EContentAdapter is no longer a superclass; its code is copied over with modifications.
 * See {@link EContentAdapter} header for original authorship and copyright information. 
 * 
 * @author Gabor Bergmann
 * @see EContentAdapter
 * @noextend This class is not intended to be subclassed by clients.
 */
public class NavigationHelperContentAdapter extends AdapterImpl {

    private final NavigationHelperImpl navigationHelper;



    // move optimization to avoid removing and re-adding entire subtrees
    EObject ignoreInsertionAndDeletion;
    // Set<EObject> ignoreRootInsertion = new HashSet<EObject>();
    // Set<EObject> ignoreRootDeletion = new HashSet<EObject>();

    private final EMFModelComprehension comprehension;

    private IBaseIndexObjectFilter objectFilterConfiguration;
    private IBaseIndexResourceFilter resourceFilterConfiguration;



    private EMFVisitor removalVisitor;
    private EMFVisitor insertionVisitor;

    public NavigationHelperContentAdapter(final NavigationHelperImpl navigationHelper) {
        this.navigationHelper = navigationHelper;
        final BaseIndexOptions options = this.navigationHelper.getBaseIndexOptions();
        objectFilterConfiguration = options.getObjectFilterConfiguration();
        resourceFilterConfiguration = options.getResourceFilterConfiguration();
        this.comprehension = navigationHelper.getComprehension();
        
        removalVisitor = initChangeVisitor(false);
        insertionVisitor = initChangeVisitor(true);
    }

    /**
     * Point of customization, called by constructor.
     */
    protected ChangeVisitor initChangeVisitor(boolean isInsertion) {
        return new NavigationHelperVisitor.ChangeVisitor(navigationHelper, isInsertion);
    }

    // key representative of the EObject class


    @Override
    public void notifyChanged(final Notification notification) {
        try {
            this.navigationHelper.coalesceTraversals(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    simpleNotifyChanged(notification);

                    final Object oFeature = notification.getFeature();
                    final Object oNotifier = notification.getNotifier();
                    if (oNotifier instanceof EObject && oFeature instanceof EStructuralFeature) {
                        final EObject notifier = (EObject) oNotifier;
                        final EStructuralFeature feature = (EStructuralFeature) oFeature;

                        final boolean notifyLightweightObservers = handleNotification(notification, notifier, feature);

                        if (notifyLightweightObservers) {
                            navigationHelper.notifyLightweightObservers(notifier, feature, notification);
                        }
                    } else if (oNotifier instanceof Resource) {
                        if (notification.getFeatureID(Resource.class) == Resource.RESOURCE__IS_LOADED) {
                            final Resource resource = (Resource) oNotifier;
                            if (comprehension.isLoading(resource))
                                navigationHelper.resolutionDelayingResources.add(resource);
                            else
                                navigationHelper.resolutionDelayingResources.remove(resource);
                        }
                    }
                    return null;
                }
            });
        } catch (final InvocationTargetException ex) {
            navigationHelper.processingFatal(ex.getCause(), "handling the following update notification: " + notification);
        } catch (final Exception ex) {
            navigationHelper.processingFatal(ex, "handling the following update notification: " + notification);
        }

        navigationHelper.notifyBaseIndexChangeListeners();
    }

    @SuppressWarnings("deprecation")
    protected boolean handleNotification(final Notification notification, final EObject notifier,
            final EStructuralFeature feature) {
        final Object oldValue = notification.getOldValue();
        final Object newValue = notification.getNewValue();
        final int positionInt = notification.getPosition();
        final Integer position = positionInt == Notification.NO_INDEX ? null : positionInt;
        final int eventType = notification.getEventType();
        boolean notifyLightweightObservers = true;
        switch (eventType) {
        case Notification.ADD:
            featureUpdate(true, notifier, feature, newValue, position);
            break;
        case Notification.ADD_MANY:
            for (final Object newElement : (Collection<?>) newValue) {
                featureUpdate(true, notifier, feature, newElement, position);
            }
            break;
        case Notification.CREATE:
            notifyLightweightObservers = false;
            break;
        case Notification.MOVE:
            // lightweight observers should be notified on MOVE
            break; // currently no support for ordering
        case Notification.REMOVE:
            featureUpdate(false, notifier, feature, oldValue, position);
            break;
        case Notification.REMOVE_MANY:
            for (final Object oldElement : (Collection<?>) oldValue) {
                featureUpdate(false, notifier, feature, oldElement, position);
            }
            break;
        case Notification.REMOVING_ADAPTER:
            notifyLightweightObservers = false;
            break;
        case Notification.RESOLVE: // must be EReference
            if (navigationHelper.isFeatureResolveIgnored(feature))
                break; // otherwise same as SET
            if (!feature.isMany()) { // if single-valued, can be removed from delayed resolutions
                navigationHelper.delayedProxyResolutions.removePairOrNop(notifier, (EReference) feature);
            }
            featureUpdate(false, notifier, feature, oldValue, position);
            featureUpdate(true, notifier, feature, newValue, position);
            break;
        case Notification.UNSET:
        case Notification.SET:
            if(feature.isMany() && position == null){
                // spurious UNSET notification of entire collection
                notifyLightweightObservers = false;
            } else {
                featureUpdate(false, notifier, feature, oldValue, position);
                featureUpdate(true, notifier, feature, newValue, position);
            }
            break;
        default:
            notifyLightweightObservers = false;
            break;
        }
        return notifyLightweightObservers;
    }

    protected void featureUpdate(final boolean isInsertion, final EObject notifier, final EStructuralFeature feature,
            final Object value, final Integer position) {
        // this is a safe visitation, no reads will happen, thus no danger of notifications or matcher construction
        comprehension.traverseFeature(getVisitorForChange(isInsertion), notifier, feature, value, position);
    }

    // OFFICIAL ENTRY POINT OF BASE INDEX RELATED PARTS
    protected void addAdapter(final Notifier notifier) {
        if (notifier == ignoreInsertionAndDeletion) {
            return;
        }
        try {
            // cross-resource containment workaround, see Bug 483089 and Bug 483086.
            if (notifier.eAdapters().contains(this))
                return;

            if (objectFilterConfiguration != null && objectFilterConfiguration.isFiltered(notifier)) {
                return;
            }
            this.navigationHelper.coalesceTraversals(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // the object is really traversed BEFORE the notification listener is added,
                    // so that if a proxy is resolved due to the traversal, we do not get notified about it
                    if (notifier instanceof EObject) {
                        comprehension.traverseObject(getVisitorForChange(true), (EObject) notifier);
                    } else if (notifier instanceof Resource) {
                        Resource resource = (Resource) notifier;
                        if (resourceFilterConfiguration != null
                                && resourceFilterConfiguration.isResourceFiltered(resource)) {
                            return null;
                        }
                        if (comprehension.isLoading(resource))
                            navigationHelper.resolutionDelayingResources.add(resource);
                    }
                    // subscribes to the adapter list, will receive setTarget callback that will spread addAdapter to
                    // children
                    simpleAddAdapter(notifier);
                    return null;
                }
            });
        } catch (final InvocationTargetException ex) {
            navigationHelper.processingFatal(ex.getCause(), "add the object: " + notifier);
        } catch (final Exception ex) {
            navigationHelper.processingFatal(ex, "add the object: " + notifier);
        }
    }

    // OFFICIAL ENTRY POINT OF BASE INDEX RELATED PARTS
    protected void removeAdapter(final Notifier notifier) {
        if (notifier == ignoreInsertionAndDeletion) {
            return;
        }
        try {
            removeAdapterInternal(notifier);
        } catch (final InvocationTargetException ex) {
            navigationHelper.processingFatal(ex.getCause(), "remove the object: " + notifier);
        } catch (final Exception ex) {
            navigationHelper.processingFatal(ex, "remove the object: " + notifier);
        }
    }

    // The additional boolean options are there to save the cost of extra checks, see Bug 483089 and Bug 483086.
    protected void removeAdapter(final Notifier notifier, boolean additionalObjectContainerPossible,
            boolean additionalResourceContainerPossible) {
        if (notifier == ignoreInsertionAndDeletion) {
            return;
        }
        try {

            // cross-resource containment workaround, see Bug 483089 and Bug 483086.
            if (notifier instanceof InternalEObject) {
                InternalEObject internalEObject = (InternalEObject) notifier;
                if (additionalResourceContainerPossible) {
                    Resource eDirectResource = internalEObject.eDirectResource();
                    if (eDirectResource != null && eDirectResource.eAdapters().contains(this)) {
                        return;
                    }
                }
                if (additionalObjectContainerPossible) {
                    InternalEObject eInternalContainer = internalEObject.eInternalContainer();
                    if (eInternalContainer != null && eInternalContainer.eAdapters().contains(this)) {
                        return;
                    }
                }
            }

            removeAdapterInternal(notifier);
        } catch (final InvocationTargetException ex) {
            navigationHelper.processingFatal(ex.getCause(), "remove the object: " + notifier);
        } catch (final Exception ex) {
            navigationHelper.processingFatal(ex, "remove the object: " + notifier);
        }
    }

    /**
     * @throws InvocationTargetException
     */
    protected void removeAdapterInternal(final Notifier notifier) throws InvocationTargetException {
        // some non-standard EMF implementations send these 
        if (!notifier.eAdapters().contains(this)) {
            // the adapter was not even attached to the notifier
            navigationHelper.logIncidentAdapterRemoval(notifier);

            // skip the rest of the method, do not traverse contents
            //  as they have either never been added to the index or already removed
            return;
        }

        if (objectFilterConfiguration != null && objectFilterConfiguration.isFiltered(notifier)) {
            return;
        }
        this.navigationHelper.coalesceTraversals(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (notifier instanceof EObject) {
                    final EObject eObject = (EObject) notifier;
                    comprehension.traverseObject(getVisitorForChange(false), eObject);
                    navigationHelper.delayedProxyResolutions.lookupAndRemoveAll(eObject);
                } else if (notifier instanceof Resource) {
                    if (resourceFilterConfiguration != null
                            && resourceFilterConfiguration.isResourceFiltered((Resource) notifier)) {
                        return null;
                    }
                    navigationHelper.resolutionDelayingResources.remove(notifier);
                }
                // unsubscribes from the adapter list, will receive unsetTarget callback that will spread
                // removeAdapter to children
                simpleRemoveAdapter(notifier);
                return null;
            }
        });
    }

    protected EMFVisitor getVisitorForChange(final boolean isInsertion) {
        return isInsertion ? insertionVisitor : removalVisitor;
    }


    // WORKAROUND (TMP) for eContents vs. derived features bug
    protected void setTarget(final EObject target) {
        basicSetTarget(target);
        spreadToChildren(target, true);
    }

    protected void unsetTarget(final EObject target) {
        basicUnsetTarget(target);
        spreadToChildren(target, false);
    }

    // Spread adapter removal/addition to children of EObject
    protected void spreadToChildren(final EObject target, final boolean add) {
        final EList<EReference> features = target.eClass().getEAllReferences();
        for (final EReference feature : features) {
            if (!feature.isContainment()) {
                continue;
            }
            if (!comprehension.representable(feature)) {
                continue;
            }
            if (feature.isMany()) {
                final Collection<?> values = (Collection<?>) target.eGet(feature);
                for (final Object value : values) {
                    final Notifier notifier = (Notifier) value;
                    if (add) {
                        addAdapter(notifier);
                    } else {
                        removeAdapter(notifier, false, true);
                    }
                }
            } else {
                final Object value = target.eGet(feature);
                if (value != null) {
                    final Notifier notifier = (Notifier) value;
                    if (add) {
                        addAdapter(notifier);
                    } else {
                        removeAdapter(notifier, false, true);
                    }
                }
            }
        }
    }


    //
    // ***********************************************************
    // RENAMED METHODS COPIED OVER FROM EContentAdapter DOWN BELOW
    // ***********************************************************
    //
    
    /**
     * Handles a notification by calling {@link #selfAdapt selfAdapter}.
     */
    public void simpleNotifyChanged(Notification notification)
    {
      selfAdapt(notification);

      super.notifyChanged(notification);
    }

    protected void simpleAddAdapter(Notifier notifier)
    {
      EList<Adapter> eAdapters = notifier.eAdapters();
      if (!eAdapters.contains(this))
      {
        eAdapters.add(this); 
      }
    }
    
    protected void simpleRemoveAdapter(Notifier notifier)
    {
      notifier.eAdapters().remove(this); 
    }


    // 
    // *********************************************************
    // CODE COPIED OVER VERBATIM FROM EContentAdapter DOWN BELOW
    // *********************************************************
    //
    

    /**
     * Handles a notification by calling {@link #handleContainment handleContainment}
     * for any containment-based notification.
     */
    protected void selfAdapt(Notification notification)
    {
      Object notifier = notification.getNotifier();
      if (notifier instanceof ResourceSet)
      {
        if (notification.getFeatureID(ResourceSet.class) == ResourceSet.RESOURCE_SET__RESOURCES)
        {
          handleContainment(notification);
        }
      }
      else if (notifier instanceof Resource)
      {
        if (notification.getFeatureID(Resource.class) == Resource.RESOURCE__CONTENTS)
        {
          handleContainment(notification);
        }
      }
      else if (notifier instanceof EObject)
      {
        Object feature = notification.getFeature();
        if (feature instanceof EReference)
        {
          EReference eReference = (EReference)feature;
          if (eReference.isContainment())
          {
            handleContainment(notification);
          }
        }
      }
    }

    /**
     * Handles a containment change by adding and removing the adapter as appropriate.
     */
    protected void handleContainment(Notification notification)
    {
      switch (notification.getEventType())
      {
        case Notification.RESOLVE:
        {
          // We need to be careful that the proxy may be resolved while we are attaching this adapter.
          // We need to avoid attaching the adapter during the resolve 
          // and also attaching it again as we walk the eContents() later.
          // Checking here avoids having to check during addAdapter.
          //
          Notifier oldValue = (Notifier)notification.getOldValue();
          if (oldValue.eAdapters().contains(this))
          {
            removeAdapter(oldValue);
            Notifier newValue = (Notifier)notification.getNewValue();
            addAdapter(newValue);
          }
          break;
        }
        case Notification.UNSET:
        {
          Object oldValue = notification.getOldValue();
          if (!Objects.equals(oldValue, Boolean.TRUE) && !Objects.equals(oldValue, Boolean.FALSE))
          {
            if (oldValue != null)
            {
              removeAdapter((Notifier)oldValue, false, true);
            }
            Notifier newValue = (Notifier)notification.getNewValue();
            if (newValue != null)
            {
              addAdapter(newValue);
            }
          }
          break;
        }
        case Notification.SET:
        {
          Notifier oldValue = (Notifier)notification.getOldValue();
          if (oldValue != null)
          {
            removeAdapter(oldValue, false, true);
          }
          Notifier newValue = (Notifier)notification.getNewValue();
          if (newValue != null)
          {
            addAdapter(newValue);
          }
          break;
        }
        case Notification.ADD:
        {
          Notifier newValue = (Notifier)notification.getNewValue();
          if (newValue != null)
          {
            addAdapter(newValue);
          }
          break;
        }
        case Notification.ADD_MANY:
        {
          @SuppressWarnings("unchecked") Collection<Notifier> newValues = (Collection<Notifier>)notification.getNewValue();
          for (Notifier newValue : newValues)
          {
            addAdapter(newValue);
          }
          break;
        }
        case Notification.REMOVE:
        {
          Notifier oldValue = (Notifier)notification.getOldValue();
          if (oldValue != null)
          {
            boolean checkContainer = notification.getNotifier() instanceof Resource;
            boolean checkResource = notification.getFeature() != null;
            removeAdapter(oldValue, checkContainer, checkResource);
          }
          break;
        }
        case Notification.REMOVE_MANY:
        {
          boolean checkContainer = notification.getNotifier() instanceof Resource;
          boolean checkResource = notification.getFeature() != null;
          @SuppressWarnings("unchecked") Collection<Notifier> oldValues = (Collection<Notifier>)notification.getOldValue();
          for ( Notifier oldContentValue : oldValues)
          {
            removeAdapter(oldContentValue, checkContainer, checkResource);
          }
          break;
        }
      }
    }

    /**
     * Handles installation of the adapter
     * by adding the adapter to each of the directly contained objects.
     */
    @Override
    public void setTarget(Notifier target)
    {
      if (target instanceof EObject)
      {
        setTarget((EObject)target);
      }
      else if (target instanceof Resource)
      {
        setTarget((Resource)target);
      }
      else if (target instanceof ResourceSet)
      {
        setTarget((ResourceSet)target);
      }
      else
      {
        basicSetTarget(target);
      }
    }
    
    /**
     * Actually sets the target by calling super.
     */
    protected void basicSetTarget(Notifier target)
    {
      super.setTarget(target);
    }

    /**
     * Handles installation of the adapter on a Resource
     * by adding the adapter to each of the directly contained objects.
     */
    protected void setTarget(Resource target)
    {
      basicSetTarget(target);
      List<EObject> contents = target.getContents();
      for (int i = 0, size = contents.size(); i < size; ++i)
      {
        Notifier notifier = contents.get(i);
        addAdapter(notifier);
      }
    }

    /**
     * Handles installation of the adapter on a ResourceSet
     * by adding the adapter to each of the directly contained objects.
     */
    protected void setTarget(ResourceSet target)
    {
      basicSetTarget(target);
      List<Resource> resources =  target.getResources();
      for (int i = 0; i < resources.size(); ++i)
      {
        Notifier notifier = resources.get(i);
        addAdapter(notifier);
      }
    }

    /**
     * Handles undoing the installation of the adapter
     * by removing the adapter from each of the directly contained objects.
     */
    @Override
    public void unsetTarget(Notifier target)
    {
      Object target1 = target;
    if (target1 instanceof EObject)
      {
        unsetTarget((EObject)target1);
      }
      else if (target1 instanceof Resource)
      {
        unsetTarget((Resource)target1);
      }
      else if (target1 instanceof ResourceSet)
      {
        unsetTarget((ResourceSet)target1);
      }
      else
      {
        basicUnsetTarget((Notifier)target1);
      }
    }

    /**
     * Actually unsets the target by calling super.
     */
    protected void basicUnsetTarget(Notifier target)
    {
      super.unsetTarget(target);
    }

    /**
     * Handles undoing the installation of the adapter from a Resource
     * by removing the adapter from each of the directly contained objects.
     */
    protected void unsetTarget(Resource target)
    {
      basicUnsetTarget(target);
      List<EObject> contents = target.getContents();
      for (int i = 0, size = contents.size(); i < size; ++i)
      {
        Notifier notifier = contents.get(i);
        removeAdapter(notifier, true, false);
      }
    }

    /**
     * Handles undoing the installation of the adapter from a ResourceSet
     * by removing the adapter from each of the directly contained objects.
     */
    protected void unsetTarget(ResourceSet target)
    {
      basicUnsetTarget(target);
      List<Resource> resources =  target.getResources();
      for (int i = 0; i < resources.size(); ++i)
      {
        Notifier notifier = resources.get(i);
        removeAdapter(notifier, false, false);
      }
    }

    protected boolean resolve()
    {
      return true;
    }

    // 
    // *********************************************************
    // OBSOLETE CODE COPIED OVER FROM EContentAdapter DOWN BELOW
    // *********************************************************
    //
    // *** Preserved on purpose as comments, 
    // ***  in order to more easily follow future changes to EContentAdapter.
    //


//  protected void removeAdapter(Notifier notifier, boolean checkContainer, boolean checkResource)
//  {
//    if (checkContainer || checkResource)
//    {
//      InternalEObject internalEObject = (InternalEObject) notifier;
//      if (checkResource)
//      {
//        Resource eDirectResource = internalEObject.eDirectResource();
//        if (eDirectResource != null && eDirectResource.eAdapters().contains(this))
//        {
//          return;
//        }
//      }
//      if (checkContainer)
//      {
//        InternalEObject eInternalContainer = internalEObject.eInternalContainer();
//        if (eInternalContainer != null && eInternalContainer.eAdapters().contains(this))
//        {
//          return;
//        }
//      }
//    }
//
//    removeAdapter(notifier);
//  }

//  /**
//   * Handles undoing the installation of the adapter from an EObject
//   * by removing the adapter from each of the directly contained objects.
//   */
//  protected void unsetTarget(EObject target)
//  {
//    basicUnsetTarget(target);
//    for (Iterator<? extends Notifier> i = resolve() ? 
//           target.eContents().iterator() : 
//           ((InternalEList<EObject>)target.eContents()).basicIterator(); 
//         i.hasNext(); )
//    {
//      Notifier notifier = i.next();
//      removeAdapter(notifier, false, true);
//    }
//  }

//  /**
//   * Handles installation of the adapter on an EObject
//   * by adding the adapter to each of the directly contained objects.
//   */
//  protected void setTarget(EObject target)
//  {
//    basicSetTarget(target);
//    for (Iterator<? extends Notifier> i = resolve() ? 
//           target.eContents().iterator() : 
//           ((InternalEList<? extends Notifier>)target.eContents()).basicIterator();
//         i.hasNext(); )
//    {
//      Notifier notifier = i.next();
//      addAdapter(notifier);
//    }
//  }

}
