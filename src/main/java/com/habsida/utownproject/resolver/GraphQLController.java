package com.habsida.utownproject.resolver;

import com.habsida.utownproject.entity.Dish;
import com.habsida.utownproject.entity.Order;
import com.habsida.utownproject.entity.Restaurant;
import com.habsida.utownproject.entity.User;
import com.habsida.utownproject.service.DishService;
import com.habsida.utownproject.service.OrderService;
import com.habsida.utownproject.service.RestaurantService;
import com.habsida.utownproject.service.UserService;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class GraphQLController {

    private final UserService userService;
    private final RestaurantService restaurantService;
    private final DishService dishService;
    private final OrderService orderService;

    public GraphQLController(UserService userService,
                             RestaurantService restaurantService,
                             DishService dishService,
                             OrderService orderService) {
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.dishService = dishService;
        this.orderService = orderService;
    }

    @QueryMapping
    public List<User> users() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());

        if (!currentUser.getRole().equals("ADMIN")) {
            throw new AccessDeniedException("Недостаточно прав для просмотра всех пользователей!");
        }

        return userService.getAllUsers();
    }

    @QueryMapping
    public User user(@Argument Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        User currentUser = userService.getUserByUsername(currentUsername);

        if (!currentUser.getRole().equals("ADMIN") && !currentUser.getId().equals(id)) {
            throw new AccessDeniedException("Вы не можете запрашивать данные другого пользователя!");
        }

        return userService.getUserById(id);
    }

    @QueryMapping
    public List<Restaurant> restaurants() {
        return restaurantService.getAllRestaurants();
    }

    @QueryMapping
    public Restaurant restaurant(@Argument Long id) {
        return restaurantService.getRestaurantById(id).orElse(null);
    }

    @QueryMapping
    public List<Dish> dishes() {
        return dishService.getAllDishes();
    }

    @QueryMapping
    public Dish dish(@Argument Long id) {
        return dishService.getDishById(id).orElse(null);
    }

    @QueryMapping
    public List<Dish> dishesByRestaurant(@Argument Long restaurantId) {
        return dishService.getDishesByRestaurant(restaurantId);
    }

    @QueryMapping
    public List<Order> orders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userService.getUserByUsername(currentUsername);
        if (currentUser.getRole().equals("ADMIN")) {
            return orderService.getAllOrders();
        } else if (currentUser.getRole().equals("COURIER")) {
            return orderService.getUnassignedOrders();
        } else {
            return orderService.getOrdersByUser(currentUser.getId());
        }
    }

    @QueryMapping
    public Order order(@Argument Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());

        Order order = orderService.getOrderById(id).orElse(null);
        if (order == null) {
            throw new RuntimeException("Заказ не найден!");
        }

        if (!currentUser.getRole().equals("ADMIN") && !order.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Вы не можете просматривать чужие заказы!");
        }

        return order;
    }

    @MutationMapping
    public User createUser(@Argument CreateUserInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());

        if (!currentUser.getRole().equals("ADMIN")) {
            throw new AccessDeniedException("Недостаточно прав для создания пользователей!");
        }

        User user = new User();
        user.setUsername(input.username());
        user.setPassword(input.password());
        user.setRole(input.role());
        user.setFullName(input.fullName());
        return userService.createUser(user);
    }


    @MutationMapping
    public User updateUser(@Argument Long id, @Argument UpdateUserInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());

        if (!currentUser.getRole().equals("ADMIN") && !currentUser.getId().equals(id)) {
            throw new AccessDeniedException("Вы можете изменять только свои данные!");
        }

        User user = new User();
        if (input.username() != null) user.setUsername(input.username());
        if (input.password() != null) user.setPassword(input.password());
        if (input.role() != null) user.setRole(input.role());
        if (input.fullName() != null) user.setFullName(input.fullName());

        return userService.updateUser(id, user);
    }

    @MutationMapping
    public Boolean deleteUser(@Argument Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByUsername(auth.getName());

        if (!currentUser.getRole().equals("ADMIN")) {
            throw new AccessDeniedException("Недостаточно прав для удаления пользователей!");
        }

        userService.deleteUser(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public Restaurant createRestaurant(@Argument CreateRestaurantInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        User owner = userService.getUserByUsername(currentUsername);
        if (!owner.getRole().equals("OWNER")) {
            throw new AccessDeniedException("Только владельцы ресторанов могут создавать заведения!");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(input.name());
        restaurant.setAddress(input.address());
        restaurant.setOwner(owner);

        return restaurantService.createRestaurant(restaurant);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public Restaurant updateRestaurant(@Argument Long id, @Argument UpdateRestaurantInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        Restaurant restaurant = restaurantService.getRestaurantById(id)
                .orElseThrow(() -> new RuntimeException("Ресторан не найден"));

        if (!restaurant.getOwner().getUsername().equals(currentUsername) &&
                auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Вы можете редактировать только свои рестораны!");
        }

        if (input.name() != null) restaurant.setName(input.name());
        if (input.address() != null) restaurant.setAddress(input.address());

        return restaurantService.updateRestaurant(id, restaurant);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public Boolean deleteRestaurant(@Argument Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        Restaurant restaurant = restaurantService.getRestaurantById(id)
                .orElseThrow(() -> new RuntimeException("Ресторан не найден"));

        if (!restaurant.getOwner().getUsername().equals(currentUsername) &&
                auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Вы можете удалять только свои рестораны!");
        }

        restaurantService.deleteRestaurant(id);
        return true;
    }

    @MutationMapping
    public Dish createDish(@Argument CreateDishInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userService.getUserByUsername(currentUsername);

        Restaurant restaurant = restaurantService.getRestaurantById(input.restaurantId())
                .orElseThrow(() -> new RuntimeException("Ресторан не найден"));

        if (!restaurant.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Вы не владелец этого ресторана!");
        }

        return dishService.createDish(input.restaurantId(), input.name(), BigDecimal.valueOf(input.price()));
    }

    @MutationMapping
    public Dish updateDish(@Argument Long id, @Argument UpdateDishInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userService.getUserByUsername(currentUsername);

        Dish dish = dishService.getDishById(id).orElseThrow(() -> new RuntimeException("Блюдо не найдено"));
        if (!dish.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Вы не владелец ресторана, в котором находится это блюдо!");
        }

        return dishService.updateDish(id, input.name(), input.price() != null ? BigDecimal.valueOf(input.price()) : null);
    }

    @MutationMapping
    public Boolean deleteDish(@Argument Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userService.getUserByUsername(currentUsername);

        Dish dish = dishService.getDishById(id).orElseThrow(() -> new RuntimeException("Блюдо не найдено"));
        if (!dish.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Вы не владелец ресторана, в котором находится это блюдо!");
        }

        dishService.deleteDish(id);
        return true;
    }

    @MutationMapping
    public Order createOrder(@Argument CreateOrderInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        User currentUser = userService.getUserByUsername(currentUsername);
        if (!currentUser.getRole().equals("USER")) {
            throw new AccessDeniedException("Только пользователи могут делать заказы!");
        }
        List<OrderService.OrderItemRequest> itemRequests = input.items().stream()
                .map(i -> new OrderService.OrderItemRequest(i.dishId(), i.quantity()))
                .collect(Collectors.toList());

        return orderService.createOrder(currentUser.getId(), input.restaurantId(), itemRequests);
    }

    @MutationMapping
    public Order updateOrderStatus(@Argument Long orderId, @Argument String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userService.getUserByUsername(currentUsername);

        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (status.equals("CONFIRMED") && order.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            if (!order.getStatus().equals("CREATED")) {
                throw new RuntimeException("Только новые заказы можно подтверждать!");
            }
            order.setStatus(status);
            return orderService.updateOrder(order);
        }

        if (status.equals("DELIVERED") && currentUser.getRole().equals("COURIER")) {
            if (!order.getStatus().equals("IN_PROGRESS")) {
                throw new RuntimeException("Заказ можно доставить только после принятия в работу!");
            }
            order.setStatus(status);
            return orderService.updateOrder(order);
        }

        throw new AccessDeniedException("Вы не можете изменить статус заказа!");
    }

    @MutationMapping
    public Order assignOrderToCourier(@Argument Long orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        User currentUser = userService.getUserByUsername(currentUsername);
        if (!currentUser.getRole().equals("COURIER")) {
            throw new AccessDeniedException("Только курьер может брать заказ в работу!");
        }
        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getStatus().equals("CREATED")) {
            throw new RuntimeException("Этот заказ уже выполняется или завершён!");
        }
        order.setStatus("IN_PROGRESS");
        order.setUser(currentUser);
        return orderService.updateOrder(order);
    }

    @MutationMapping
    public Boolean deleteOrder(@Argument Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        User currentUser = userService.getUserByUsername(currentUsername);
        if (!order.getUser().getId().equals(currentUser.getId()) &&
                !order.getRestaurant().getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Вы не можете удалить этот заказ!");
        }

        orderService.deleteOrder(id);
        return true;
    }

    public record CreateUserInput(String username, String password, String role, String fullName) {}
    public record UpdateUserInput(String username, String password, String role, String fullName) {}
    public record CreateRestaurantInput(String name, String address) {}
    public record UpdateRestaurantInput(String name, String address) {}
    public record CreateDishInput(String name, Float price, Long restaurantId) {}
    public record UpdateDishInput(String name, Float price, Long restaurantId) {}
    public record CreateOrderInput(Long userId, Long restaurantId, List<OrderItemInput> items) {}
    public record OrderItemInput(Long dishId, Integer quantity) {}
}
