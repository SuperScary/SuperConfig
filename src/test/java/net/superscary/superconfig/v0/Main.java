package net.superscary.superconfig.v0;

import net.superscary.superconfig.core.ConfigFactory;

public class Main {
    public static void main(String[] args) throws Exception {
        var factory = new ConfigFactory();
        var config = factory.load( Config.class);
        System.out.println(config.testString);
        System.out.println(config.testBoolean);
    }
}
