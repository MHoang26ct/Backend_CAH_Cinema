package com.uit.backend_cinema.modules.food_order.infrastructure.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodOrderJpaEntity;

@Repository
public interface JpaFoodOrderRepository extends JpaRepository<FoodOrderJpaEntity, Long> {
    @Query("SELECT o FROM FoodOrderJpaEntity o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.food " +
            "WHERE o.bookingId = :id")
    Optional<FoodOrderJpaEntity> findOrderWithItemsByBookingId(@Param("id") long bookingId);

    @Query("SELECT DISTINCT o FROM FoodOrderJpaEntity o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.food " +
            "WHERE o.bookingId IN :bookingIds")
    List<FoodOrderJpaEntity> findAllWithItemsByBookingIdList(@Param("bookingIds") Collection<Long> bookingIds);
}
