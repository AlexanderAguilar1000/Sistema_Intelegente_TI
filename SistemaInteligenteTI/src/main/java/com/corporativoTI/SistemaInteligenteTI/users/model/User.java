package com.corporativoTI.SistemaInteligenteTI.users.model;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Supervisor o técnico precargado (RF-1, RF-2). Sin contraseña: el MVP no autentica. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Nulo para supervisores; obligatorio para técnicos (RF-2). */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Area area;
}
