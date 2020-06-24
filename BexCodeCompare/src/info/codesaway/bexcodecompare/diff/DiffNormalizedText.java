package info.codesaway.bexcodecompare.diff;

import com.google.common.base.Objects;

public class DiffNormalizedText {
    private final String text1;
    private final String text2;

    public DiffNormalizedText(final String text1, final String text2) {
        this.text1 = text1;
        this.text2 = text2;
    }

    public String getText1() {
        return this.text1;
    }

    public String getText2() {
        return this.text2;
    }

    /**
     * Indicates if the two text values in this normalized text are equal
     *
     * @return
     * @since
     * <pre> Change History
     * ========================================================================================
     * Version  Change #        Developer           Date        Description
     * =======  =============== =================== ==========  ===============================
     * TRS.01T                  Amy Brennan-Luna    01/05/2019  Initial code
     *</pre>***********************************************************************************
     */
    public boolean hasEqualText() {
        return Objects.equal(this.text1, this.text2);
    }
}
