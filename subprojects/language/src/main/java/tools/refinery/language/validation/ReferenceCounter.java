/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.model.problem.Problem;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class ReferenceCounter {
	private static final String REFERENCE_COUNTS =
			"tools.refinery.language.validation.ReferenceCounter.REFERENCE_COUNTS";

	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;

	public int countReferences(Problem problem, EObject eObject) {
		var count = getReferenceCounts(problem).get(eObject);
		if (count == null) {
			return 0;
		}
		return count;
	}

	protected Map<EObject, Integer> getReferenceCounts(Problem problem) {
		var resource = problem.eResource();
		if (resource == null) {
			return doGetReferenceCounts(problem);
		}
		return cache.get(Tuples.create(problem, REFERENCE_COUNTS), resource, () -> doGetReferenceCounts(problem));
	}

	protected Map<EObject, Integer> doGetReferenceCounts(Problem problem) {
		var map = new HashMap<EObject, Integer>();
		countCrossReferences(problem, map);
		var iterator = problem.eAllContents();
		while (iterator.hasNext()) {
			var eObject = iterator.next();
			countCrossReferences(eObject, map);
		}
		return map;
	}

	protected void countCrossReferences(EObject eObject, Map<EObject, Integer> map) {
		for (var referencedObject : eObject.eCrossReferences()) {
			map.compute(referencedObject, (key, currentValue) -> currentValue == null ? 1 : currentValue + 1);
		}
	}
}
