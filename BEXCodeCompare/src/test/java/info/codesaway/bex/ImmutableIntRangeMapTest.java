package info.codesaway.bex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ImmutableIntRangeMapTest {
	@Test
	void testBuildRangeMap() {
		ImmutableIntRangeMap.Builder<String> builder = ImmutableIntRangeMap.builder();
		builder
				.put(IntBEXRange.of(0, 1), "0")
				.put(IntBEXRange.of(1, 2), "1");

		ImmutableIntRangeMap<String> rangeMap = builder.build();
		assertThat(rangeMap.get(0)).isEqualTo("0");
		assertThat(rangeMap.get(1)).isEqualTo("1");
	}

	@Test
	void testOverlapBuildRangeMap() {
		ImmutableIntRangeMap.Builder<String> builder = ImmutableIntRangeMap.builder();
		builder
				.put(IntBEXRange.of(0, 1), "0")
				.put(IntBEXRange.of(1, 2), "1")
				.put(IntBEXRange.closed(-1, 0), "Overlap!");

		assertThatThrownBy(builder::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Overlapping ranges: range [-1..0] overlaps with entry [0..1)");
	}

	@Test
	void testAddItemsInRandomOrder() {
		ImmutableIntRangeMap.Builder<String> builder = ImmutableIntRangeMap.builder();
		builder
				.put(IntBEXRange.of(0, 1), "0")
				.put(IntBEXRange.of(1, 2), "1")
				.put(IntBEXRange.of(-1, 0), "No overlap!");

		builder.build();
	}

	@Test
	void testGetItem() {
		ImmutableIntRangeMap.Builder<String> builder = ImmutableIntRangeMap.builder();
		builder
				.put(IntBEXRange.of(0, 1), "0")
				.put(IntBEXRange.of(1, 2), "1")
				.put(IntBEXRange.of(-1, 0), "No overlap!");

		ImmutableIntRangeMap<String> rangeMap = builder.build();

		assertThat(rangeMap.get(0)).isEqualTo("0");
	}

	@Test
	void testGetItemWhoseRageContainsTheKey() {
		ImmutableIntRangeMap.Builder<String> builder = ImmutableIntRangeMap.builder();
		builder
				.put(IntBEXRange.of(0, 2), "0")
				.put(IntBEXRange.of(2, 4), "2");

		ImmutableIntRangeMap<String> rangeMap = builder.build();

		assertThat(rangeMap.get(-1)).isNull();
		assertThat(rangeMap.get(0)).isEqualTo("0");
		assertThat(rangeMap.get(1)).isEqualTo("0");
		assertThat(rangeMap.get(2)).isEqualTo("2");
		assertThat(rangeMap.get(3)).isEqualTo("2");
		assertThat(rangeMap.get(4)).isNull();

	}
}
