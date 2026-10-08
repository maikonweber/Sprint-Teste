package com.example.people.config;

import com.example.people.entity.Role;
import com.example.people.entity.User;
import com.example.people.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o usuário administrador inicial (credenciais vindas de ADMIN_USERNAME / ADMIN_PASSWORD)
 * caso ele ainda não exista. A senha é persistida apenas como hash BCrypt.
 */
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = adminProperties.username();
        if (userRepository.existsByUsername(username)) {
            log.info("Usuário administrador '{}' já existe; seed ignorado", username);
            return;
        }
        userRepository.save(new User(username, passwordEncoder.encode(adminProperties.password()), Role.ADMIN));
        log.info("Usuário administrador '{}' criado", username);
    }
}
