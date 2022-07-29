package tools.refinery.language;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.ImplicitVariable;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.ReferenceDeclaration;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.Variable;

public final class ProblemUtil {
	public static final String BUILTIN_LIBRARY_NAME = "builtin";

	public static final URI BUILTIN_LIBRARY_URI = getLibraryUri(BUILTIN_LIBRARY_NAME);

	public static final String NODE_CLASS_NAME = "node";

	private ProblemUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static boolean isSingletonVariable(Variable variable) {
		return variable.eContainingFeature() == ProblemPackage.Literals.VARIABLE_OR_NODE_ARGUMENT__SINGLETON_VARIABLE;
	}

	public static boolean isImplicitVariable(Variable variable) {
		return variable instanceof ImplicitVariable;
	}

	public static boolean isImplicitNode(Node node) {
		return node.eContainingFeature() == ProblemPackage.Literals.PROBLEM__NODES;
	}

	public static boolean isImplicit(EObject eObject) {
		if (eObject instanceof Node node) {
			return isImplicitNode(node);
		} else if (eObject instanceof Variable variable) {
			return isImplicitVariable(variable);
		} else {
			return false;
		}
	}

	public static boolean isIndividualNode(Node node) {
		var containingFeature = node.eContainingFeature();
		return containingFeature == ProblemPackage.Literals.INDIVIDUAL_DECLARATION__NODES
				|| containingFeature == ProblemPackage.Literals.ENUM_DECLARATION__LITERALS;
	}

	public static boolean isNewNode(Node node) {
		return node.eContainingFeature() == ProblemPackage.Literals.CLASS_DECLARATION__NEW_NODE;
	}

	public static Optional<Problem> getBuiltInLibrary(EObject context) {
		return Optional.ofNullable(context.eResource()).map(Resource::getResourceSet)
				.map(resourceSet -> resourceSet.getResource(BUILTIN_LIBRARY_URI, true)).map(Resource::getContents)
				.filter(contents -> !contents.isEmpty()).map(contents -> contents.get(0))
				.filter(Problem.class::isInstance).map(Problem.class::cast);
	}

	public static boolean isBuiltIn(EObject eObject) {
		if (eObject != null) {
			var eResource = eObject.eResource();
			if (eResource != null) {
				return BUILTIN_LIBRARY_URI.equals(eResource.getURI());
			}
		}
		return false;
	}

	public static Optional<ClassDeclaration> getNodeClassDeclaration(EObject context) {
		return getBuiltInLibrary(context).flatMap(problem -> problem.getStatements().stream()
				.filter(ClassDeclaration.class::isInstance).map(ClassDeclaration.class::cast)
				.filter(declaration -> NODE_CLASS_NAME.equals(declaration.getName())).findFirst());
	}

	public static Collection<ClassDeclaration> getSuperclassesAndSelf(ClassDeclaration classDeclaration) {
		Set<ClassDeclaration> found = new HashSet<>();
		getNodeClassDeclaration(classDeclaration).ifPresent(found::add);
		Deque<ClassDeclaration> queue = new ArrayDeque<>();
		queue.addLast(classDeclaration);
		while (!queue.isEmpty()) {
			ClassDeclaration current = queue.removeFirst();
			if (!found.contains(current)) {
				found.add(current);
				for (Relation superType : current.getSuperTypes()) {
					if (superType instanceof ClassDeclaration superDeclaration) {
						queue.addLast(superDeclaration);
					}
				}
			}
		}
		return found;
	}

	public static Collection<ReferenceDeclaration> getAllReferenceDeclarations(ClassDeclaration classDeclaration) {
		Set<ReferenceDeclaration> referenceDeclarations = new HashSet<>();
		for (ClassDeclaration superclass : getSuperclassesAndSelf(classDeclaration)) {
			referenceDeclarations.addAll(superclass.getReferenceDeclarations());
		}
		return referenceDeclarations;
	}

	private static URI getLibraryUri(String libraryName) {
		return URI.createURI(ProblemUtil.class.getClassLoader()
				.getResource("tools/refinery/language/%s.problem".formatted(libraryName)).toString());
	}
}
