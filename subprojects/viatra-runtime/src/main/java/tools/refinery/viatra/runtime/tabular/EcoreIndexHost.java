/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.tabular;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import tools.refinery.viatra.runtime.api.scope.QueryScope;
import tools.refinery.viatra.runtime.emf.EMFQueryMetaContext;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.emf.types.EClassExactInstancesKey;
import tools.refinery.viatra.runtime.emf.types.EClassTransitiveInstancesKey;
import tools.refinery.viatra.runtime.emf.types.EDataTypeInSlotsKey;
import tools.refinery.viatra.runtime.emf.types.EStructuralFeatureInstancesKey;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.scopes.IStorageBackend;
import tools.refinery.viatra.runtime.matchers.scopes.SimpleRuntimeContext;
import tools.refinery.viatra.runtime.matchers.scopes.tables.DisjointUnionTable;
import tools.refinery.viatra.runtime.matchers.scopes.tables.ITableWriterBinary;
import tools.refinery.viatra.runtime.matchers.scopes.tables.ITableWriterUnary;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;

/**
 * Simple EMF-specific demo tabular index host. 
 * 
 * <p> Usage: <ul>
 * <li> First, instantiate index host with given Ecore metamodel packages
 * <li> To emulate an EMF instance model, write arbitrary content into the tables using {@link #getTableDirectInstances(EClassifier)} and {@link #getTableFeatureSlots(EStructuralFeature)}.
 * <li> Initialize and evaluate regular EMF-based Viatra queries on the scope provided by {@link #getScope()}, as you would on an {@link EMFScope}. 
 * <ul> 
 * 
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @author Gabor Bergmann
 * @since 2.1
 */
public class EcoreIndexHost extends TabularIndexHost {
	
	public EcoreIndexHost(IStorageBackend storage, EPackage... packages) {
		super(storage, new SimpleRuntimeContext(EMFQueryMetaContext.DEFAULT_SURROGATE));
		
		initTables(packages);
	}
	
	@Override
    protected boolean isQueryScopeEmulated(Class<? extends QueryScope> queryScopeClass) {
        return EMFScope.class.equals(queryScopeClass);
    }

	
	private Map<EClassifier, ITableWriterUnary.Table<Object>> tableDirectInstances = CollectionsFactory.createMap();
	private Map<EClass, DisjointUnionTable> tableTransitiveInstances = CollectionsFactory.createMap();
	private Map<EStructuralFeature, ITableWriterBinary.Table<Object, Object>> tableFeatures = CollectionsFactory.createMap();
	
	private void initTables(EPackage[] packages) {
		
		// create instance tables first
		for (EPackage ePackage : packages) {
			for (EClassifier eClassifier : ePackage.getEClassifiers()) {
				boolean unique;
				IInputKey classifierKey;
				if (eClassifier instanceof EClass) {
					EClass eClass = (EClass) eClassifier;

					// create transitive instances table
					IInputKey transitiveKey = new EClassTransitiveInstancesKey(eClass);
					DisjointUnionTable transitiveTable = registerNewTable(new DisjointUnionTable(transitiveKey, runtimeContext));
					tableTransitiveInstances.put(eClass, transitiveTable);
					
					// process feature tables 
					for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
						IInputKey featureKey = new EStructuralFeatureInstancesKey(feature);
						ITableWriterBinary.Table<Object, Object> featureTable = newBinaryInputTable(featureKey, feature.isUnique());
						tableFeatures.put(feature, featureTable);
					}
					
					// direct instance table
					unique = true;
					classifierKey = new EClassExactInstancesKey(eClass);
				} else { // datatype
					unique = false;
					classifierKey = new EDataTypeInSlotsKey((EDataType) eClassifier);
				}
				ITableWriterUnary.Table<Object> directTable = newUnaryInputTable(classifierKey, unique);
				tableDirectInstances.put(eClassifier, directTable);
			}
		}
		
		// global implicit supertype EObject is always available as a transitive table
		EClass eObjectClass = EcorePackage.eINSTANCE.getEObject();
		DisjointUnionTable eObjectAllInstancesTable = tableTransitiveInstances.get(eObjectClass);
		if (eObjectAllInstancesTable == null) { // is it already added?
			IInputKey transitiveKey = new EClassTransitiveInstancesKey(eObjectClass);
			eObjectAllInstancesTable = registerNewTable(new DisjointUnionTable(transitiveKey, runtimeContext));
			tableTransitiveInstances.put(eObjectClass, eObjectAllInstancesTable);
			
			boolean unique = true;
			IInputKey classifierKey = new EClassExactInstancesKey(eObjectClass);
			ITableWriterUnary.Table<Object> directTable = newUnaryInputTable(classifierKey, unique);
			tableDirectInstances.put(eObjectClass, directTable);
		}
		
		// set up disjoint unoin tables
		for (Entry<EClass, DisjointUnionTable> entry : tableTransitiveInstances.entrySet()) {
			EClass eClass = entry.getKey();
			ITableWriterUnary.Table<Object> directTable = tableDirectInstances.get(eClass);
			
			// the direct type itself is a child
			entry.getValue().addChildTable(directTable);
			
			// connect supertypes
			for (EClass superClass : eClass.getEAllSuperTypes()) {
				DisjointUnionTable transitiveTable = tableTransitiveInstances.get(superClass);
				if (transitiveTable == null) {
				    throw new IllegalStateException(
				            String.format("No index table found for EClass %s, supertype of %s",
                                    superClass.getName(), eClass.getName())
                    );
                }
				transitiveTable.addChildTable(directTable);
			}
			// global implicit supertype
			if (!eClass.equals(eObjectClass)) {
				eObjectAllInstancesTable.addChildTable(directTable);
			}
				
		}
		
	}
	
	public ITableWriterUnary.Table<Object> getTableDirectInstances(EClassifier classifier) {
		return tableDirectInstances.get(classifier);
	}
	public ITableWriterBinary.Table<Object, Object> getTableFeatureSlots(EStructuralFeature feature) {
		return tableFeatures.get(feature);
	}


	
    public Set<Entry<EClassifier, ITableWriterUnary.Table<Object>>> getAllCurrentTablesDirectInstances() {
        return Collections.unmodifiableSet(tableDirectInstances.entrySet());
    }
    public Set<Entry<EStructuralFeature, ITableWriterBinary.Table<Object, Object>>> getAllCurrentTablesFeatures() {
        return Collections.unmodifiableSet(tableFeatures.entrySet());
    }

	
}
