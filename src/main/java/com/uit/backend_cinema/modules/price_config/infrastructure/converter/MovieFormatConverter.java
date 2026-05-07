package com.uit.backend_cinema.modules.price_config.infrastructure.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;

@Converter(autoApply = true)
public class MovieFormatConverter implements AttributeConverter<MovieFormat, String> {
    @Override
    public String convertToDatabaseColumn(MovieFormat movieFormat) {
        if (movieFormat == null) {return null;}
        return movieFormat.getValue();
    }

    @Override
    public MovieFormat convertToEntityAttribute(String value) {
        if (value == null) {return null;}
        return MovieFormat.fromValue(value);
    }
}
