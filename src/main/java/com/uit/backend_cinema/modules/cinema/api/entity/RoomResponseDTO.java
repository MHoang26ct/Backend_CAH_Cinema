package com.uit.backend_cinema.modules.cinema.api.entity;

import lombok.Data;

@Data
public class RoomResponseDTO {
    private long roomId;
    private long cinemaId;
    private String roomName;
}
