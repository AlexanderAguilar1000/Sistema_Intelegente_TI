package com.corporativoTI.SistemaInteligenteTI.users.dto;

/** Un usuario para el selector de usuario activo: id, nombre, rol y área (RF-1). */
public record UserResponse(Long id, String fullName, String role, String area) {}
//un record se utiliza cuando desarrollas un dto simple que transporta dtos y sus valores no se modifican