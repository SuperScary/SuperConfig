package net.superscary.superconfig.tests.v2;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.annotations.Ignore;
import net.superscary.superconfig.value.wrappers.BooleanValue;
import net.superscary.superconfig.value.wrappers.CharValue;

@Config(name = "configv2")
public class ConfigV2 {

	@Comment("This is a comment")
	public BooleanValue ENABLE = new BooleanValue(false);

	public ConfigV2 () {
		// Constructor test (should skip)
	}

	@Comment("CharValue test")
	public CharValue testChar = new CharValue('a');

	@Comment("Ignore this field")
	@Ignore
	public String ignoredField = "This should be ignored";

	@Comment("Test class load with nested class.")
	public Test test = new Test();

	@Config(name = "test")
	public static class Test {

		@Comment("This is a comment")
		public BooleanValue ENABLE = new BooleanValue(false);

	}

}
