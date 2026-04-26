package com.uit.backend_cinema.modules.price_config.domain.helper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MovieFormat {
    TYPE_2D("2D"),
    TYPE_3D("3D"),
    TYPE_IMAX("IMAX");

    private final String value;

    MovieFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MovieFormat fromValue(String text) {
        for (MovieFormat format : MovieFormat.values()) {
            if (format.value.equalsIgnoreCase(text)) {
                return format;
            }
        }
        return null;
    }
}
