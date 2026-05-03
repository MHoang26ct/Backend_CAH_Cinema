package com.uit.backend_cinema.modules.cinema.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "rooms")
@SQLRestriction("is_deleted = false")
@Data
public class RoomJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long roomId;

    @Column(name = "cinema_id", nullable = false)
    private long cinemaId;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
