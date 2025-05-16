package net.superscary.superconfig.tests.v1;

public enum Transformers {
	OPTIMUS_PRIME ("Optimus Prime"),
	MEGATRON ("Megatron"),
	STARSCREAM ("Starscream"),
	GRIMLOCK ("Grimlock"),
	DEVASTATOR ("Devastator"),
	SHOCKWAVE ("Shockwave"),
	ULTRA_MAGNUS ("Ultra Magnus"),
	IRONHIDE ("Ironhide"),
	BLUESTREAK ("Bluestreak"),
	PROWL ("Prowl"),
	SMOKESCREEN ("Smokescreen"),
	BLASTER ("Blaster"),
	RAIDEN ("Raiden"),
	THUNDERCRACKER ("Thundercracker"),
	SCREECHER ("Screamer"),
	THUNDERBIRD ("Thunderbird");

	private final String name;
	Transformers (String name) {
		this.name = name;
	}

	public String getName () {
		return name;
	}
}
