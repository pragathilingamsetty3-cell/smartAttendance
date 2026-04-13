package com.example.smartAttendence.repository.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenV1Repository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUser(User user);
    
    List<RefreshToken> findByUserAndExpiryDateAfter(User user, Instant now);
    
    List<RefreshToken> findByExpiryDateBefore(Instant now);
    
    void deleteByUser(User user);
    
    void deleteByExpiryDateBefore(Instant now);
    
    void deleteByUserId(UUID userId);
}
