package com.splitwise.security;

import com.splitwise.model.User;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads user-specific data for Spring Security.
 *
 * Spring Security calls this during authentication to find the user by email
 * and get their password hash for comparison.
 *
 * WHY two methods (loadByUsername and loadById)?
 * - loadUserByUsername: Called by Spring Security during login (email/password auth)
 * - loadUserById: Called by our JWT filter to load the user from the JWT's userId claim
 *
 * WHY @Transactional(readOnly = true)?
 * - Tells Hibernate this is a read-only operation — no dirty checking needed
 * - Slightly better performance because Hibernate skips the "did anything change?" check
 * - It's a best practice for all read-only service methods
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return UserPrincipal.create(user);
    }
}
