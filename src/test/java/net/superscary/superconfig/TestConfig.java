package net.superscary.superconfig;

import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.wrappers.BooleanValue;
import net.superscary.superconfig.value.wrappers.StringValue;

@Config(name = "test_config")
public class TestConfig {
	public StringValue testString = new StringValue("default");
	public BooleanValue testBoolean = new BooleanValue(true);

	public TestConfig () {
		// Default constructor required for instantiation
	}
}
