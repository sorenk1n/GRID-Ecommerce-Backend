package com.khomsi.backend.main.user.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_credentials")
public class UserCredential {
    @Id
    @Column(name = "users_id", nullable = false)
    private String userId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
