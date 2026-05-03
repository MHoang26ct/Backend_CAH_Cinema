package com.uit.backend_cinema.modules.food_order.infrastructure.persistence;

import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrderItem;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodOrderRepository;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodJpaEntity;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodOrderItemId;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodOrderItemJpaEntity;
import com.uit.backend_cinema.modules.food_order.infrastructure.entity.FoodOrderJpaEntity;
import com.uit.backend_cinema.modules.food_order.infrastructure.mapper.FoodOrderInfraMapper;
import com.uit.backend_cinema.modules.food_order.infrastructure.mapper.FoodOrderItemInfraMapper;
import com.uit.backend_cinema.modules.food_order.infrastructure.repository.JpaFoodOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class FoodOrderRepositoryImpl implements FoodOrderRepository {
    private final FoodOrderInfraMapper mapper;
    private final FoodOrderItemInfraMapper itemMapper;
    private final JpaFoodOrderRepository jpaFoodOrderRepository;

    public FoodOrderRepositoryImpl(FoodOrderInfraMapper mapper,
                                   FoodOrderItemInfraMapper itemMapper,
                                   JpaFoodOrderRepository jpaFoodOrderRepository)
    {
        this.itemMapper = itemMapper;
        this.mapper = mapper;
        this.jpaFoodOrderRepository = jpaFoodOrderRepository;
    }

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public Optional<FoodOrder> getByBookingId(long bookingId) {
        return jpaFoodOrderRepository.findOrderWithItemsByBookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public List<FoodOrder> getAllByListBookingId(Set<Long> bookingId) {
        return jpaFoodOrderRepository.findAllWithItemsByBookingIdList(bookingId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public FoodOrder save(FoodOrder foodOrder) {
        FoodOrderJpaEntity newOrder = mapper.toEntity(foodOrder);
        // Persist trước để lấy foodOrderId được generate từ DB
        FoodOrderJpaEntity savedOrder = jpaFoodOrderRepository.saveAndFlush(newOrder);
        if (foodOrder.getItems() != null) {
            for (FoodOrderItem item : foodOrder.getItems()) {
                FoodOrderItemJpaEntity itemEntity = itemMapper.toEntity(item);

                // Dùng savedOrder.getFoodOrderId() (ID thật) thay vì newOrder (ID = 0)
                itemEntity.setId(new FoodOrderItemId(savedOrder.getFoodOrderId(), item.getFoodId()));

                FoodJpaEntity foodProxy = entityManager.getReference(FoodJpaEntity.class, item.getFoodId());
                itemEntity.setFood(foodProxy);

                itemEntity.setFoodOrder(savedOrder);
                savedOrder.getItems().add(itemEntity);
            }
        }
        return mapper.toDomain(jpaFoodOrderRepository.save(savedOrder));
    }
}
