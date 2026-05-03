package com.uit.backend_cinema.modules.cinema.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "cinemas")
@SQLRestriction("is_deleted = false")
@Data
public class CinemaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long cinemaId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "hotline")
    private String hotline;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
