package com.conduit.backend.identity.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findAllByIds(Collection<Long> ids);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User save(User user);
}
