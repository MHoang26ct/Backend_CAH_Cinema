package com.uit.backend_cinema.modules.movies.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "genres")
@SQLRestriction("is_deleted = false") // Chặn dữ liệu đã xóa
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class GenreJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long genreId;

    private String name;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private Boolean isDeleted = false;
}
