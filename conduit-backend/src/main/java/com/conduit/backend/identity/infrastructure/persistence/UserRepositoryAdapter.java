package com.conduit.backend.identity.infrastructure.persistence;

import com.conduit.backend.identity.domain.User;
import com.conduit.backend.identity.domain.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;

    UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public List<User> findAllByIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.getId() == null
                ? UserEntity.create()
                : repository.findById(user.getId()).orElseGet(UserEntity::create);
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setBio(user.getBio());
        entity.setImage(user.getImage());
        return toDomain(repository.save(entity));
    }

    private User toDomain(UserEntity entity) {
        return User.restore(entity.getId(), entity.getUsername(), entity.getEmail(),
                entity.getPasswordHash(), entity.getBio(), entity.getImage());
    }
}
