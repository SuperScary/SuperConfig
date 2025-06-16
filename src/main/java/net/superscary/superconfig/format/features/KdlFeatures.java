package net.superscary.superconfig.format.features;

public class KdlFeatures {
    private final boolean allowComments;
    private final boolean allowTypeAnnotations;
    private final boolean allowRawStrings;
    private final boolean allowMultiLineStrings;
    private final boolean allowSlashdashComments;

    public KdlFeatures() {
        this(true, true, true, true, true);
    }

    public KdlFeatures(boolean allowComments, boolean allowTypeAnnotations, boolean allowRawStrings,
                      boolean allowMultiLineStrings, boolean allowSlashdashComments) {
        this.allowComments = allowComments;
        this.allowTypeAnnotations = allowTypeAnnotations;
        this.allowRawStrings = allowRawStrings;
        this.allowMultiLineStrings = allowMultiLineStrings;
        this.allowSlashdashComments = allowSlashdashComments;
    }

    public boolean allowComments() {
        return allowComments;
    }

    public boolean allowTypeAnnotations() {
        return allowTypeAnnotations;
    }

    public boolean allowRawStrings() {
        return allowRawStrings;
    }

    public boolean allowMultiLineStrings() {
        return allowMultiLineStrings;
    }

    public boolean allowSlashdashComments() {
        return allowSlashdashComments;
    }
} 