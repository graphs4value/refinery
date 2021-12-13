package tools.refinery.language.mapping.tests

import com.google.inject.Inject
import java.util.ArrayList
import java.util.List
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.testing.util.ParseHelper
import tools.refinery.language.mapping.PartialModelMapperDTO
import tools.refinery.language.mapping.QueryableModelMapper
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.tests.ProblemInjectorProvider
import tools.refinery.store.model.Tuple
import tools.refinery.store.model.representation.TruthValue
import tools.refinery.store.query.QueriableModel
import java.util.Random

class CaseStudy {
	@Inject
	ParseHelper<Problem> parseHelper

	def static void main(String[] args) {
		val injector = new ProblemInjectorProvider
		val caseStudy = new CaseStudy
		injector.injector.injectMembers(caseStudy)
		measurement(caseStudy)
	}
	
	def static void measurement(CaseStudy caseStudy) {
		val partialModelMapperDTO = caseStudy.generate(1, 4, 3)
		val List<Double> measurementResults = new ArrayList(12)
		
		for (var i = 0; i < 12; i++) {
			System.gc()
			System.gc()
			System.gc()
			Thread.sleep(2000)
			val startTime = System.nanoTime()
			caseStudy.runLaneChangeOrFollowLane(partialModelMapperDTO, 100_000)
			val endTime = System.nanoTime()
			val elapsedTime = endTime - startTime
			val seconds = elapsedTime as double / 1_000_000_000.0
			measurementResults.add(seconds)
		}
		for (seconds : measurementResults) {
			println(seconds)
		}
	}
	
	protected def void countResults(PartialModelMapperDTO partialModelMapperDTO) {
		val model = partialModelMapperDTO.getModel as QueriableModel
		val hasLeft = model.predicates.findFirst[name == "hasLeft"]
		val hasRight = model.predicates.findFirst[name == "hasRight"]
		val leftOrFollowing = model.predicates.findFirst[name == "leftOrFollowing"]
		val rightOrFollowing = model.predicates.findFirst[name == "rightOrFollowing"]
		val twoLanesClose = model.predicates.findFirst[name == "twoLanesClose"]
		val twoCarsClose = model.predicates.findFirst[name == "twoCarsClose"]
		val pred = model.predicates.findFirst[name == "carOnLaneWithPossibleNextLanes"]
		println(("hasLeft: %d, hasRight: %d, leftOrFollowing: %d, rightOrFollowing: %d, pred: %d," +
				"twoLanesClose: %d, twoCarsClose: %d")
			.formatted(model.countResults(hasLeft), model.countResults(hasRight), model.countResults(leftOrFollowing),
				model.countResults(rightOrFollowing), model.countResults(pred), model.countResults(twoLanesClose),
				model.countResults(twoCarsClose)
			)
		)
	}

	protected def void printCarAndLanes(List<int[]> cars_lane1s_lane2s) {
		for (int[] car_lane1_lane2 : cars_lane1s_lane2s) {
			println("car:%3d | l1:%3d | l2:%3d".formatted(car_lane1_lane2.get(0), car_lane1_lane2.get(1),
				car_lane1_lane2.get(2)))
		}
		println("-------------------------")
	}

