package tools.refinery.language.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import tools.refinery.language.model.problem.Problem;

@Singleton
public class ReferenceCounter {
	@Inject
	private IResourceScopeCache cache;

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
		return cache.get(problem, resource, () -> doGetReferenceCounts(problem));
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
