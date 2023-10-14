/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.recipes.helper;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jetbrains.annotations.Nullable;
import tools.refinery.interpreter.rete.recipes.RecipesPackage;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores a set of known <em>canonical</em> recipes, each representing a disjoint equivalence class of recipes, modulo
 * {@link #isEquivalentRecipe(ReteNodeRecipe, ReteNodeRecipe)}.
 *
 * @author Gabor Bergmann
 * @since 1.3
 */
public class RecipeRecognizer {
	private static final AtomicLong nextRecipeEquivalenceClassID = new AtomicLong(0);

	/**
	 * if EcoreUtil.equals(recipe1, recipe2), only one of them will be included here
	 */
	Map<Long, Set<ReteNodeRecipe>> canonicalRecipesByHashCode = new HashMap<>();
	Map<Long, ReteNodeRecipe> canonicalRecipeByEquivalenceClassID = new HashMap<>();
	Set<ReteNodeRecipe> canonicalRecipesWithoutHashCode = new LinkedHashSet<>();

	private final IQueryRuntimeContext runtimeContext;

	/**
	 * @param runtimeContext can be null; if provided, further equivalences can be detected based on
	 *                       {@link IQueryRuntimeContext#wrapElement(Object)}
	 * @since 1.6
	 */
	public RecipeRecognizer(IQueryRuntimeContext runtimeContext) {
		this.runtimeContext = runtimeContext;
	}

	public RecipeRecognizer() {
		this(null);
	}

	/**
	 * Recognizes when an equivalent canonical recipe is already known.
	 *
	 * @return an equivalent canonical recipe, or the null if no known equivalent found
	 */
	public ReteNodeRecipe peekCanonicalRecipe(final ReteNodeRecipe recipe) {
		var recipeByEquivalenceClass = getRecipeByEquivalenceClass(recipe);
		if (recipeByEquivalenceClass != null) {
			return recipeByEquivalenceClass;
		}

		var hashCode = computeHashCode(recipe);
		if (hashCode != null) {
			var recipeByHashCode = getRecipeByHashCode(recipe, hashCode);
			if (recipeByHashCode != null) {
				return recipeByHashCode;
			}
		}

		// If we already designated {@code recipe} as canonical during a recursive call in {@code computeHashCode},
		// it will be found here, and we will move it to {@code canonicalRecipesByHashCode}. This could be improved by
		// checking whether {@code recipe} is already canonical explicitly if there are many recursive patterns.
		return getRecipeAndAssignHashCode(recipe, hashCode);
	}

	@Nullable
	private ReteNodeRecipe getRecipeByEquivalenceClass(ReteNodeRecipe recipe) {
		for (Long classID : recipe.getEquivalenceClassIDs()) {
			ReteNodeRecipe knownRecipe = canonicalRecipeByEquivalenceClassID.get(classID);
			if (knownRecipe != null) {
				return knownRecipe;
			}
		}
		return null;
	}

	@Nullable
	private ReteNodeRecipe getRecipeByHashCode(ReteNodeRecipe recipe, Long hashCode) {
		var equivalentRecipesByHashCode = canonicalRecipesByHashCode.get(hashCode);
		if (equivalentRecipesByHashCode != null) {
			for (ReteNodeRecipe knownRecipe : equivalentRecipesByHashCode) {
				if (isEquivalentRecipe(recipe, knownRecipe)) {
					// FOUND EQUIVALENT RECIPE
					recipe.getEquivalenceClassIDs().add(knownRecipe.getEquivalenceClassIDs().get(0));
					return knownRecipe;
				}
			}
		}
		return null;
	}

	@Nullable
	private ReteNodeRecipe getRecipeAndAssignHashCode(ReteNodeRecipe recipe, Long hashCode) {
		var iterator = canonicalRecipesWithoutHashCode.iterator();
		while (iterator.hasNext()) {
			var knownRecipe = iterator.next();
			if (isEquivalentRecipe(recipe, knownRecipe)) {
				// FOUND EQUIVALENT RECIPE
				recipe.getEquivalenceClassIDs().add(knownRecipe.getEquivalenceClassIDs().get(0));
				var cachedHashCode = knownRecipe.getCachedHashCode();
				if (cachedHashCode != null && !cachedHashCode.equals(hashCode)) {
					throw new AssertionError("Cached recipe %s already had hash code %s"
							.formatted(knownRecipe, cachedHashCode));
				}
				if (hashCode != null) {
					knownRecipe.setCachedHashCode(hashCode);
					addHashCodeRepresentative(hashCode, knownRecipe);
					iterator.remove();
				}
				return knownRecipe;
			}
		}
		return null;
	}

	private final Deque<EObject> hashCodeStack = new ArrayDeque<>();

	private Long computeHashCode(Object object) {
		if (object instanceof List<?> list) {
			return computeListHashCode(list);
		}
		if (object instanceof ReteNodeRecipe recipe) {
			return ensureRecipeHashCode(recipe);
		}
		if (object instanceof EObject eObject) {
			return computeEObjectHashCode(eObject);
		}
		return (long) Objects.hashCode(object);
	}

	private Long computeHashCodeOrEquivalenceClassId(Object object) {
		if (object instanceof ReteNodeRecipe recipe) {
			var equivalenceClassIDs = recipe.getEquivalenceClassIDs();
			if (!equivalenceClassIDs.isEmpty()) {
				return equivalenceClassIDs.get(0);
			}
			if (hashCodeStack.contains(recipe)) {
				return null;
			}
			var canonicalRecipe = canonicalizeRecipe(recipe);
			return canonicalRecipe.getEquivalenceClassIDs().get(0);
		} else {
			return computeHashCode(object);
		}
	}

	private Long computeListHashCode(List<?> list) {
		long result = 1;
		for (var item : list) {
			var update = computeHashCodeOrEquivalenceClassId(item);
			if (update == null) {
				return null;
			}
			result = result * 37 + update;
		}
		return result;
	}

	private Long ensureRecipeHashCode(ReteNodeRecipe recipe) {
		var hashCode = recipe.getCachedHashCode();
		if (hashCode != null) {
			return hashCode;
		}
		hashCode = computeEObjectHashCode(recipe);
		if (hashCode == null) {
			return null;
		}
		recipe.setCachedHashCode(hashCode);
		return hashCode;
	}

	private Long computeEObjectHashCode(EObject eObject) {
		if (hashCodeStack.contains(eObject)) {
			return null;
		}
		hashCodeStack.addLast(eObject);
		try {
			long result = eObject.eClass().hashCode();
			for (var feature : eObject.eClass().getEAllStructuralFeatures()) {
				if (eObject instanceof ReteNodeRecipe && (
						RecipesPackage.Literals.RETE_NODE_RECIPE__EQUIVALENCE_CLASS_IDS.equals(feature) ||
								RecipesPackage.Literals.RETE_NODE_RECIPE__CACHED_HASH_CODE.equals(feature) ||
								RecipesPackage.Literals.RETE_NODE_RECIPE__CONSTRUCTED.equals(feature))) {
					continue;
				}
				var value = eObject.eGet(feature);
				var update = computeHashCodeOrEquivalenceClassId(value);
				if (update == null) {
					return null;
				}
				result = result * 37 + update;
			}
			return result;
		} finally {
			hashCodeStack.removeLast();
		}
	}

	private void addHashCodeRepresentative(Long hashCode, ReteNodeRecipe recipe) {
		canonicalRecipesByHashCode.computeIfAbsent(hashCode, ignored -> new LinkedHashSet<>()).add(recipe);
	}

	/**
	 * This recipe will be remembered as a canonical recipe. Method maintains both internal data structures and the
	 * equivalence class attribute of the recipe. PRECONDITION: {@link #peekCanonicalRecipe(ReteNodeRecipe)} must
	 * return null or the recipe itself
	 */
	public void makeCanonical(final ReteNodeRecipe recipe) {
		// this is a canonical recipe, chosen representative of its new
		// equivalence class
		if (recipe.getEquivalenceClassIDs().isEmpty()) {
			recipe.getEquivalenceClassIDs().add(nextRecipeEquivalenceClassID.getAndIncrement());
		}
		for (Long classID : recipe.getEquivalenceClassIDs()) {
			canonicalRecipeByEquivalenceClassID.put(classID, recipe);
		}
		var hashCode = computeHashCode(recipe);
		if (hashCode == null) {
			canonicalRecipesWithoutHashCode.add(recipe);
		} else {
			addHashCodeRepresentative(hashCode, recipe);
		}
	}

	/**
	 * Ensures that there is an equivalent canonical recipe; if none is known yet, this recipe will be remembered as
	 * canonical.
	 *
	 * @return an equivalent canonical recipe; the argument recipe itself (which is made canonical) if no known
	 * equivalent found
	 */
	public ReteNodeRecipe canonicalizeRecipe(final ReteNodeRecipe recipe) {
		ReteNodeRecipe knownRecipe = peekCanonicalRecipe(recipe);
		if (knownRecipe == null) {
			knownRecipe = recipe;
			makeCanonical(recipe);
		}
		return knownRecipe;
	}

	/**
	 * @return true iff recipe is a canonical recipe
	 */
	public boolean isKnownCanonicalRecipe(final ReteNodeRecipe recipe) {
		if (recipe.getEquivalenceClassIDs().isEmpty()) {
			return false;
		}
		ReteNodeRecipe knownRecipe = canonicalRecipeByEquivalenceClassID.get(recipe.getEquivalenceClassIDs().get(0));
		return recipe == knownRecipe;
	}

	private boolean isEquivalentRecipe(ReteNodeRecipe recipe, ReteNodeRecipe knownRecipe) {
		return new EqualityHelper(runtimeContext).equals(recipe, knownRecipe);
	}

	// TODO reuse in more cases later, e.g. switching join node parents, etc.
	private static class EqualityHelper extends EcoreUtil.EqualityHelper {
		private static final long serialVersionUID = -8841971394686015188L;

		private static final EAttribute RETE_NODE_RECIPE_EQUIVALENCE_CLASS_IDS =
				RecipesPackage.eINSTANCE.getReteNodeRecipe_EquivalenceClassIDs();
		private static final EAttribute RETE_NODE_RECIPE_CACHED_HASH_CODE =
				RecipesPackage.eINSTANCE.getReteNodeRecipe_CachedHashCode();
		private static final EAttribute RETE_NODE_RECIPE_CONSTRUCTED =
				RecipesPackage.eINSTANCE.getReteNodeRecipe_Constructed();
		private static final EAttribute DISCRIMINATOR_BUCKET_RECIPE_BUCKET_KEY =
				RecipesPackage.eINSTANCE.getDiscriminatorBucketRecipe_BucketKey();

		private final IQueryRuntimeContext runtimeContext;

		public EqualityHelper(IQueryRuntimeContext runtimeContext) {
			this.runtimeContext = runtimeContext;
		}

		@Override
		protected boolean haveEqualFeature(EObject eObject1, EObject eObject2, EStructuralFeature feature) {
			// ignore differences in this attribute, as it may only be assigned
			// after the equivalence check
			if (RETE_NODE_RECIPE_EQUIVALENCE_CLASS_IDS.equals(feature) ||
					RETE_NODE_RECIPE_CACHED_HASH_CODE.equals(feature) ||
					RETE_NODE_RECIPE_CONSTRUCTED.equals(feature))
				return true;

			if (runtimeContext != null) {
				// constant values
				if (DISCRIMINATOR_BUCKET_RECIPE_BUCKET_KEY.equals(feature)) {
					// use runtime context to map to canonical wrapped form
					// this is costly for constant recipes (TODO improve this), but essential for discriminator buckets

					Object val1 = eObject1.eGet(feature);
					Object val2 = eObject2.eGet(feature);

					if (val1 != null && val2 != null) {
						return runtimeContext.wrapElement(val1).equals(runtimeContext.wrapElement(val2));
					} else {
						return val1 == null && val2 == null;
					}

				}
			}

			// fallback to general comparison
			return super.haveEqualFeature(eObject1, eObject2, feature);
		}

		@Override
		public boolean equals(EObject eObject1, EObject eObject2) {
			// short-circuit if already known to be equivalent
			if (eObject1 instanceof ReteNodeRecipe) {
				if (eObject2 instanceof ReteNodeRecipe) {
					EList<Long> eqClassIDs1 = ((ReteNodeRecipe) eObject1).getEquivalenceClassIDs();
					EList<Long> eqClassIDs2 = ((ReteNodeRecipe) eObject2).getEquivalenceClassIDs();

					if (!Collections.disjoint(eqClassIDs1, eqClassIDs2))
						return true;
				}
			}

			// fallback to general comparison
			return super.equals(eObject1, eObject2);
		}
	}
}
