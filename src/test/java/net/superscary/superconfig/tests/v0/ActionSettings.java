package net.superscary.superconfig.tests.v0;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.wrappers.BooleanValue;

@Config(name = "ActionSettings")
public class ActionSettings {

	@Comment("Enable swapping to the next best tool in the inventory when a tool breaks.")
	public BooleanValue SWAP_ON_BREAK = new BooleanValue(true);

	@Comment("Mining Settings")
	public ActionConfig.Mine MINE = new ActionConfig.Mine();

	@Comment("Attacking Settings")
	public ActionConfig.Attack ATTACK = new ActionConfig.Attack();
}