	def PartialModelMapperDTO generateCyclicRoadScenarioFromText(int numberOfCars, int numberOfLanes,
		int numberOfParallelLanes) throws Exception {
		var problemText = new StringBuilder
		problemText.append('''
			class Lane {
				Lane[0..*] following
				Lane[0..1] left opposite right
				Lane[0..1] right opposite left
			}
			class Car {
				Lane[0..1] placedOn
			}
			direct pred carOnLaneWithItsFollowingLane(car, currLane, followingLane) <->
				Car(car) = true,
				Lane(currLane) = true,
				Lane(followingLane) = true,
				placedOn(car, currLane) = true,
				following(currLane, followingLane) = true.
				
			direct pred hasLeft(lane) <->
				left(lane, leftLane) = true.
				
			direct pred hasRight(lane) <->
				right(lane, rightLane) = true.
				
			direct pred leftOrFollowing(currLane, followingLane, lane) <->
				Lane(followingLane) = true,
				left(currLane, leftLane) = true,
				equals(lane, leftLane) = true
			 ;	Lane(currLane) = true,
				!hasLeft(currLane),
				equals(lane, followingLane) = true.
				
			direct pred rightOrFollowing(currLane, followingLane, lane) <->
				Lane(followingLane) = true,
				right(currLane, rightLane) = true,
				equals(lane, rightLane) = true
			 ;	Lane(currLane) = true,
				!hasRight(currLane),
				equals(lane, followingLane) = true.
				
			direct pred carOnLaneWithPossibleNextLanes(car, currLane, followingLane, lane1, lane2) <->
				Car(car) = true,
				Lane(currLane) = true,
				Lane(followingLane) = true,
				Lane(lane1) = true,
				Lane(lane2) = true,
				placedOn(car, currLane) = true,
				following(currLane, followingLane) = true,
				leftOrFollowing(currLane, followingLane, lane1),
				rightOrFollowing(currLane, followingLane, lane2).
				
			direct pred twoLanesClose(lane1, lane2) <->
				following(lane1, lane2) = true
			 ;  following(lane2, lane1) = true
			 ;  left(lane1, lane2) = true
			 ;  right(lane1, lane2) = true
			 ;	equals(lane1, lane2) = true.
				
			direct pred twoCarsClose(car1, car2) <->
				Car(car1) = true, Car(car2) = true, Lane(lane1) = true, Lane(lane2) = true,
				equals(car1, car2) = false, placedOn(car1, lane1) = true, placedOn(car2, lane2) = true,
				twoLanesClose(lane1, lane2).
			''')
		for (var i = 0; i < numberOfCars; i++) {
			problemText.append(String.format("Car(car%d).\n", i))
		}
		for (var i = 0; i < numberOfLanes * numberOfParallelLanes; i++) {
			problemText.append(String.format("Lane(l%d).\n", i))
		}
		for (var i = 1; i < numberOfLanes; i++) {
			for (var j = 0; j < numberOfParallelLanes; j++) {
				problemText.append(
					String.format("following(l%d, l%d).\n", (i - 1) * numberOfParallelLanes + j,
						i * numberOfParallelLanes + j))
			}
		}
		for (var j = 0; j < numberOfParallelLanes; j++) {
			problemText.append(
				String.format("following(l%d, l%d).\n", numberOfParallelLanes * (numberOfLanes - 1) + j, j)
			)
		}
		for (var i = 0; i < numberOfLanes; i++) {
			for (var j = 1; j < numberOfParallelLanes; j++) {
				problemText.append(
					String.format("left(l%d, l%d).\n", i * numberOfParallelLanes + j - 1,
						i * numberOfParallelLanes + j))
				problemText.append(
					String.format("right(l%d, l%d).\n", i * numberOfParallelLanes + j,
						i * numberOfParallelLanes + j - 1))
			}
		}
		for (var i = 0; i < numberOfCars; i++) {
			problemText.append(String.format("placedOn(car%d, l0).\n", i))
		}

		val problem = parseHelper.parse(problemText)
		EcoreUtil.resolveAll(problem)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(problem)
		return partialModelMapperDTO
	}
	
