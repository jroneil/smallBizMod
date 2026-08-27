package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

	List<RolePermissionEntity> findByRoleId(UUID roleId);

	List<RolePermissionEntity> findByPermissionId(UUID permissionId);

	boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

	void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
