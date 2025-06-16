package net.superscary.superconfig.v0;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.format.ConfigFormatType;

@net.superscary.superconfig.annotations.Config(path = "configs", format = ConfigFormatType.KDL)
public class Config {
    @Comment("This is a test string")
    public String testString = "test";
    @Comment("This is a test boolean")
    public boolean testBoolean = true;

    @Comment("This is a test inner config")
    public static class InnerConfig {
        @Comment("This is a test string")
        public String testString = "test";
        @Comment("This is a test boolean")
        public boolean testBoolean = true;
    }

    @Comment("This is a test inner config2")
    public InnerConfig2 innerConfig2 = new InnerConfig2();

    public class InnerConfig2 {
        @Comment("This is a test string")
        public String testString = "test";
        @Comment("This is a test boolean")
        public boolean testBoolean = true;
    }
}
