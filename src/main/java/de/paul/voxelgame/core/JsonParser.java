package de.paul.voxelgame.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonParser {
    private static final char BYTE_ORDER_MARK = '\uFEFF';

    private final String input;
    private int index;

    private JsonParser(final String input) {
        this.input = removeLeadingByteOrderMark(input == null ? "" : input);
    }

    private static String removeLeadingByteOrderMark(final String input) {
        if (!input.isEmpty() && input.charAt(0) == BYTE_ORDER_MARK) {
            return input.substring(1);
        }
        return input;
    }

    static Object parse(final String input) {
        final JsonParser parser = new JsonParser(input);
        final Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(final String input) {
        final Object value = parse(input);
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("JSON root must be an object");
        }
        return (Map<String, Object>) value;
    }

    private Object readValue() {
        skipWhitespace();
        if (isAtEnd()) {
            throw error("Unexpected end of JSON");
        }

        final char c = input.charAt(index);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield readNumber();
                }
                throw error("Unexpected character: " + c);
            }
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        final Map<String, Object> object = new LinkedHashMap<>();
        skipWhitespace();
        if (tryConsume('}')) {
            return object;
        }

        while (true) {
            skipWhitespace();
            final String key = readString();
            skipWhitespace();
            expect(':');
            object.put(key, readValue());
            skipWhitespace();
            if (tryConsume('}')) {
                return object;
            }
            expect(',');
        }
    }

    private List<Object> readArray() {
        expect('[');
        final List<Object> array = new ArrayList<>();
        skipWhitespace();
        if (tryConsume(']')) {
            return array;
        }

        while (true) {
            array.add(readValue());
            skipWhitespace();
            if (tryConsume(']')) {
                return array;
            }
            expect(',');
        }
    }

    private String readString() {
        expect('"');
        final StringBuilder builder = new StringBuilder();
        while (!isAtEnd()) {
            final char c = input.charAt(index++);
            if (c == '"') {
                return builder.toString();
            }
            if (c != '\\') {
                builder.append(c);
                continue;
            }

            if (isAtEnd()) {
                throw error("Unfinished escape sequence");
            }
            final char escaped = input.charAt(index++);
            switch (escaped) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> builder.append(readUnicodeEscape());
                default -> throw error("Invalid escape sequence: \\" + escaped);
            }
        }
        throw error("Unterminated string");
    }

    private char readUnicodeEscape() {
        if (index + 4 > input.length()) {
            throw error("Invalid unicode escape");
        }
        final String hex = input.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw error("Invalid unicode escape: " + hex);
        }
    }

    private Object readLiteral(final String literal, final Object value) {
        if (!input.startsWith(literal, index)) {
            throw error("Expected literal: " + literal);
        }
        index += literal.length();
        return value;
    }

    private Number readNumber() {
        final int start = index;
        if (input.charAt(index) == '-') {
            index++;
        }
        readDigits();
        if (!isAtEnd() && input.charAt(index) == '.') {
            index++;
            readDigits();
        }
        if (!isAtEnd() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
            index++;
            if (!isAtEnd() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                index++;
            }
            readDigits();
        }

        final String raw = input.substring(start, index);
        try {
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw error("Invalid number: " + raw);
        }
    }

    private void readDigits() {
        final int start = index;
        while (!isAtEnd()) {
            final char c = input.charAt(index);
            if (c < '0' || c > '9') {
                break;
            }
            index++;
        }
        if (start == index) {
            throw error("Expected digit");
        }
    }

    private void skipWhitespace() {
        while (!isAtEnd()) {
            final char c = input.charAt(index);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                return;
            }
            index++;
        }
    }

    private boolean tryConsume(final char expected) {
        if (!isAtEnd() && input.charAt(index) == expected) {
            index++;
            return true;
        }
        return false;
    }

    private void expect(final char expected) {
        if (isAtEnd() || input.charAt(index) != expected) {
            throw error("Expected '" + expected + "'");
        }
        index++;
    }

    private boolean isAtEnd() {
        return index >= input.length();
    }

    private IllegalArgumentException error(final String message) {
        return new IllegalArgumentException(message + " at character " + index);
    }
}
