/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.IScopeProvider;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;

import java.util.Locale;

public class NodeNameProvider {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private IScopeProvider scopeProvider;

	private Problem problem;
	private final MutableObjectIntMap<String> indexMap = ObjectIntMaps.mutable.empty();

	public void setProblem(Problem problem) {
		if (this.problem != null) {
			throw new IllegalStateException("Problem was already set");
		}
		this.problem = problem;
	}

	public String getNextName(String typeName) {
		if (problem == null) {
			throw new IllegalStateException("Problem was not set");
		}
		String namePrefix;
		if (typeName == null || typeName.isEmpty()) {
			namePrefix = "node";
		} else {
			namePrefix = typeName.substring(0, 1).toLowerCase(Locale.ROOT) + typeName.substring(1);
		}
		int index = indexMap.getIfAbsent(namePrefix, 0);
		String nodeName;
		QualifiedName qualifiedName;
		var scope = scopeProvider.getScope(problem, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE);
		do {
			index++;
			nodeName = namePrefix + index;
			qualifiedName = qualifiedNameConverter.toQualifiedName(nodeName);
		} while (semanticsUtils.maybeGetElement(problem, scope, qualifiedName, Node.class) != null);
		indexMap.put(namePrefix, index);
		return nodeName;
	}
}
