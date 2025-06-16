package net.superscary.superconfig.format.formats;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.format.ConfigFormatAdapter;
import net.superscary.superconfig.format.ConfigFormatType;
import net.superscary.superconfig.format.features.KdlFeatures;
import net.superscary.superconfig.format.tokenizer.KdlTokenizer;
import net.superscary.superconfig.format.tokens.Token;
import net.superscary.superconfig.format.tokens.TokenType;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Format adapter for KDL configuration files.
 * <p>
 * This adapter handles reading and writing configuration data in KDL format,
 * with support for comments, positional arguments, properties, and nested nodes.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class KdlFormatAdapter implements ConfigFormatAdapter {
    private final KdlFeatures features;

    public KdlFormatAdapter() {
        this.features = new KdlFeatures();
    }

    @Override
    public String extension() {
        return ConfigFormatType.KDL.getFileExtension();
    }

    @Override
    public String lineCommentPrefix() {
        return "//";
    }

    @Override
    public String lineCommentSuffix() {
        return "";
    }

    @Override
    public Map<String, Object> read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            KdlTokenizer tokenizer = new KdlTokenizer(reader);
            List<Token> tokens = tokenizer.tokenize();
            return parseDocument(tokens);
        }
    }

    @Override
    public <T> void write(Path file, T config, Class<T> configClass) throws IOException {
        Map<String, Object> data = convertToMap(config);
        String kdl = generateKdl(data, configClass);
        Files.writeString(file, kdl);
    }

    private Map<String, Object> parseDocument(List<Token> tokens) {
        Map<String, Object> document = new LinkedHashMap<>();
        int i = 0;

        // Handle version marker if present
        if (!tokens.isEmpty() && tokens.get(0).type() == TokenType.VERSION_MARKER) {
            i++;
        }

        while (i < tokens.size()) {
            Token token = tokens.get(i);
            if (token.type() == TokenType.IDENTIFIER || token.type() == TokenType.STRING) {
                String name = token.value();
                i++;

                // Parse type annotation if present and allowed
                String type = null;
                if (features.allowTypeAnnotations() && i < tokens.size() && tokens.get(i).type() == TokenType.TYPE_ANNOTATION) {
                    type = tokens.get(i).value();
                    i++;
                }

                // Parse arguments and properties
                List<Object> arguments = new ArrayList<>();
                Map<String, Object> properties = new LinkedHashMap<>();

                while (i < tokens.size()) {
                    token = tokens.get(i);
                    if (token.type() == TokenType.LEFT_BRACE) {
                        i++;
                        Map<String, Object> children = parseChildren(tokens, i);
                        i = findMatchingBrace(tokens, i);
                        document.put(name, children);
                        break;
                    } else if (token.type() == TokenType.EQUALS) {
                        i++;
                        if (i < tokens.size()) {
                            properties.put(name, parseValue(tokens.get(i)));
                        }
                        i++;
                    } else if (token.type() == TokenType.SEMICOLON || token.type() == TokenType.RIGHT_BRACE) {
                        if (!arguments.isEmpty()) {
                            document.put(name, arguments.size() == 1 ? arguments.get(0) : arguments);
                        } else if (!properties.isEmpty()) {
                            document.put(name, properties);
                        } else {
                            document.put(name, null);
                        }
                        i++;
                        break;
                    } else {
                        arguments.add(parseValue(token));
                        i++;
                    }
                }
            } else if (features.allowComments() && token.type() == TokenType.COMMENT) {
                // Store comments in the document
                String comment = token.value();
                if (!comment.isEmpty()) {
                    document.put("_comment_" + document.size(), comment);
                }
                i++;
            } else {
                i++;
            }
        }

        return document;
    }

    private Map<String, Object> parseChildren(List<Token> tokens, int startIndex) {
        Map<String, Object> children = new LinkedHashMap<>();
        int i = startIndex;

        while (i < tokens.size()) {
            Token token = tokens.get(i);
            if (token.type() == TokenType.RIGHT_BRACE) {
                break;
            } else if (token.type() == TokenType.IDENTIFIER || token.type() == TokenType.STRING) {
                String name = token.value();
                i++;

                // Parse type annotation if present
                String type = null;
                if (i < tokens.size() && tokens.get(i).type() == TokenType.TYPE_ANNOTATION) {
                    type = tokens.get(i).value();
                    i++;
                }

                // Parse arguments and properties
                List<Object> arguments = new ArrayList<>();
                Map<String, Object> properties = new LinkedHashMap<>();

                while (i < tokens.size()) {
                    token = tokens.get(i);
                    if (token.type() == TokenType.LEFT_BRACE) {
                        i++;
                        Map<String, Object> nestedChildren = parseChildren(tokens, i);
                        i = findMatchingBrace(tokens, i);
                        children.put(name, nestedChildren);
                        break;
                    } else if (token.type() == TokenType.EQUALS) {
                        i++;
                        if (i < tokens.size()) {
                            properties.put(name, parseValue(tokens.get(i)));
                        }
                        i++;
                    } else if (token.type() == TokenType.SEMICOLON || token.type() == TokenType.RIGHT_BRACE) {
                        if (!arguments.isEmpty()) {
                            children.put(name, arguments.size() == 1 ? arguments.get(0) : arguments);
                        } else if (!properties.isEmpty()) {
                            children.put(name, properties);
                        } else {
                            children.put(name, null);
                        }
                        i++;
                        break;
                    } else {
                        arguments.add(parseValue(token));
                        i++;
                    }
                }
            } else {
                i++;
            }
        }

        return children;
    }

    private Object parseValue(Token token) {
        return switch (token.type()) {
            case STRING, RAW_STRING -> token.value();
            case NUMBER -> parseNumber(token.value());
            case BOOLEAN -> Boolean.parseBoolean(token.value());
            case NULL -> null;
            default -> throw new IllegalArgumentException("Unexpected token type: " + token.type());
        };
    }

    private Object parseNumber(String value) {
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return Long.parseLong(value.substring(2), 16);
        } else if (value.startsWith("0o") || value.startsWith("0O")) {
            return Long.parseLong(value.substring(2), 8);
        } else if (value.startsWith("0b") || value.startsWith("0B")) {
            return Long.parseLong(value.substring(2), 2);
        } else if (value.equals("#inf")) {
            return Double.POSITIVE_INFINITY;
        } else if (value.equals("#-inf")) {
            return Double.NEGATIVE_INFINITY;
        } else if (value.equals("#nan")) {
            return Double.NaN;
        } else {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return Double.parseDouble(value);
            }
        }
    }

    private int findMatchingBrace(List<Token> tokens, int startIndex) {
        int depth = 1;
        for (int i = startIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type() == TokenType.LEFT_BRACE) {
                depth++;
            } else if (token.type() == TokenType.RIGHT_BRACE) {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        throw new IllegalArgumentException("Unmatched braces");
    }

    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        Class<?> cls = obj.getClass();

        // First, handle static nested classes
        for (Class<?> nestedClass : cls.getDeclaredClasses()) {
            if (java.lang.reflect.Modifier.isStatic(nestedClass.getModifiers())) {
                try {
                    // Create an instance of the static nested class
                    Object nestedInstance = nestedClass.getDeclaredConstructor().newInstance();
                    map.put(nestedClass.getSimpleName(), convertToMap(nestedInstance));
                } catch (ReflectiveOperationException e) {
                    // Skip if we can't instantiate the nested class
                }
            }
        }

        // Then handle instance fields
        for (Field field : cls.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                // Skip synthetic fields (like this$0 for inner classes)
                if (field.isSynthetic()) {
                    continue;
                }
                Object value = field.get(obj);
                if (value != null) {
                    // Skip static fields
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    // Handle nested objects
                    if (value.getClass().getDeclaringClass() != null) {
                        // This is a nested class instance
                        map.put(field.getName(), convertToMap(value));
                    } else {
                        map.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException e) {
                // Skip inaccessible fields
            }
        }
        return map;
    }

    private void generateNode(StringBuilder kdl, Map<String, Object> data, int indent, Class<?> configClass) {
        String indentStr = "    ".repeat(indent);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            // Add field comment if present
            String comment = getFieldComment(entry.getKey(), configClass);
            if (comment != null && !comment.isEmpty()) {
                kdl.append(indentStr).append("// ").append(comment).append("\n");
            }

            kdl.append(indentStr);
            kdl.append(entry.getKey());
            
            Object value = entry.getValue();
            if (value == null) {
                kdl.append("\n");
            } else if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                if (map.isEmpty()) {
                    kdl.append("\n");
                } else {
                    kdl.append(" {\n");
                    // Find the nested class for this map
                    Class<?> nestedClass = findNestedClass(entry.getKey(), configClass);
                    generateNode(kdl, map, indent + 1, nestedClass != null ? nestedClass : configClass);
                    kdl.append(indentStr).append("}\n");
                }
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    kdl.append("\n");
                } else {
                    for (Object item : list) {
                        kdl.append(" ").append(formatValue(item));
                    }
                    kdl.append("\n");
                }
            } else {
                kdl.append(" ").append(formatValue(value)).append("\n");
            }
        }
    }

    private Class<?> findNestedClass(String name, Class<?> parentClass) {
        for (Class<?> nestedClass : parentClass.getDeclaredClasses()) {
            if (nestedClass.getSimpleName().equals(name)) {
                return nestedClass;
            }
        }
        return null;
    }

    private String getFieldComment(String fieldName, Class<?> configClass) {
        try {
            // Try to find the field in the current class
            Field field = configClass.getDeclaredField(fieldName);
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                return String.join("\n// ", comment.value());
            }
        } catch (NoSuchFieldException e) {
            // Field not found in current class, try in nested classes
            for (Class<?> nestedClass : configClass.getDeclaredClasses()) {
                try {
                    Field field = nestedClass.getDeclaredField(fieldName);
                    Comment comment = field.getAnnotation(Comment.class);
                    if (comment != null) {
                        return String.join("\n// ", comment.value());
                    }
                } catch (NoSuchFieldException ignored) {
                    // Field not found in this nested class
                }
            }
        }
        return null;
    }

    private String generateKdl(Map<String, Object> data, Class<?> configClass) {
        StringBuilder kdl = new StringBuilder();
        kdl.append("/- kdl-version 2\n\n");
        generateNode(kdl, data, 0, configClass);
        return kdl.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "#null";
        } else if (value instanceof String) {
            String str = (String) value;
            if (features.allowRawStrings() && str.contains("\n")) {
                return "#\"\"\"" + str + "\"\"\"";
            }
            return "\"" + escapeString(str) + "\"";
        } else if (value instanceof Boolean) {
            return "#" + value;
        } else if (value instanceof Number) {
            if (value instanceof Double) {
                double d = (Double) value;
                if (Double.isInfinite(d)) {
                    return d > 0 ? "#inf" : "#-inf";
                } else if (Double.isNaN(d)) {
                    return "#nan";
                }
            }
            return value.toString();
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.isEmpty()) {
                return "{}";
            }
            return "{" + formatMap(map) + "}";
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            return "[" + formatList(list) + "]";
        }
        return "\"" + value.toString() + "\"";
    }

    private boolean needsQuotes(String str) {
        if (str.isEmpty()) {
            return true;
        }
        char first = str.charAt(0);
        if (!isIdentifierStart(first)) {
            return true;
        }
        for (int i = 1; i < str.length(); i++) {
            if (!isIdentifierChar(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private String escapeString(String str) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 32 || c > 126) {
                        escaped.append(String.format("\\u{%x}", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String formatMap(Map<?, ?> map) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                result.append(", ");
            }
            result.append(entry.getKey()).append("=").append(formatValue(entry.getValue()));
            first = false;
        }
        return result.toString();
    }

    private String formatList(List<?> list) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                result.append(", ");
            }
            result.append(formatValue(item));
            first = false;
        }
        return result.toString();
    }

    private boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isIdentifierChar(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9') || c == '-' || c == '.';
    }
} 