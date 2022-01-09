package info.codesaway.becr.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class CodeInfoWithSourceInfoTest {
	// Test issue #122
	@Test
	public void callConstructorNullPackageNameYieldsBlankPackageName() {
		CodeInfoWithSourceInfo codeInfoWithSourceInfo = new CodeInfoWithSourceInfo("", null, "", null, "", "");
		assertThat(codeInfoWithSourceInfo.getPackageName()).isBlank();
	}

	@Test
	public void callConstructorNullProjectNullPointException() {
		assertThatThrownBy(() -> new CodeInfoWithSourceInfo(null, "", "", null, "", ""))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("project is null");
	}

	@Test
	public void callConstructorNullJavaFilenameWithoutExtensionNullPointException() {
		assertThatThrownBy(() -> new CodeInfoWithSourceInfo("", "", null, null, "", ""))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("javaFilenameWithoutExtension is null");
	}

	@Test
	public void callConstructorNullInfoNullPointException() {
		assertThatThrownBy(() -> new CodeInfoWithSourceInfo("", "", "", null, null, ""))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("info is null");
	}

	@Test
	public void callConstructorNullSourcePathnameNullPointException() {
		assertThatThrownBy(() -> new CodeInfoWithSourceInfo("", "", "", null, "", null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("sourcePathname is null");
	}
}
