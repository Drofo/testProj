package com.habsida.utownproject.service;

import com.habsida.utownproject.entity.*;
import com.habsida.utownproject.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final RestaurantRepository restaurantRepository;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        DishRepository dishRepository,
                        RestaurantRepository restaurantRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.dishRepository = dishRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public Order createOrder(Long userId, Long restaurantId, List<OrderItemRequest> items) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        Order order = new Order(user, restaurant, "CREATED");
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        for (OrderItemRequest itemReq : items) {
            Dish dish = dishRepository.findById(itemReq.getDishId())
                    .orElseThrow(() -> new RuntimeException("Dish not found: " + itemReq.getDishId()));
            if (!dish.getRestaurant().getId().equals(restaurantId)) {
                throw new RuntimeException("Dish " + dish.getName() + " does not belong to the selected restaurant.");
            }

            OrderItem orderItem = new OrderItem(order, dish, itemReq.getQuantity());
            order.getOrderItems().add(orderItem);
        }
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Order updateOrderStatus(Long orderId, String newStatus) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus(newStatus);
                    return orderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    public List<Order> getUnassignedOrders() {
        return orderRepository.findByStatus("CREATED");
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public static class OrderItemRequest {
        private Long dishId;
        private Integer quantity;

        public OrderItemRequest() {
        }

        public OrderItemRequest(Long dishId, Integer quantity) {
            this.dishId = dishId;
            this.quantity = quantity;
        }

        public Long getDishId() {
            return dishId;
        }

        public void setDishId(Long dishId) {
            this.dishId = dishId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
