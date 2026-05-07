package de.paul.voxelgame.core;

public enum Language {
    DE_DE("de_de"),
    EN_US("en_us");

    private final String code;

    Language(final String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public Language next() {
        final Language[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
