/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreapAggregateTest {
	private TreapAggregate<Integer, Integer> sut;

	@BeforeEach
	void beforeEach() {
		sut = createAggregate();
	}

	private static TreapAggregate<Integer, Integer> createAggregate() {
		var aggregator = TreapAggregator.of(Integer.class, Integer.class, (count, value) -> count * value, 0,
				Integer::sum);
		return (TreapAggregate<Integer, Integer>) aggregator.createEmptyAggregate();
	}

	@Test
	void initialValueTest() {
		assertThat(sut.getResult(), is(0));
	}

	@Test
	void singleValueTest() {
		sut.add(5);
		assertThat(sut.getResult(), is(5));
	}

	@Test
	void repeatedValueTest() {
		sut.add(5);
		sut.add(5);
		sut.add(5);
		assertThat(sut.getResult(), is(15));
	}

	@Test
	void multipleValuesTests() {
		sut.add(1);
		sut.add(2);
		sut.add(3);
		assertThat(sut.getResult(), is(6));
	}

	@Test
	void removalTest() {
		sut.add(3);
		sut.add(5);
		sut.remove(5);
		assertThat(sut.getResult(), is(3));
	}

	@Test
	void removeRepeatedTest() {
		sut.add(3);
		sut.add(5);
		sut.add(5);
		sut.remove(5);
		sut.remove(5);
		assertThat(sut.getResult(), is(3));
	}

	@Test
	void removeToEmptyTest() {
		sut.add(5);
		sut.remove(5);
		assertThat(sut.getResult(), is(0));
	}

	@Test
	void removeInvalidTest() {
		assertThrows(IllegalArgumentException.class, () -> sut.remove(5));
	}

	@Test
	void confluenceTest() {
		int range = 100;
		var numbers = new int[range];
		var random = new Random(1);
		int sum = 0;
		for (int i = 0; i < 10_000; i++) {
			int number = random.nextInt(range);
			int count = numbers[number];
			boolean add = true;
			if (count > 0){
				add = random.nextInt(3) > 0;
			}
			if (add) {
				numbers[number]++;
				sum += number;
                sut.add(number);
            } else {
				numbers[number]--;
				sum -= number;
                sut.remove(number);
            }
			var addOnlyAggregate = createAggregate();
			for (int j = 0; j < range; j++) {
				addOnlyAggregate.add(j, numbers[j]);
            }
			assertThat(sut.getResult(), is(sum));
			assertThat(sut.validate(addOnlyAggregate), is(true));
		}
	}
}
