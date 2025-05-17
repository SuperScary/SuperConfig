# SuperConfig

![GitHub License](https://img.shields.io/github/license/SuperScary/SuperConfig?style=for-the-badge)
![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fnet%2Fsuperscary%2FSuperConfig%2Fmaven-metadata.xml&style=for-the-badge)
[![GitHub Sponsors](https://img.shields.io/github/sponsors/SuperScary?style=for-the-badge)](https://github.com/sponsors/SuperScary)

SuperConfig is a drop-in, class-based configuration manager for Java projects that lets you define your application’s 
settings as plain Java classes—no XML, no boilerplate, no hand-rolled parsers. Simply annotate your classes with 
`@ConfigContainer` and fields with `@Comment`, and SuperConfig automatically handles JSON serialization, deserialization, 
default values, inline comments, and pretty-printed output. Under the hood it leverages Jackson for fast, reliable I/O, 
and supports nested containers so you can group related settings into logical sub-classes. Whether you’re building a 
small CLI tool or a large enterprise service, SuperConfig gives you a zero-hassle, human-friendly way to manage configuration.

---

## Table of Contents

- [Features](#features)  
- [Getting Started](#getting-started)  
  - [Prerequisites](#prerequisites)  
  - [Installation](#installation)  
- [Usage](#usage)  
  - [Define a Config Class](#define-a-config-class)  
  - [Load & Save Config](#load--save-config)  
- [Advanced](#advanced)  
  - [Default Values](#default-values)  
  - [Comments & Documentation](#comments--documentation)  
  - [Nested Config Containers](#nested-config-containers)  
- [API Reference](#api-reference)  
- [Contributing](#contributing)  
- [License](#license)  

---

## Features

- **Annotation-driven** – mark up your POJOs to become configuration holders  
- **Automatic I/O** – loads defaults on first run and persists any changes  
- **Human-friendly** – supports in-file comments and pretty-printed JSON  
- **Nested containers** – group related settings into sub-classes  
- **Zero dependencies** – only requires Jackson for JSON binding  

---

## Getting Started

### Prerequisites

- Java 8 or higher  
- Jackson Core & Databind on your classpath  

### Installation

**Gradle**  
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'net.superscary:SuperConfig:1.0.0'
}
````

**Maven**

```xml
<dependency>
  <groupId>net.superscary</groupId>
  <artifactId>SuperConfig</artifactId>
  <version>1.0.0</version>
</dependency>
```

---

## Usage

### Define a Config Class

Annotate a plain Java class and its fields:

```java
import net.superscary.superconfig.annotations.ConfigContainer;
import net.superscary.superconfig.annotations.Comment;

@ConfigContainer
public class ServerConfig {
    @Comment("Port the server will bind to")
    public int port = 8080;

    @Comment("Maximum number of simultaneous connections")
    public int maxConnections = 100;

    @Comment("Enable verbose logging")
    public boolean verbose = false;
}
```

### Load & Save Config

Use `ConfigManager` to handle I/O:

```java
import net.superscary.superconfig.manager.ConfigManager;

public class Main {
    public static void main(String[] args) throws IOException {
        // Create manager, pointing to config file and class
        ConfigManager<ServerConfig> cfgMan =
            new ConfigManager<>("server-config.json", ServerConfig.class);

        // Load (creates file with defaults if missing)
        ServerConfig config = cfgMan.load();

        // Read or modify values
        System.out.println("Listening on port " + config.port);
        config.verbose = true;

        // Persist changes back to disk
        cfgMan.save(config);
    }
}
```

---

## Advanced

### Default Values

Fields initialized in your class serve as defaults on first load.

### Comments & Documentation

Use `@Comment("...")` to embed human-readable notes into the generated JSON.

### Nested Config Containers

Group related settings:

```java
@ConfigContainer
public class AppConfig {
    @Comment("Database settings")
    public DatabaseConfig database = new DatabaseConfig();

    @ConfigContainer
    public static class DatabaseConfig {
        @Comment("JDBC URL")
        public String url = "jdbc:h2:mem:test";
        @Comment("User name")
        public String user = "sa";
        @Comment("Password")
        public String pass = "";
    }
}
```

---

## API Reference

### `ConfigManager<T>`

* **Constructor**
  `ConfigManager(String filePath, Class<T> configClass)`
* **`T load()`**
  Load or create the config file.
* **`void save(T config)`**
  Persist the in-memory config back to disk.

---

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b feature/YourFeature`)
3. Commit your changes (`git commit -am 'Add YourFeature'`)
4. Push to the branch (`git push origin feature/YourFeature`)
5. Open a Pull Request

Please follow the existing code style and include unit tests for new functionality.

---

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

[![forthebadge](https://forthebadge.com/images/featured/featured-built-with-love.svg)](https://forthebadge.com)
[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/SuperScary)