	def PartialModelMapperDTO generate(int numberOfCars, int lengthOfCycle, int widthOfCycle) throws Exception {
		var problemText = new StringBuilder
		problemText.append('''
			class Lane {
				Lane[0..*] following
				Lane[0..1] left opposite right
				Lane[0..1] right opposite left
			}
			class Car {
				Lane[0..1] placedOn
			}
			direct pred hasLeft(lane) <->
				left(lane, leftLane) = true.
				
			direct pred hasRight(lane) <->
				right(lane, rightLane) = true.
				
			direct pred leftOrFollowing(currLane, followingLane, lane) <->
				following(currLane, followingLane) = true,
				left(currLane, leftLane) = true,
				equals(lane, leftLane) = true
			 ;	following(currLane, followingLane) = true,
				!hasLeft(currLane),
				equals(lane, followingLane) = true.
				
			direct pred rightOrFollowing(currLane, followingLane, lane) <->
				following(currLane, followingLane) = true,
				right(currLane, rightLane) = true,
				equals(lane, rightLane) = true
			 ;	following(currLane, followingLane) = true,
				!hasRight(currLane),
				equals(lane, followingLane) = true.
				
			direct pred carOnLaneWithPossibleNextLanes(car, currLane, followingLane, lane1, lane2) <->
				Car(car) = true,
				Lane(currLane) = true,
				Lane(followingLane) = true,
				Lane(lane1) = true,
				Lane(lane2) = true,
				placedOn(car, currLane) = true,
				following(currLane, followingLane) = true,
				leftOrFollowing(currLane, followingLane, lane1),
				rightOrFollowing(currLane, followingLane, lane2).
				
			direct pred twoLanesClose(lane1, lane2) <->
				following(lane1, lane2) = true
			 ;  following(lane2, lane1) = true
			 ;  left(lane1, lane2) = true
			 ;  right(lane1, lane2) = true
			 ;	equals(lane1, lane2) = true.
				
			direct pred twoCarsClose(car1, car2) <->
				Car(car1) = true, Car(car2) = true, Lane(lane1) = true, Lane(lane2) = true,
				equals(car1, car2) = false, placedOn(car1, lane1) = true, placedOn(car2, lane2) = true,
				twoLanesClose(lane1, lane2).
			''')
		
		val problem = parseHelper.parse(problemText)
		EcoreUtil.resolveAll(problem)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(problem)
		
		val model = partialModelMapperDTO.getModel as QueriableModel
		val firstId = mapper.prepareNodes(partialModelMapperDTO).values.max + 1
		val exists = partialModelMapperDTO.relationMap.values.findFirst[name == "exists"]
		val equals = partialModelMapperDTO.relationMap.values.findFirst[name == "equals"]
		val car = partialModelMapperDTO.relationMap.values.findFirst[name == "Car"]
		val lane = partialModelMapperDTO.relationMap.values.findFirst[name == "Lane"]
		val following = partialModelMapperDTO.relationMap.values.findFirst[name == "following"]
		val left = partialModelMapperDTO.relationMap.values.findFirst[name == "left"]
		val right = partialModelMapperDTO.relationMap.values.findFirst[name == "right"]
		val placedOn = partialModelMapperDTO.relationMap.values.findFirst[name == "placedOn"]
		val totalLanes = lengthOfCycle * widthOfCycle
		
		for (var i = firstId; i < firstId + lengthOfCycle * widthOfCycle; i++) {
			model.put(lane, Tuple.of(i), TruthValue.TRUE)
			model.put(exists, Tuple.of(i), TruthValue.TRUE)
			model.put(equals, Tuple.of(i, i), TruthValue.TRUE)
		}
		for (var i = 1; i < lengthOfCycle; i++) {
			for (var j = 0; j < widthOfCycle; j++) {
				model.put(following, Tuple.of(firstId + (i - 1) * widthOfCycle + j, firstId + i * widthOfCycle + j),
					TruthValue.TRUE
				)
			}
		}
		for (var j = 0; j < widthOfCycle; j++) {
			model.put(following, Tuple.of(firstId + totalLanes - widthOfCycle + j, firstId + j), TruthValue.TRUE)
		}
		for (var i = 0; i < lengthOfCycle; i++) {
			for (var j = 1; j < widthOfCycle; j++) {
				model.put(left, Tuple.of(firstId + i * widthOfCycle + j - 1, firstId + i * widthOfCycle + j),
					TruthValue.TRUE
				)
				model.put(right, Tuple.of(firstId + i * widthOfCycle + j, firstId + i * widthOfCycle + j - 1),
					TruthValue.TRUE
				)
			}
		}
		for (var i = firstId + totalLanes; i < firstId + totalLanes + numberOfCars; i++) {
			model.put(car, Tuple.of(i), TruthValue.TRUE)
			model.put(exists, Tuple.of(i), TruthValue.TRUE)
			model.put(equals, Tuple.of(i, i), TruthValue.TRUE)
		}
		for (var i = 0; i < numberOfCars; i++) {
			model.put(placedOn, Tuple.of(
				firstId + totalLanes + i,
				firstId + ((3 * i) % lengthOfCycle) * widthOfCycle + widthOfCycle / 2
			), TruthValue.TRUE)
		}
		model.flushChanges
		println("model.flushChanges")
		return partialModelMapperDTO
	}

