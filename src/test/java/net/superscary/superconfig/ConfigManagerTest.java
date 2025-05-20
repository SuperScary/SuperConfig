package net.superscary.superconfig;

import net.superscary.superconfig.factory.ConfigFactory;
import net.superscary.superconfig.format.formats.Json5Format;
import net.superscary.superconfig.format.formats.JsonFormat;
import net.superscary.superconfig.manager.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigManagerTest {

    private ConfigManager<TestConfig> configManager;
    private TestConfig testConfig;
    private Path configFile;

    @TempDir
    Path tempDir = Paths.get("");

    @BeforeEach
    void setUp() throws IOException, IllegalAccessException {
        // Create the config directory if it doesn't exist
        Files.createDirectories(tempDir);
        
        // Set up the config file path
        configFile = tempDir.resolve("testconfig.json5");
        
        // Create the ConfigManager
        testConfig = ConfigFactory.load(TestConfig.class, configFile);
    }

    @Test
    void testLoadAndSaveConfig() {
        // Verify the file was created
        assertTrue(Files.exists(configFile), "Config file should exist after save");
        
        // Verify values
        assertNotNull(testConfig, "Loaded config should not be null");
        assertEquals("default", testConfig.testString.get(), "String value should match");
        assertTrue(testConfig.testBoolean.get(), "Boolean value should be true");
    }

    @Test
    void testSaveConfigWithInvalidPath() {
        // Create a ConfigManager with an invalid path
        Path invalidPath = tempDir.resolve("nonexistent/dir/config.json");
        ConfigManager<TestConfig> invalidManager = ConfigManager.<TestConfig>builder(TestConfig.class)
                .file(invalidPath)
                .format(new JsonFormat())
                .build();
        
        // Attempting to save should throw IOException
        assertThrows(IOException.class, () -> invalidManager.save(testConfig),
            "Saving to invalid path should throw IOException");
    }
} 