/*******************************************************************************
 * Copyright (c) 2010-2015, Abel Hegedus, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.internal;

import static tools.refinery.viatra.runtime.matchers.util.Preconditions.checkState;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.emf.types.EStructuralFeatureInstancesKey;
import tools.refinery.viatra.runtime.extensibility.ViatraQueryRuntimeConstants;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.surrogate.SurrogateQueryRegistry;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery;
import tools.refinery.viatra.runtime.matchers.util.IProvider;

/**
 * @author Abel Hegedus
 *
 */
public class ExtensionBasedSurrogateQueryLoader {

    private static final String DUPLICATE_SURROGATE_QUERY = "Duplicate surrogate query definition %s for feature %s of EClass %s in package %s (FQN in map %s, contributing plug-ins %s, plug-in %s)";
    
    private Map<String, String> contributingPluginOfFeatureMap = new HashMap<>();
    private Map<EStructuralFeature, PQueryProvider> contributedSurrogateQueries;

    private static final ExtensionBasedSurrogateQueryLoader INSTANCE = new ExtensionBasedSurrogateQueryLoader();

    /**
     * A provider implementation for PQuery instances based on extension elements. It is expected that the getter will only
     * @author Zoltan Ujhelyi
     *
     */
    private static final class PQueryProvider implements IProvider<PQuery> {

        private final IConfigurationElement element;
        private PQuery query;
        
        public PQueryProvider(IConfigurationElement element) {
            this.element = element;
            this.query = null;
        }

        @Override
        public PQuery get() {
            try {
                if (query == null) {
                    query = (PQuery) element.createExecutableExtension("surrogate-query");
                }
                return query;
            } catch (CoreException e) {
                throw new IllegalArgumentException("Error initializing surrogate query", e);
            }
        }
    }
    
    public static ExtensionBasedSurrogateQueryLoader instance() {
        return INSTANCE;
    }

    public void loadKnownSurrogateQueriesIntoRegistry() {
        Map<EStructuralFeature, PQueryProvider> knownSurrogateQueryFQNs = getSurrogateQueryProviders();
        for (Entry<EStructuralFeature, PQueryProvider> entry : knownSurrogateQueryFQNs.entrySet()) {
            final IInputKey inputKey = new EStructuralFeatureInstancesKey(entry.getKey());
            SurrogateQueryRegistry.instance().registerSurrogateQueryForFeature(inputKey, entry.getValue());
        }
    }
    
    private Map<EStructuralFeature, PQueryProvider> getSurrogateQueryProviders() {
        if(contributedSurrogateQueries != null) {
            return contributedSurrogateQueries;
        }
        contributedSurrogateQueries = new HashMap<>();
        if (Platform.isRunning()) {
            for (IConfigurationElement e : Platform.getExtensionRegistry().getConfigurationElementsFor(ViatraQueryRuntimeConstants.SURROGATE_QUERY_EXTENSIONID)) {
                if (e.isValid()) {
                    processExtension(e);
                }
            }
        }
        return contributedSurrogateQueries;
    }

    private void processExtension(IConfigurationElement el) {
        
        try {
            String packageUri = el.getAttribute("package-nsUri");
            String className = el.getAttribute("class-name");
            String featureName = el.getAttribute("feature-name");
            String queryFqn = el.getAttribute("query-fqn");
            if (queryFqn == null) {
                queryFqn = "";
            }
            PQueryProvider surrogateQueryProvider = new PQueryProvider(el); 
            
            String contributorName = el.getContributor().getName();
            StringBuilder featureIdBuilder = new StringBuilder();
            checkState(packageUri != null, "Package NsURI cannot be null in extension");
            checkState(className != null, "Class name cannot be null in extension");
            checkState(featureName != null, "Feature name cannot be null in extension");
            
            EPackage pckg = EPackage.Registry.INSTANCE.getEPackage(packageUri);
            featureIdBuilder.append(packageUri);
            checkState(pckg != null, "Package %s not found! (plug-in %s)", packageUri, contributorName);
            
            EClassifier clsr = pckg.getEClassifier(className);
            featureIdBuilder.append("##").append(className);
            checkState(clsr instanceof EClass, "EClassifier %s does not exist in package %s! (plug-in %s)", className, packageUri, contributorName);
            
            EClass cls = (EClass) clsr;
            EStructuralFeature feature = cls.getEStructuralFeature(featureName);
            featureIdBuilder.append("##").append(featureName);
            checkState(feature != null, "Feature %s of EClass %s in package %s not found! (plug-in %s)", featureName, className, packageUri, contributorName);
            
            PQueryProvider fqnInMap = contributedSurrogateQueries.get(feature);
            if(fqnInMap != null) {
                String duplicateSurrogateFormatString = DUPLICATE_SURROGATE_QUERY;
                Collection<String> contributorPlugins = Arrays.asList(contributorName, contributingPluginOfFeatureMap.get(featureIdBuilder.toString()));
                String duplicateSurrogateMessage = String.format(duplicateSurrogateFormatString, queryFqn, featureName,
                        className, packageUri, fqnInMap, contributorPlugins, contributorName);
                throw new IllegalStateException(duplicateSurrogateMessage);
            }
            contributedSurrogateQueries.put(feature, surrogateQueryProvider);
            contributingPluginOfFeatureMap.put(featureIdBuilder.toString(), contributorName);
        } catch (Exception e) {
            final Logger logger = Logger.getLogger(SurrogateQueryRegistry.class);
            logger.error("Surrogate query registration failed", e);
        }
    }
}
