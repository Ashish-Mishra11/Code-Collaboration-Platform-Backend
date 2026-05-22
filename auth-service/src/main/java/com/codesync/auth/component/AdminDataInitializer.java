package com.codesync.auth.component;

import com.codesync.auth.entity.Admin;
import com.codesync.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the hardcoded admin account (Ashish / Ashish#1112) on first startup.
 * Skipped if the record already exists.
 */
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;

    @Override
    public void run(String... args) {
        if (adminRepository.findByUsername("Ashish").isEmpty()) {
            Admin admin = new Admin();
            admin.setUsername("Ashish");
            admin.setPasswordHash(new BCryptPasswordEncoder(12).encode("Ashish#1112"));
            admin.setRole("ADMIN");
            adminRepository.save(admin);
            System.out.println("[AdminDataInitializer] Admin 'Ashish' created.");
        }
    }
}
