package net.superscary.superconfig.tests.v0;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.wrappers.BooleanValue;

@Config(name = "Basic")
public class BasicSettings {
	@Comment("Allow swapping while in creative mode.")
	public BooleanValue ALLOW_IN_CREATIVE = new BooleanValue(false);

	@Comment("Allow swapping while in adventure mode.")
	public BooleanValue ALLOW_IN_ADVENTURE = new BooleanValue(false);

	@Comment("Allow swapping while in survival mode.")
	public BooleanValue ALLOW_IN_SURVIVAL = new BooleanValue(true);

	@Comment("Allow auto swapping for attacking.")
	public BooleanValue ALLOW_FOR_ATTACKING = new BooleanValue(true);

	@Comment("Stay on selected tool.")
	public BooleanValue KEEP_LAST = new BooleanValue(false);

}
