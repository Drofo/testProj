package com.habsida.utownproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, unique = true)
    private RoleType type;

    @Column(nullable = true, unique = true)
    private String name;

    public Role() {}

    public Role(RoleType type) {
        this.type = type;
        this.name = type.name();
    }

    public Role(String name) {
        this.type = null;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
        this.name = type.name();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.type = null;
        this.name = name;
    }
}
