package com.uit.backend_cinema.modules.cinema.domain.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PendingRoomCleanup {
    private Long cleanupId;
    private Long oldRoomId;
    private Long newRoomId;
    private LocalDateTime replacedAt;
    private Boolean cleanedUp;
    private LocalDateTime cleanedUpAt;

    public PendingRoomCleanup(Long oldRoomId, Long newRoomId) {
        this.oldRoomId = oldRoomId;
        this.newRoomId = newRoomId;
        this.replacedAt = LocalDateTime.now();
        this.cleanedUp = false;
    }
}
