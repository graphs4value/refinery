/*******************************************************************************
 * Copyright (c) 2008, 2018 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.language.serializer;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.conversion.IValueConverterService;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.serializer.diagnostic.ISerializationDiagnostic;
import org.eclipse.xtext.serializer.diagnostic.SerializationDiagnostic;
import org.eclipse.xtext.serializer.tokens.CrossReferenceSerializer;
import org.eclipse.xtext.serializer.tokens.SerializerScopeProviderBinding;
import org.eclipse.xtext.util.EmfFormatter;

import java.util.List;

public class ProblemCrossReferenceSerializer extends CrossReferenceSerializer {
	public static final String AMBIGUOUS_REFERENCE_DIAGNOSTIC = "tools.refinery.language.serializer" +
			".ProblemCrossReferenceSerializer.AMBIGUOUS_REFERENCE";

	@Inject
	private LinkingHelper linkingHelper;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	@SerializerScopeProviderBinding
	private IScopeProvider scopeProvider;

	@Inject
	private IValueConverterService valueConverter;

	@Inject
	private IGrammarAccess grammarAccess;

	@Override
	public String serializeCrossRef(EObject semanticObject, CrossReference crossref, EObject target, INode node,
									ISerializationDiagnostic.Acceptor errors) {
		if ((target == null || target.eIsProxy()) && node != null) {
			return tokenUtil.serializeNode(node);
		}

		final EReference ref = GrammarUtil.getReference(crossref, semanticObject.eClass());
		final IScope scope = scopeProvider.getScope(semanticObject, ref);
		if (scope == null) {
			if (errors != null) {
				errors.accept(diagnostics.getNoScopeFoundDiagnostic(semanticObject, crossref, target));
			}
			return null;
		}

		if (target != null && target.eIsProxy()) {
			target = handleProxy(target, semanticObject, ref);
		}

		if (target != null && node != null) {
			String text = linkingHelper.getCrossRefNodeAsString(node, true);
			QualifiedName qualifiedName = qualifiedNameConverter.toQualifiedName(text);
			URI targetUri = EcoreUtil2.getPlatformResourceOrNormalizedURI(target);
			if (isUniqueInScope(scope, qualifiedName, targetUri)) {
				return tokenUtil.serializeNode(node);
			}
		}

		return getCrossReferenceNameFromScope(semanticObject, crossref, target, scope, errors);
	}

	private boolean isUniqueInScope(IScope scope, QualifiedName qualifiedName, URI targetUri) {
		var iterator = scope.getElements(qualifiedName).iterator();
		if (!iterator.hasNext()) {
			return false;
		}
		var description = iterator.next();
		return targetUri.equals(description.getEObjectURI()) && !iterator.hasNext();
	}

	@Override
	protected String getCrossReferenceNameFromScope(EObject semanticObject, CrossReference crossref, EObject target,
													IScope scope, ISerializationDiagnostic.Acceptor errors) {
		var ruleName = linkingHelper.getRuleNameFrom(crossref);
		var targetUri = EcoreUtil2.getPlatformResourceOrNormalizedURI(target);
		FoundName foundName = FoundName.NONE;
		int shortestNameLength = Integer.MAX_VALUE;
		String shortestName = null;
		List<ISerializationDiagnostic> recordedErrors = null;
		for (var description : scope.getElements(target)) {
			var qualifiedName = description.getName();
			var segmentCount = qualifiedName.getSegmentCount();
			if (shortestName != null && segmentCount >= shortestNameLength) {
				continue;
			}
			if (isUniqueInScope(scope, qualifiedName, targetUri)) {
				var unconverted = qualifiedNameConverter.toString(qualifiedName);
				try {
					shortestName = valueConverter.toString(unconverted, ruleName);
					shortestNameLength = segmentCount;
					foundName = FoundName.VALID;
				} catch (ValueConverterException e) {
					if (recordedErrors == null) {
						recordedErrors = Lists.newArrayList();
					}
					recordedErrors.add(diagnostics.getValueConversionExceptionDiagnostic(semanticObject, crossref,
							unconverted, e));
				}
			} else if (foundName == FoundName.NONE) {
				foundName = FoundName.AMBIGUOUS;
			}
		}
		handleErrors(semanticObject, crossref, target, scope, errors, recordedErrors, foundName);
		return shortestName;
	}

	private void handleErrors(
			EObject semanticObject, CrossReference crossref, EObject target, IScope scope,
			ISerializationDiagnostic.Acceptor errors, List<ISerializationDiagnostic> recordedErrors,
			FoundName foundName) {
		if (errors == null) {
			return;
		}
		if (recordedErrors != null) {
			recordedErrors.forEach(errors::accept);
		}
		if (foundName == FoundName.NONE) {
			errors.accept(diagnostics.getNoEObjectDescriptionFoundDiagnostic(semanticObject, crossref, target,
					scope));
		} else if (foundName == FoundName.AMBIGUOUS) {
			// Computation of reference name copied from
			// {@link org.eclipse.xtext.serializer.diagnostic.TokenDiagnosticProvider#getFullReferenceName}.
			var ref = GrammarUtil.getReference(crossref);
			var clazz = semanticObject.eClass().getName();
			if (ref.getEContainingClass() != semanticObject.eClass()) {
                clazz = ref.getEContainingClass().getName() + "(" + clazz + ")";
            }
			var message = "No unambiguous name could be found in scope %s.%s for %s"
					.formatted(clazz, ref.getName(), EmfFormatter.objPath(target));
			var diagnostic = new SerializationDiagnostic(AMBIGUOUS_REFERENCE_DIAGNOSTIC, semanticObject, crossref,
					grammarAccess.getGrammar(), message);
			errors.accept(diagnostic);
		}
	}

	private enum FoundName {
		NONE,
		AMBIGUOUS,
		VALID
	}
}
