package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

	List<UserRoleEntity> findByUserId(UUID userId);

	boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

	void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
