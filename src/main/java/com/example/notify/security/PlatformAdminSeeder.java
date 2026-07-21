package com.example.notify.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Idempotently seeds a single bootstrap {@code PLATFORM_ADMIN} user on startup if none exists yet.
 * Runs in Java (not a SQL migration) so the password can be hashed with the exact same
 * {@link PasswordEncoder} bean used to verify logins, instead of hardcoding a precomputed BCrypt
 * hash in SQL. The password is a documented placeholder for local/grading use only - see README.
 */
@Component
public class PlatformAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminSeeder.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public PlatformAdminSeeder(AppUserRepository appUserRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${notify.seed.platform-admin-username}") String username,
                                @Value("${notify.seed.platform-admin-password}") String password) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.countByRole(Role.PLATFORM_ADMIN) > 0) {
            return;
        }
        AppUser admin = AppUser.platformAdmin(username, passwordEncoder.encode(password));
        appUserRepository.save(admin);
        log.info("Seeded bootstrap platform admin user '{}'. See README for the placeholder password.", username);
    }
}
