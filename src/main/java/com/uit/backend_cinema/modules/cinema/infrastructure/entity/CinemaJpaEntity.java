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

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "hotline")
    private String hotline;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
