# SuperConfig

![GitHub License](https://img.shields.io/github/license/SuperScary/SuperConfig?style=for-the-badge)
![Maven metadata](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fnet%2Fsuperscary%2FSuperConfig%2Fmaven-metadata.xml&style=for-the-badge)
[![GitHub Sponsors](https://img.shields.io/github/sponsors/SuperScary?style=for-the-badge)](https://github.com/sponsors/SuperScary)

> A **drop-in**, **class-based** configuration manager for Java — JSON, JSON5, YAML, TOML, and XML.

SuperConfig lets you define your app’s settings as plain Java classes—no XML schemas, no boilerplate, no hand-rolled parsers.  
Annotate with `@Config`, use `@Comment` for inline docs, `@Ignore` to skip fields, or value–wrappers like `CharValue`.  
Under the hood each format ships its own Jackson mapper, so you get human-friendly files and zero surprises.

---

## Table of Contents

- [Features](#features)  
- [Getting Started](#getting-started)  
  - [Prerequisites](#prerequisites)  
  - [Installation](#installation)  
- [Usage](#usage)  
  - [Define a Config Class](#define-a-config-class)  
  - [Load & Save (Multi-Format)](#load--save-multi-format)  
- [Advanced](#advanced)  
  - [Default Values & Comments](#default-values--comments)  
  - [Nested Containers](#nested-containers)  
  - [Ignoring Fields](#ignoring-fields)  
  - [Value Wrappers](#value-wrappers)  
- [API Reference](#api-reference)  
- [Contributing](#contributing)  
- [License](#license)  

---

## Features

- **Annotation-driven** – `@Config`, `@Comment`, `@Ignore`  
- **Multi-format** – JSON, JSON5, YAML, TOML, and XML 
- **Nested containers** – group related settings in sub-classes  
- **Value wrappers** – `ConfigValue<T>`, `ListValue<T>`, `CharValue`, enums  
- **Zero boilerplate** – defaults from field initializers  
- **Human-friendly** – inline comments, pretty-printed output  
- **Case-insensitive & forgiving** – unknown props ignored, enums & keys match ignoring case  

---

## Getting Started

### Prerequisites

- Java 8+  
- Jackson Core & Databind (for JSON, YAML, TOML, XML, & KDL)  

### Installation

**Gradle**  
```groovy
repositories { mavenCentral() }
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

```java
import net.superscary.superconfig.annotations.*;
import net.superscary.superconfig.value.wrappers.BooleanValue;
import net.superscary.superconfig.value.wrappers.IntegerValue;
import net.superscary.superconfig.format.ConfigFormatType;

@Config(value = "server_config", path = "configs", format = ConfigFormatType.KDL)
public class ServerConfig {
  @Comment("Server port")
  public IntegerValue port = new IntegerValue(8080);

  @Comment("Enable verbose logging")
  public BooleanValue verbose = new BooleanValue(false);

  @Ignore
  public String internalSecret = "do-not-serialize";

  @Comment("Activation key")
  public CharValue activationKey = new CharValue('G');
}
```

### Load & Save (Multi-Format)

```java
import net.superscary.superconfig.manager.ConfigManager;
import net.superscary.superconfig.format.formats.*;

public class Main {
    public static void main(String[] args) throws Exception {
      var factory = new ConfigFactory();
      var config = factory.load( Config.class);
    }
}
```

---

## Advanced

### Default Values & Comments

* Field initializers become defaults on first load.
* Use `@Comment("…")` to embed notes above each entry.

### Nested Containers

```java
import net.superscary.superconfig.format.ConfigFormatType;

@Config(value = "server_config", path = "configs", format = ConfigFormatType.KDL)
public class AppConfig {
    @Config(name = "database")
    public static class Db {
        @Comment("JDBC URL")     
        public String url  = "jdbc:h2:mem:test";
        @Comment("DB user")      
        public String user = "sa";
    }
    @Comment("Database settings")
    public Db database = new Db();
}
```

### Ignoring Fields

Mark any field with `@Ignore` to **skip** loading and saving:

```java
@Ignore 
public transient Path tempDir;
```

### Value Wrappers

* **Primitive**: `ConfigValue<T>`
* **List**: `ListValue<T>`
* **Char**: `CharValue`
* **Enums**: `EnumValue<E>`

---

## API Reference

### `ConfigManager<T>`

```java
public final class ConfigManager<T> {
  public static <T> Builder<T> builder(Class<T> type);
  
  public T load() throws IOException, IllegalAccessException;
  public void save(T config) throws IOException, IllegalAccessException;

  public static final class Builder<T> {
    public Builder<T> file(Path file);
    public Builder<T> format(ConfigFormat fmt);    // default = JSON
    public ConfigManager<T> build();
  }
}
```

---

## Contributing

1. Fork the repo
2. Create a branch (`git checkout -b feat/YourFeature`)
3. Commit your changes (`git commit -m "Add feature"`)
4. Push & open a PR

Please follow code style and add tests for new features.

---

## License

This project is MIT-licensed. See the [LICENSE](LICENSE) file for details.

[![Built with ♥](https://forthebadge.com/images/featured/featured-built-with-love.svg)](https://github.com/sponsors/SuperScary)