	def void runWithOneChangeForEachRound(PartialModelMapperDTO partialModelMapperDTO, int rounds) {
		val model = partialModelMapperDTO.getModel as QueriableModel
		val pred = model.predicates.findFirst [
			name == "carOnLaneWithItsFollowingLane"
		]
		val placedOn = partialModelMapperDTO.relationMap.values.findFirst[name == "placedOn"]
		var match = model.oneResult(pred).get
		var car = (match.get(0) as Tuple).get(0)
		var l1 = (match.get(1) as Tuple).get(0)
		var l2 = (match.get(2) as Tuple).get(0)
		printCarAndLanes(List.of(#[car, l1, l2]))

		for (var i = 0; i < rounds; i++) {
			model.put(placedOn, Tuple.of(car, l1), TruthValue.FALSE)
			model.put(placedOn, Tuple.of(car, l2), TruthValue.TRUE)
			model.flushChanges

			match = model.oneResult(pred).get
			car = (match.get(0) as Tuple).get(0)
			l1 = (match.get(1) as Tuple).get(0)
			l2 = (match.get(2) as Tuple).get(0)
			printCarAndLanes(List.of(#[car, l1, l2]))
		}
	}

	def void runLaneChangeOrFollowLane(PartialModelMapperDTO partialModelMapperDTO, int rounds) {
		val model = partialModelMapperDTO.getModel as QueriableModel
		val pred = model.predicates.findFirst[name == "carOnLaneWithPossibleNextLanes"]
		val placedOn = partialModelMapperDTO.relationMap.values.findFirst[name == "placedOn"]

		var List<int[]> car_lane1_lane2s = new ArrayList(4096)
		var Random rand = new Random

		for (var i = 0; i < rounds; i++) {
			car_lane1_lane2s.clear
			val results = model.allResults(pred)
			for (var iterator = results.iterator; iterator.hasNext;) {
				var match = iterator.next
				val car = (match.get(0) as Tuple).get(0)
				val currLane = (match.get(1) as Tuple).get(0)
				val followingLane = (match.get(2) as Tuple).get(0)
				val lane1 = (match.get(3) as Tuple).get(0)
				val lane2 = (match.get(4) as Tuple).get(0)
				val chosenLane = #[followingLane, lane1, lane2].get(rand.nextInt(3))
				car_lane1_lane2s.add(#[car, currLane, chosenLane])
			}
			//printCarAndLanes(car_lane1_lane2s)
			
			for (int[] car_lane1_lane2 : car_lane1_lane2s) {
				val car = car_lane1_lane2.get(0)
				val l1 = car_lane1_lane2.get(1)
				val l2 = car_lane1_lane2.get(2)
				model.put(placedOn, Tuple.of(car, l1), TruthValue.FALSE)
				model.put(placedOn, Tuple.of(car, l2), TruthValue.TRUE)
				model.flushChanges
			}
		}
	}
	
	def List<int[]> runCarsWereClose(PartialModelMapperDTO partialModelMapperDTO, int rounds) {
		val model = partialModelMapperDTO.getModel as QueriableModel
		val pred = model.predicates.findFirst[name == "carOnLaneWithPossibleNextLanes"]
		val twoCarsClose = model.predicates.findFirst[name == "twoCarsClose"]
		val placedOn = partialModelMapperDTO.relationMap.values.findFirst[name == "placedOn"]
		
		var List<int[]> car_lane1_lane2s = new ArrayList(4096)
		var List<int[]> round_car1_car2s = new ArrayList(rounds)
		var Random rand = new Random
		
		var carsCloseResults = model.allResults(twoCarsClose)
		for (var iterator = carsCloseResults.iterator; iterator.hasNext;) {
			val match = iterator.next
			val round_car1_car2 = #[0, (match.get(0) as Tuple).get(0), (match.get(1) as Tuple).get(0)]
			round_car1_car2s.add(round_car1_car2)
		}

		for (var i = 0; i < rounds; i++) {
			car_lane1_lane2s.clear
			val results = model.allResults(pred)
			for (var iterator = results.iterator; iterator.hasNext;) {
				var match = iterator.next
				val car = (match.get(0) as Tuple).get(0)
				val currLane = (match.get(1) as Tuple).get(0)
				val followingLane = (match.get(2) as Tuple).get(0)
				val lane1 = (match.get(3) as Tuple).get(0)
				val lane2 = (match.get(4) as Tuple).get(0)
				val chosenLane = #[followingLane, lane1, lane2].get(rand.nextInt(3))
				car_lane1_lane2s.add(#[car, currLane, chosenLane])
			}
			//printCarAndLanes(car_lane1_lane2s)
			
			for (int[] car_lane1_lane2 : car_lane1_lane2s) {
				val car = car_lane1_lane2.get(0)
				val l1 = car_lane1_lane2.get(1)
				val l2 = car_lane1_lane2.get(2)
				model.put(placedOn, Tuple.of(car, l1), TruthValue.FALSE)
				model.put(placedOn, Tuple.of(car, l2), TruthValue.TRUE)
			}
			model.flushChanges
			
			carsCloseResults = model.allResults(twoCarsClose)
			for (var iterator = carsCloseResults.iterator; iterator.hasNext;) {
				val match = iterator.next
				val round_car1_car2 = #[i, (match.get(0) as Tuple).get(0), (match.get(1) as Tuple).get(0)]
				round_car1_car2s.add(round_car1_car2)
			}
		}
		return round_car1_car2s;
	}
}
