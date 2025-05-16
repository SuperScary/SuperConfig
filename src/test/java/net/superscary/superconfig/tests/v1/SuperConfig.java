package net.superscary.superconfig.tests.v1;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.wrappers.BooleanValue;
import net.superscary.superconfig.value.wrappers.EnumValue;
import net.superscary.superconfig.value.wrappers.IntegerValue;
import net.superscary.superconfig.value.wrappers.ListValue;

import java.util.List;

@Config(name = "config")
public class SuperConfig {

	@Comment({"Enable verbose logging","Set to false to silence debug output"})
	public BooleanValue verbose = new BooleanValue(false);

	@Comment("Connection timeout in seconds")
	public IntegerValue timeout = new IntegerValue(30);

	@Comment("database")
	public DatabaseConfig db = new DatabaseConfig();

	@Comment("Transformers")
	public EnumValue<Transformers> transformers = new EnumValue<>(Transformers.class, Transformers.DEVASTATOR);

	@Comment("List of items")
	public ListValue<Integer> items = new ListValue<>(List.of(
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10
	));

}
