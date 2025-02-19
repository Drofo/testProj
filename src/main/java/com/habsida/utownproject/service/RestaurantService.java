package com.habsida.utownproject.service;

import com.habsida.utownproject.entity.Restaurant;
import com.habsida.utownproject.entity.User;
import com.habsida.utownproject.repository.RestaurantRepository;
import com.habsida.utownproject.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    public Restaurant createRestaurant(Restaurant restaurant) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        User owner = userRepository.findByUsername(currentUsername);
        if (owner == null) {
            throw new RuntimeException("Пользователь не найден: " + currentUsername);
        }

        restaurant.setOwner(owner);
        return restaurantRepository.save(restaurant);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public Optional<Restaurant> getRestaurantById(Long id) {
        return restaurantRepository.findById(id);
    }

    public Restaurant updateRestaurant(Long id, Restaurant updated) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ресторан не найден"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        if (!restaurant.getOwner().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Вы не владелец этого ресторана!");
        }

        if (updated.getName() != null) restaurant.setName(updated.getName());
        if (updated.getAddress() != null) restaurant.setAddress(updated.getAddress());

        return restaurantRepository.save(restaurant);
    }

    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ресторан не найден"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        if (!restaurant.getOwner().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Вы не владелец этого ресторана!");
        }

        restaurantRepository.deleteById(id);
    }
}
