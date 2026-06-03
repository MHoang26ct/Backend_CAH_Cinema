package com.uit.backend_cinema.modules.cinema.infrastructure.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pending_room_cleanups")
@Data
@NoArgsConstructor
public class PendingRoomCleanupJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cleanup_id")
    private Long cleanupId;

    @Column(name = "old_room_id", nullable = false)
    private Long oldRoomId;

    @Column(name = "new_room_id", nullable = false)
    private Long newRoomId;

    @Column(name = "replaced_at", nullable = false)
    private LocalDateTime replacedAt;

    @Column(name = "cleaned_up", nullable = false)
    private Boolean cleanedUp = false;

    @Column(name = "cleaned_up_at")
    private LocalDateTime cleanedUpAt;
}
