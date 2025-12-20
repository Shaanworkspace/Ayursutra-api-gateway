package com.apigateway.User.Repository;


import com.apigateway.User.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
	Optional<User> findByAuth0Id(String auth0Id);
	boolean existsByEmail(String email);
}