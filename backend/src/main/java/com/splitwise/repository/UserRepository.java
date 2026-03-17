package com.splitwise.repository;

import com.splitwise.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * WHY extend JpaRepository (not CrudRepository or PagingAndSortingRepository)?
 *
 * Inheritance chain: JpaRepository > PagingAndSortingRepository > CrudRepository
 *
 * - CrudRepository: Basic CRUD (save, findById, delete, findAll)
 * - PagingAndSortingRepository: Adds pagination and sorting
 * - JpaRepository: Adds batch operations (saveAll, flush, deleteInBatch) + returns List instead of Iterable
 *
 * We use JpaRepository because we want ALL of these features. No downside.
 *
 * HOW DOES "findByEmail" WORK WITHOUT WRITING SQL?
 * Spring Data JPA uses "Derived Query Methods" — it parses the method name:
 *   findByEmail → SELECT * FROM users WHERE email = ?
 *   findByNameContainingIgnoreCase → SELECT * FROM users WHERE LOWER(name) LIKE LOWER('%?%')
 *   existsByEmail → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 *
 * This is called "Convention over Configuration" — follow the naming convention,
 * get the implementation for free. For complex queries, we use @Query with JPQL.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByNameContainingIgnoreCaseAndRegisteredTrue(String name);

    List<User> findByRegisteredTrue();

    List<User> findByIdIn(List<Long> ids);
}
