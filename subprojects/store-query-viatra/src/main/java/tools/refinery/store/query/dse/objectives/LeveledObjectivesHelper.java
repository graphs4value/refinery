/*******************************************************************************
 * Copyright (c) 2010-2016, Andras Szabolcs Nagy and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.query.dse.objectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LeveledObjectivesHelper {

	private List<Objective> objectives = new ArrayList<Objective>();
	private Objective[][] leveledObjectives;

	public LeveledObjectivesHelper(List<Objective> objectives) {
		this.objectives = objectives;
	}

	public Objective[][] initLeveledObjectives() {
		if (objectives.isEmpty()) {
			leveledObjectives = new Objective[0][0];
			return leveledObjectives;
		}

		int level = objectives.get(0).getLevel();
		boolean oneLevelOnly = true;
		for (Objective objective : objectives) {
			if (objective.getLevel() != level) {
				oneLevelOnly = false;
				break;
			}
		}

		if (oneLevelOnly) {
			leveledObjectives = new Objective[1][objectives.size()];
			for (int i = 0; i < objectives.size(); i++) {
				leveledObjectives[0][i] = objectives.get(i);
			}
			return leveledObjectives;
		}

		Objective[] objectivesArray = getSortedByLevelObjectives(objectives);

		int numberOfLevels = getNumberOfObjectiveLevels(objectivesArray);

		leveledObjectives = new Objective[numberOfLevels][];

		fillLeveledObjectives(objectivesArray);

		return leveledObjectives;
	}

	private void fillLeveledObjectives(Objective[] objectivesArray) {
		int actLevel = objectivesArray[0].getLevel();
		int levelIndex = 0;
		int lastIndex = 0;
		int corrigationForLastLevel = 0;
		boolean oneObjectiveAtLastLevel = false;
		for (int i = 0; i < objectivesArray.length; i++) {
			if (i == objectivesArray.length - 1) {
				corrigationForLastLevel = 1;
				if (objectivesArray[i - 1].getLevel() != objectivesArray[i].getLevel()) {
					oneObjectiveAtLastLevel = true;
					corrigationForLastLevel = 0;
				}
			}
			if (objectivesArray[i].getLevel() != actLevel || corrigationForLastLevel == 1 || oneObjectiveAtLastLevel) {
				leveledObjectives[levelIndex] = new Objective[i - lastIndex + corrigationForLastLevel];
				for (int j = lastIndex; j < i + corrigationForLastLevel; j++) {
					leveledObjectives[levelIndex][j - lastIndex] = objectivesArray[j];
				}
				if (oneObjectiveAtLastLevel) {
					leveledObjectives[levelIndex + 1] = new Objective[1];
					leveledObjectives[levelIndex + 1][0] = objectivesArray[i];
				}
				actLevel = objectivesArray[i].getLevel();
				levelIndex++;
				lastIndex = i;
			}
		}
	}

	private int getNumberOfObjectiveLevels(Objective[] objectivesArray) {

		int actLevel = objectivesArray[0].getLevel();
		int numberOfLevels = 1;

		for (int i = 1; i < objectivesArray.length; i++) {
			if (objectivesArray[i].getLevel() != actLevel) {
				numberOfLevels++;
				actLevel = objectivesArray[i].getLevel();
			}
		}

		return numberOfLevels;
	}

	private Objective[] getSortedByLevelObjectives(List<Objective> objectives) {
		Objective[] objectivesArray = objectives.toArray(new Objective[objectives.size()]);
		Arrays.sort(objectivesArray, Comparator.comparingInt(Objective::getLevel));
		return objectivesArray;
	}

	public Objective[][] getLeveledObjectives() {
		return leveledObjectives;
	}

}
