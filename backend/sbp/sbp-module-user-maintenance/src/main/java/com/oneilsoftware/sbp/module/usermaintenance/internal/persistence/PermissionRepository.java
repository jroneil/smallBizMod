package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

	Optional<PermissionEntity> findByKey(String key);

	List<PermissionEntity> findAllByOrderByKeyAsc();
}
