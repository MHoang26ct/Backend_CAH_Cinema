package com.uit.backend_cinema.modules.cinema.domain.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cinema {
    private long cinemaId;
    private String name;
    private String address;
    private String hotline;
    private boolean deleted = false;
}
