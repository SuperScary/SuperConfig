package net.superscary.superconfig.tests.v2;

import net.superscary.superconfig.format.formats.TomlFormat;
import net.superscary.superconfig.format.formats.YamlFormat;
import net.superscary.superconfig.manager.ConfigManager;

import java.io.IOException;
import java.nio.file.Paths;

public class MainV2 {

	public static void main (String[] args) throws IOException, IllegalAccessException {
		ConfigManager<ConfigV2> mgr = new ConfigManager<>(ConfigV2.class, Paths.get("configv2"), new YamlFormat());
		ConfigV2 config = mgr.load();
		mgr.save(config);

		System.out.println(config.testChar.get());
	}

}
