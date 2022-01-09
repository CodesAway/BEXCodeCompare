package info.codesaway.bex.diff;

import static info.codesaway.bex.diff.NormalizationFunction.indexedNormalization;
import static info.codesaway.bex.diff.NormalizationFunction.normalization;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BEXNormalizationFunctionTest {

	// TODO adding either of these tests causes other tests to fail due to NullPointerException on NormalizationFunciton.NO_NORMALIZATION

	@Test
	void testNormalizationMethodNullParameter() {
		NormalizationFunction normalization = normalization(null);
		assertThat(normalization).isNotNull();
	}

	@Test
	void testIndexedNormalizationMethodNullParameter() {
		NormalizationFunction normalization = indexedNormalization(null);
		assertThat(normalization).isNotNull();
	}
}
