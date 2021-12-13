package tools.refinery.language.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.building.DNFAnd;
import tools.refinery.store.query.building.DNFAtom;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.DNFPredicateCallAtom;
import tools.refinery.store.query.building.DirectRelationAtom;
import tools.refinery.store.query.building.RelationAtom;
import tools.refinery.store.query.building.Variable;
import tools.refinery.store.query.view.FilteredRelationView;
import tools.refinery.store.query.view.RelationView;

public class RelationViewConverter {
	public RelationViewMappings convertDirectPredicates(Collection<DNFPredicate> originalPredicates,
			Collection<Relation<TruthValue>> relations) {
		Map<Relation<TruthValue>, RelationView<TruthValue>> mayViewMap = new HashMap<>();
		Map<Relation<TruthValue>, RelationView<TruthValue>> mustViewMap = new HashMap<>();
		for (Relation<TruthValue> relation : relations) {
			mayViewMap.put(relation, new FilteredRelationView<>(relation, (k, v) -> v.may()));
			mustViewMap.put(relation, new FilteredRelationView<>(relation, (k, v) -> v.must()));
		}
		Set<DNFPredicate> dnfPredicates = new HashSet<>();
		Map<DNFPredicate, DNFPredicate> dnfPredicateConversions = createPredicateConversionMap(originalPredicates);
		Map<RelationView<TruthValue>, DNFPredicate> mayHelpers = new HashMap<>();
		Map<RelationView<TruthValue>, DNFPredicate> mustHelpers = new HashMap<>();
		for (Map.Entry<DNFPredicate, DNFPredicate> entry : dnfPredicateConversions.entrySet()) {
			DNFPredicate originalPredicate = entry.getKey();
			DNFPredicate newPredicate = entry.getValue();
			List<DNFAnd> clauses = newPredicate.getClauses();
			for (DNFAnd originalClause : originalPredicate.getClauses()) {
				List<DNFAtom> newAtoms = new ArrayList<>();
				for (DNFAtom originalAtom : originalClause.getConstraints()) {
					if (originalAtom instanceof DirectRelationAtom directRelationAtom) {
						List<DNFAtom> relationAtoms = convertDirectRelationView(mayHelpers, mustHelpers,
								mayViewMap.get(directRelationAtom.getRelation()),
								mustViewMap.get(directRelationAtom.getRelation()), directRelationAtom);
						newAtoms.addAll(relationAtoms);
					} else if (originalAtom instanceof DNFPredicateCallAtom dnfPredicateCallAtom) {
						DNFPredicateCallAtom callAtom = new DNFPredicateCallAtom(dnfPredicateCallAtom.isPositive(),
								dnfPredicateCallAtom.isTransitive(),
								dnfPredicateConversions.get(dnfPredicateCallAtom.getReferred()),
								dnfPredicateCallAtom.getSubstitution());
						newAtoms.add(callAtom);
					} else
						throw new UnsupportedOperationException("Unknown DNFAtom type");
				}
				clauses.add(new DNFAnd(originalClause.getExistentiallyQuantified(), newAtoms));
			}
			dnfPredicates.add(newPredicate);
		}
		return new RelationViewMappings(dnfPredicates, mayHelpers, mustHelpers, mayViewMap, mustViewMap,
				new HashSet<>(relations));
	}

	public Map<DNFPredicate, DNFPredicate> createPredicateConversionMap(Collection<DNFPredicate> originalPredicates) {
		Map<DNFPredicate, DNFPredicate> dnfPredicateConversions = new HashMap<>();
		for (DNFPredicate originalPred : originalPredicates) {
			dnfPredicateConversions.put(originalPred,
					new DNFPredicate(originalPred.getName(), originalPred.getVariables(), new ArrayList<DNFAnd>()));
		}
		return dnfPredicateConversions;
	}

