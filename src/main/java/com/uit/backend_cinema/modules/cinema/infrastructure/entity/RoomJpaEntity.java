package com.uit.backend_cinema.modules.cinema.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.Data;

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
