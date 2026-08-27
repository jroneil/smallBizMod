package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.Permission;
import com.oneilsoftware.sbp.module.usermaintenance.api.PermissionOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownPermissionException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.PermissionEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.PermissionRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RolePermissionEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RolePermissionRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleRepository;
import org.springframework.transaction.annotation.Transactional;

public class PermissionApplicationService implements PermissionOperations {

	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final RoleRepository roleRepository;

	public PermissionApplicationService(
			PermissionRepository permissionRepository,
			RolePermissionRepository rolePermissionRepository,
			RoleRepository roleRepository) {
		this.permissionRepository = permissionRepository;
		this.rolePermissionRepository = rolePermissionRepository;
		this.roleRepository = roleRepository;
	}

	@Override
	@Transactional
	public Permission define(String key, String description) {
		String trimmedKey = requireKey(key);
		String trimmedDescription = description == null ? "" : description.trim();
		if (trimmedDescription.isBlank()) {
			throw new IllegalArgumentException("Permission description is required");
		}

		Optional<PermissionEntity> existing = permissionRepository.findByKey(trimmedKey);
		if (existing.isPresent()) {
			existing.get().updateDescription(trimmedDescription);
			return toPermission(permissionRepository.save(existing.get()));
		}
		return toPermission(permissionRepository.save(
				new PermissionEntity(UUID.randomUUID(), trimmedKey, trimmedDescription)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Permission> list() {
		return permissionRepository.findAllByOrderByKeyAsc().stream()
				.map(PermissionApplicationService::toPermission)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Permission> listByRole(UUID roleId) {
		requireRole(roleId);
		List<UUID> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
				.map(RolePermissionEntity::getPermissionId)
				.toList();
		if (permissionIds.isEmpty()) {
			return List.of();
		}
		return permissionRepository.findAllById(permissionIds).stream()
				.map(PermissionApplicationService::toPermission)
				.sorted(Comparator.comparing(Permission::key))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> listRolesByPermission(String permissionKey) {
		PermissionEntity permission = requirePermission(permissionKey);
		List<UUID> roleIds = rolePermissionRepository.findByPermissionId(permission.getId()).stream()
				.map(RolePermissionEntity::getRoleId)
				.toList();
		if (roleIds.isEmpty()) {
			return List.of();
		}
		return roleRepository.findAllById(roleIds).stream()
				.map(RoleApplicationService::toRole)
				.sorted(Comparator.comparing(Role::name))
				.toList();
	}

	@Override
	@Transactional
	public void grant(UUID roleId, String permissionKey) {
		requireRole(roleId);
		PermissionEntity permission = requirePermission(permissionKey);
		if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permission.getId())) {
			rolePermissionRepository.save(new RolePermissionEntity(roleId, permission.getId(), Timestamps.now()));
		}
	}

	@Override
	@Transactional
	public void revoke(UUID roleId, String permissionKey) {
		requireRole(roleId);
		PermissionEntity permission = requirePermission(permissionKey);
		rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permission.getId());
	}

	private void requireRole(UUID roleId) {
		if (!roleRepository.existsById(roleId)) {
			throw new UnknownRoleException(roleId);
		}
	}

	private PermissionEntity requirePermission(String key) {
		return permissionRepository.findByKey(requireKey(key))
				.orElseThrow(() -> new UnknownPermissionException(requireKey(key)));
	}

	private static String requireKey(String key) {
		String trimmed = key == null ? "" : key.trim();
		if (trimmed.isBlank()) {
			throw new IllegalArgumentException("Permission key is required");
		}
		return trimmed;
	}

	private static Permission toPermission(PermissionEntity entity) {
		return new Permission(entity.getId(), entity.getKey(), entity.getDescription());
	}
}
