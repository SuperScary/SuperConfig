package net.superscary.superconfig.tests.v2;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.wrappers.BooleanValue;

@Config(name = "configv2")
public class ConfigV2 {

	@Comment("This is a comment")
	public static BooleanValue ENABLE = new BooleanValue(false);

	public ConfigV2 () {
		// Constructor test (should skip)
	}

	@Comment("Test class load with nested class.")
	public static Test test = new Test();

	@Config(name = "test")
	public static class Test {

		@Comment("This is a comment")
		public static BooleanValue ENABLE = new BooleanValue(false);

	}

}
