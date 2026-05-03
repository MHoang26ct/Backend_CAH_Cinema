package com.uit.backend_cinema.modules.cinema.domain.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Room {
    private long roomId;
    private long cinemaId;
    private String roomName;
    private boolean deleted = false;
}
