package net.superscary.superconfig.tests;

import net.superscary.superconfig.format.formats.*;
import net.superscary.superconfig.manager.ConfigManager;
import net.superscary.superconfig.tests.v0.Config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException, IllegalAccessException {
		/*Path cfgPath = Paths.get("config.json5");
		ConfigManager<SuperConfig> mgr = new ConfigManager<>(SuperConfig.class, cfgPath);

		SuperConfig cfg = mgr.load();
		// … use cfg.verbose.get(), cfg.timeout.get(), cfg.db.url.get(), …

		// modify and save
		cfg.verbose.set(true);
		mgr.save(cfg);

		System.out.println(mgr.load().timeout.get());
		System.out.println(mgr.load().items.get().getLast());*/
		Path cfgPath = Paths.get("hotswap");
		ConfigManager<Config> mgr = new ConfigManager<>(Config.class, cfgPath, new XmlFormat());
		Config cfg = mgr.load();
		mgr.save(cfg);

		System.out.println(cfg.DEBUG.DEBUG_LOGGING.get());
	}

}
