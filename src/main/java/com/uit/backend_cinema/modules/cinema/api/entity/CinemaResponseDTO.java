package com.uit.backend_cinema.modules.cinema.api.entity;

import lombok.Data;

@Data
public class CinemaResponseDTO {
    private long cinemaId;
    private String name;
    private String address;
    private String hotline;
}
