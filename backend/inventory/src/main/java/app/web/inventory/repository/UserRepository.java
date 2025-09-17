package app.web.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.web.inventory.model.Users;

public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);
}
