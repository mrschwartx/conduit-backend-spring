package com.conduit.backend.identity.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.conduit.backend.identity.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	@Query("select u from User u where lower(u.email) = lower(:email)")
	Optional<User> findByEmail(@Param("email") String email);

	@Query("select u from User u where lower(u.username) = lower(:username)")
	Optional<User> findByUsername(@Param("username") String username);

	@Query("select u.username from User u where u.id = :id")
	Optional<String> findUsernameById(@Param("id") long id);

	@Query("select count(u) from User u where lower(u.username) = lower(:username) and u.id <> :excludeId")
	long countByUsername(@Param("username") String username, @Param("excludeId") long excludeId);

	@Query("select count(u) from User u where lower(u.email) = lower(:email) and u.id <> :excludeId")
	long countByEmail(@Param("email") String email, @Param("excludeId") long excludeId);
}