	public DNFPredicateCallAtom createMayNotDNFPredicateHelper(RelationView<TruthValue> relationView,
			Map<RelationView<TruthValue>, DNFPredicate> mayHelpers, DirectRelationAtom directRelationAtom) {
		DNFPredicate mayPredicate = mayHelpers.get(relationView);
		if (mayPredicate == null) {
			List<Variable> substitution = new ArrayList<>();
			for (Variable variable : directRelationAtom.getSubstitution()) {
				substitution.add(new Variable(variable.getName()));
			}
			DNFAtom mayRelationAtom = new RelationAtom(relationView, substitution);
			DNFAnd clauses = new DNFAnd(Set.of(), List.of(mayRelationAtom));
			mayPredicate = new DNFPredicate("may" + directRelationAtom.getRelation().getName(), substitution,
					List.of(clauses));
			mayHelpers.put(relationView, mayPredicate);
		}
		return new DNFPredicateCallAtom(false, false, mayPredicate, directRelationAtom.getSubstitution());
	}

	public DNFPredicateCallAtom createMustNotDNFPredicateHelper(RelationView<TruthValue> relationView,
			Map<RelationView<TruthValue>, DNFPredicate> mustHelpers, DirectRelationAtom directRelationAtom) {
		DNFPredicate mustPredicate = mustHelpers.get(relationView);
		if (mustPredicate == null) {
			List<Variable> substitution = new ArrayList<>();
			for (Variable variable : directRelationAtom.getSubstitution()) {
				substitution.add(new Variable(variable.getName()));
			}
			DNFAtom mustRelationAtom = new RelationAtom(relationView, substitution);
			DNFAnd clauses = new DNFAnd(Set.of(), List.of(mustRelationAtom));
			mustPredicate = new DNFPredicate("must" + directRelationAtom.getRelation().getName(), substitution,
					List.of(clauses));
			mustHelpers.put(relationView, mustPredicate);
		}
		return new DNFPredicateCallAtom(false, false, mustPredicate, directRelationAtom.getSubstitution());
	}

	public List<DNFAtom> convertDirectRelationView(Map<RelationView<TruthValue>, DNFPredicate> mayHelpers,
			Map<RelationView<TruthValue>, DNFPredicate> mustHelpers, RelationView<TruthValue> mayRelView,
			RelationView<TruthValue> mustRelView, DirectRelationAtom directRelationAtom) {
		DNFAtom mayRelationAtom = new RelationAtom(mayRelView, directRelationAtom.getSubstitution());
		DNFAtom mustRelationAtom = new RelationAtom(mustRelView, directRelationAtom.getSubstitution());

		List<DNFAtom> dnfAtoms = new ArrayList<>();
		int truthValues = 0;
		final int T = 8, F = 4, U = 2, E = 1;
		for (TruthValue allowedTruthValues : directRelationAtom.getAllowedTruthValues()) {
			switch (allowedTruthValues) {
			case TRUE:
				truthValues += T;
				break;
			case FALSE:
				truthValues += F;
				break;
			case UNKNOWN:
				truthValues += U;
				break;
			case ERROR:
				truthValues += E;
				break;
			default:
				break;
			}
		}
		switch (truthValues) {
		case E:
			dnfAtoms.add(createMayNotDNFPredicateHelper(mayRelView, mayHelpers, directRelationAtom));
			dnfAtoms.add(mustRelationAtom);
			break;
		case U:
			dnfAtoms.add(mayRelationAtom);
			dnfAtoms.add(createMustNotDNFPredicateHelper(mustRelView, mustHelpers, directRelationAtom));
			break;
		case F:
			dnfAtoms.add(createMayNotDNFPredicateHelper(mayRelView, mayHelpers, directRelationAtom));
			dnfAtoms.add(createMustNotDNFPredicateHelper(mustRelView, mustHelpers, directRelationAtom));
			break;
		case F | E:
			dnfAtoms.add(createMayNotDNFPredicateHelper(mayRelView, mayHelpers, directRelationAtom));
			break;
		case F | U:
			dnfAtoms.add(createMustNotDNFPredicateHelper(mustRelView, mustHelpers, directRelationAtom));
			break;
		case T:
			dnfAtoms.add(mayRelationAtom);
			dnfAtoms.add(mustRelationAtom);
			break;
		case T | E:
			dnfAtoms.add(mustRelationAtom);
			break;
		case T | U:
			dnfAtoms.add(mayRelationAtom);
			break;
		case T | F | U | E:
			// Always satisfy
			break;
		default:
			throw new UnsupportedOperationException("Unsupported TruthValue combination");
		}
		return dnfAtoms;
	}
}
