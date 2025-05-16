package net.superscary.superconfig.tests.v0;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.wrappers.BooleanValue;
import net.superscary.superconfig.value.wrappers.EnumValue;
import net.superscary.superconfig.value.wrappers.ListValue;

import java.util.List;

@Config(name = "ArmorSettings")
public class ArmorSettings {

	@Comment("Enable armor swapping.")
	public BooleanValue ENABLED = new BooleanValue(true);

	@Comment({"Enable swapping to elytra.",
			"This behavior is only valid if an elytra is worn and the player has an elytra in their inventory."})
	public BooleanValue ALLOW_ELYTRA = new BooleanValue(true);

	@Comment({"Swap armor when it breaks/low durability.",
			"If set to LOW_DURABILITY, it will swap to the best first match with a higher durability.",
			"Possible values: \"BREAK\" | \"LOW_DURABILITY\""})
	public EnumValue<ArmorSwap> ARMOR_SWAP_BEHAVIOR = new EnumValue<>(ArmorSwap.BREAK);

	@Comment({"Which tag groups to consider when swapping armor.",
			"Order does not matter."})
	public ListValue<String> TAGS = new ListValue<>(List.of(
			"#minecraft:armor",
			"#minecraft:chestplates",
			"#minecraft:leggings",
			"#minecraft:boots",
			"#minecraft:helmets",
			"#minecraft:elytra"
	));

	@Comment("Any item IDs to blacklist even if their tags match. IE: \"minecraft:diamond_leggings\" will never be swapped to if enabled in this list.")
	public ListValue<String> BLACKLIST = new ListValue<>(List.of());
}