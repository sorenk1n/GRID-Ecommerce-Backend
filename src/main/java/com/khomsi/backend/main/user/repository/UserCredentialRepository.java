package com.khomsi.backend.main.user.repository;

import com.khomsi.backend.main.user.model.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, String> {
}
