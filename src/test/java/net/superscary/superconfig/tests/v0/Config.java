package net.superscary.superconfig.tests.v0;

import net.superscary.superconfig.annotations.Comment;

@net.superscary.superconfig.annotations.Config(name = "hotswap")
public class Config {

	@Comment({
			"###################################",
			"# Basic configuration for HotSwap #",
			"###################################"})
	public BasicSettings BASIC = new BasicSettings();

	@Comment({
			"####################################",
			"# Action configuration for HotSwap #",
			"####################################"})
	public ActionSettings ACTIONS = new ActionSettings();

	@Comment({
			"####################################",
			"# Armor configuration for HotSwap #",
			"####################################"})
	public ArmorSettings ARMOR = new ArmorSettings();

	@Comment({
			"###################################",
			"# Debug configuration for HotSwap #",
			"###################################"})
	public DebugSettings DEBUG = new DebugSettings();

}
