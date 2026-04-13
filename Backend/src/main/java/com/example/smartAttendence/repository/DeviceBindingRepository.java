package com.example.smartAttendence.repository;

import com.example.smartAttendence.entity.DeviceBinding;
import com.example.smartAttendence.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceBindingRepository extends JpaRepository<DeviceBinding, UUID> {

    Optional<DeviceBinding> findByUser(User user);

    Optional<DeviceBinding> findByDeviceId(String deviceId);

    Optional<DeviceBinding> findByDeviceFingerprint(String deviceFingerprint);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceFingerprint(String deviceFingerprint);

    @Query("SELECT COUNT(d) > 0 FROM DeviceBinding d WHERE d.user.id = :userId AND d.isActive = true")
    boolean hasActiveDeviceBinding(@Param("userId") UUID userId);

    void deleteByUser(User user);
}
