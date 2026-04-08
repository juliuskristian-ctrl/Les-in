package com.lesonline.tutoringplatform.repository;

import com.lesonline.tutoringplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRole(String role);
    List<User> findByRoleAndMataPelajaranContainingIgnoreCase(String role, String mataPelajaran);

    // Tambahkan ini untuk proses Login
    Optional<User> findByEmailAndPassword(String email, String password);
}