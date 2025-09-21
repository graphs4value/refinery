/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.model.problem.Problem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ReferenceCounter {
	private static final String REFERENCE_COUNTS =
			"tools.refinery.language.validation.ReferenceCounter.REFERENCE_COUNTS";

	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;

	public int countReferences(Problem problem, EObject eObject) {
		var count = getOrComputeReferenceCounts(problem).get(eObject);
		if (count == null) {
			return 0;
		}
		return count;
	}

	protected Map<EObject, Integer> getOrComputeReferenceCounts(Problem problem) {
		var resource = problem.eResource();
		if (resource == null) {
			return computeReferenceCounts(problem);
		}
		return cache.get(Tuples.create(problem, REFERENCE_COUNTS), resource, () -> computeReferenceCounts(problem));
	}

	public static Map<EObject, Integer> computeReferenceCounts(List<? extends EObject> roots) {
		var map = new HashMap<EObject, Integer>();
		for (var root : roots) {
			computeReferenceCounts(root, map);
		}
		return map;
	}

	public static Map<EObject, Integer> computeReferenceCounts(EObject root) {
		var map = new HashMap<EObject, Integer>();
		computeReferenceCounts(root, map);
		return map;
	}

	private static void computeReferenceCounts(EObject root, Map<EObject, Integer> map) {
		countCrossReferences(root, map);
		var iterator = root.eAllContents();
		while (iterator.hasNext()) {
			var eObject = iterator.next();
			countCrossReferences(eObject, map);
		}
	}

	private static void countCrossReferences(EObject eObject, Map<EObject, Integer> map) {
		var featureIterator = (EContentsEList.FeatureIterator<EObject>) eObject.eCrossReferences().iterator();
		while (featureIterator.hasNext()) {
			var referencedObject = featureIterator.next();
			// Avoid double-counting the derived reference {@code variableOrNode} of {@code VariableOrNodeExpression},
			// as the original reference is just {@code element}.
			if (!featureIterator.feature().isDerived()) {
				map.compute(referencedObject, (key, currentValue) -> currentValue == null ? 1 : currentValue + 1);
			}
		}
	}
}
