package com.uit.backend_cinema.modules.food_order.infrastructure.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;

@Entity
@Table(name = "food_orders")
@EntityListeners(AuditingEntityListener.class)
@Data
public class FoodOrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long foodOrderId;

    @Column(name = "booking_id", nullable = false)
    private long bookingId;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodOrderItemJpaEntity> items = new ArrayList<>();

    public void addItem(FoodOrderItemJpaEntity item) {
        items.add(item);
        item.setFoodOrder(this);
    }
}
