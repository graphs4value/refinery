/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.ide.contentassist;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.IdeCrossrefProposalProvider;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.xtext.CurrentTypeFinder;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.naming.ProblemQualifiedNameConverter;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.language.utils.BuiltinSymbols;
import tools.refinery.language.utils.ProblemDesugarer;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.language.validation.ReferenceCounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ProblemCrossrefProposalProvider extends IdeCrossrefProposalProvider {
	@Inject
	private CurrentTypeFinder currentTypeFinder;

	@Inject
	private ReferenceCounter referenceCounter;

	@Inject
	private ProblemDesugarer desugarer;

	@Inject
	private ImportCollector importCollector;

	@Override
	protected Iterable<IEObjectDescription> queryScope(IScope scope, CrossReference crossReference,
													   ContentAssistContext context) {
		var eObjectDescriptionsByName = new HashMap<ProblemResourceDescriptionStrategy.ShadowingKey,
				List<IEObjectDescription>>();
		for (var candidate : super.queryScope(scope, crossReference, context)) {
			if (isExistingObject(candidate, crossReference, context)) {
				var shadowingKey = ProblemResourceDescriptionStrategy.getShadowingKey(candidate);
				var candidateList = eObjectDescriptionsByName.computeIfAbsent(shadowingKey,
						ignored -> new ArrayList<>());
				candidateList.add(candidate);
			}
		}
		var eObjectDescriptions = new ArrayList<IEObjectDescription>();
		for (var candidates : eObjectDescriptionsByName.values()) {
			if (candidates.size() == 1) {
				var candidate = candidates.getFirst();
				if (shouldBeVisible(candidate, crossReference, context)) {
					eObjectDescriptions.add(candidate);
				}
			}
		}
		return eObjectDescriptions;
	}

	protected boolean isExistingObject(IEObjectDescription candidate, CrossReference crossRef,
									   ContentAssistContext context) {
		var rootModel = context.getRootModel();
		var eObjectOrProxy = candidate.getEObjectOrProxy();
		if (!Objects.equals(rootModel.eResource(), eObjectOrProxy.eResource())) {
			return true;
		}
		var currentValue = getCurrentValue(crossRef, context);
		if (currentValue == null) {
			return true;
		}
		var eObject = EcoreUtil.resolve(eObjectOrProxy, rootModel);
		if (!Objects.equals(currentValue, eObject)) {
			return true;
		}
		if (!ProblemUtil.isImplicit(eObject)) {
			return true;
		}
		if (rootModel instanceof Problem problem) {
			return referenceCounter.countReferences(problem, eObject) >= 2;
		}
		return true;
	}

	protected boolean shouldBeVisible(IEObjectDescription candidate, CrossReference crossReference,
									  ContentAssistContext context) {
		if (NamingUtil.isFullyQualified(candidate.getName()) &&
				!context.getPrefix().startsWith(ProblemQualifiedNameConverter.DELIMITER)) {
			// Do not propose names with a root prefix unless explicitly asked for.
			return false;
		}
		var errorPredicate = candidate.getUserData(ProblemResourceDescriptionStrategy.ERROR_PREDICATE);
		if (ProblemResourceDescriptionStrategy.ERROR_PREDICATE_TRUE.equals(errorPredicate)) {
			return false;
		}

		var eReference = getEReference(crossReference);
		if (eReference == null) {
			return true;
		}

		if (eReference == ProblemPackage.Literals.IMPORT_STATEMENT__IMPORTED_MODULE) {
			return importedModuleShouldBeVisible(candidate, context);
		}

		var candidateEObjectOrProxy = candidate.getEObjectOrProxy();

		if (eReference.equals(ProblemPackage.Literals.REFERENCE_DECLARATION__OPPOSITE) &&
				candidateEObjectOrProxy instanceof ReferenceDeclaration candidateReferenceDeclaration) {
			return oppositeShouldBeVisible(candidateReferenceDeclaration, context);
		}

		var builtinSymbolsOption = desugarer.getBuiltinSymbols(context.getRootModel());
		if (builtinSymbolsOption.isEmpty()) {
			return true;
		}
		var builtinSymbols = builtinSymbolsOption.get();

		return builtinSymbolAwareShouldBeVisible(candidate, context, eReference, builtinSymbols,
				candidateEObjectOrProxy);
	}

	private boolean importedModuleShouldBeVisible(IEObjectDescription candidate, ContentAssistContext context) {
		var moduleKind = candidate.getUserData(ProblemResourceDescriptionStrategy.MODULE_KIND);
		if (!ModuleKind.MODULE.getName().equals(moduleKind)) {
			return false;
		}
		var resource = context.getResource();
		var candidateResourceUri = candidate.getEObjectURI().trimFragment();
		if (candidateResourceUri.equals(resource.getURI())) {
			return false;
		}
		var imports = importCollector.getDirectImports(resource);
		return !imports.toUriSet().contains(candidateResourceUri);
	}

	private static boolean oppositeShouldBeVisible(ReferenceDeclaration candidateReferenceDeclaration,
												   ContentAssistContext context) {
		var referenceDeclaration = EcoreUtil2.getContainerOfType(context.getCurrentModel(),
				ReferenceDeclaration.class);
		if (referenceDeclaration == null) {
			return true;
		}
		var classDeclaration = EcoreUtil2.getContainerOfType(referenceDeclaration, ClassDeclaration.class);
		if (classDeclaration == null) {
			return true;
		}
		var oppositeType = candidateReferenceDeclaration.getReferenceType();
		if (oppositeType == null) {
			return true;
		}
		var resolvedOppositeType = EcoreUtil.resolve(oppositeType, candidateReferenceDeclaration);
		return classDeclaration.equals(resolvedOppositeType);
	}

	private boolean builtinSymbolAwareShouldBeVisible(
			IEObjectDescription candidate, ContentAssistContext context, EReference eReference,
			BuiltinSymbols builtinSymbols, EObject candidateEObjectOrProxy) {
		if (eReference.equals(ProblemPackage.Literals.REFERENCE_DECLARATION__REFERENCE_TYPE) &&
				context.getCurrentModel() instanceof ReferenceDeclaration referenceDeclaration &&
				(referenceDeclaration.getKind() == ReferenceKind.CONTAINMENT ||
						referenceDeclaration.getKind() == ReferenceKind.CONTAINER)) {
			// Containment or container references must have a class type.
			// We don't support {@code node} as a container or contained type.
			return ProblemPackage.Literals.CLASS_DECLARATION.isSuperTypeOf(candidate.getEClass()) &&
					!builtinSymbols.node().equals(candidateEObjectOrProxy);
		}

		if (eReference.equals(ProblemPackage.Literals.REFERENCE_DECLARATION__REFERENCE_TYPE) ||
				eReference.equals(ProblemPackage.Literals.PARAMETER__PARAMETER_TYPE) ||
				eReference.equals(ProblemPackage.Literals.TYPE_SCOPE__TARGET_TYPE)) {
			if (builtinSymbols.exists().equals(candidateEObjectOrProxy)) {
				return false;
			}
			var arity = candidate.getUserData(ProblemResourceDescriptionStrategy.ARITY);
			return arity == null || arity.equals("1");
		}

		if (eReference.equals(ProblemPackage.Literals.CLASS_DECLARATION__SUPER_TYPES)) {
			return supertypeShouldBeVisible(candidate, context, builtinSymbols, candidateEObjectOrProxy);
		}

		return true;
	}

	private boolean supertypeShouldBeVisible(IEObjectDescription candidate, ContentAssistContext context,
											 BuiltinSymbols builtinSymbols, EObject candidateEObjectOrProxy) {
		if (!ProblemPackage.Literals.CLASS_DECLARATION.isSuperTypeOf(candidate.getEClass()) ||
				builtinSymbols.node().equals(candidateEObjectOrProxy) ||
				builtinSymbols.contained().equals(candidateEObjectOrProxy)) {
			return false;
		}
		if (context.getCurrentModel() instanceof ClassDeclaration classDeclaration &&
				candidateEObjectOrProxy instanceof ClassDeclaration candidateClassDeclaration) {
			return !classDeclaration.equals(candidateClassDeclaration) &&
					!classDeclaration.getSuperTypes().contains(candidateClassDeclaration);
		}
		return true;
	}

	@Nullable
	private EReference getEReference(CrossReference crossReference) {
		var type = currentTypeFinder.findCurrentTypeAfter(crossReference);
		if (!(type instanceof EClass eClass)) {
			return null;
		}
		return GrammarUtil.getReference(crossReference, eClass);
	}

	protected EObject getCurrentValue(CrossReference crossRef, ContentAssistContext context) {
		var value = getCurrentValue(crossRef, context.getCurrentModel());
		if (value != null) {
			return value;
		}
		var currentNodeSemanticObject = NodeModelUtils.findActualSemanticObjectFor(context.getCurrentNode());
		return getCurrentValue(crossRef, currentNodeSemanticObject);
	}

	protected EObject getCurrentValue(CrossReference crossRef, EObject context) {
		if (context == null) {
			return null;
		}
		var eReference = GrammarUtil.getReference(crossRef, context.eClass());
		if (eReference == null || eReference.isMany()) {
			return null;
		}
		return (EObject) context.eGet(eReference);
	}
}
