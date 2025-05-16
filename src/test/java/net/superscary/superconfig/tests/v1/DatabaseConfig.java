package net.superscary.superconfig.tests.v1;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.wrappers.StringValue;

@Config(name = "database")
public class DatabaseConfig {
	@Comment("JDBC URL of the database")
	public StringValue url = new StringValue("jdbc:h2:mem:test");

	@Comment("Username for DB")
	public StringValue user = new StringValue("sa");
}
