package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.AuditActions;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateRole;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleNameAlreadyExistsException;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateRole;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleRepository;
import org.springframework.transaction.annotation.Transactional;

public class RoleApplicationService implements RoleOperations {

	private final RoleRepository roleRepository;
	private final AuditSupport auditSupport;

	public RoleApplicationService(RoleRepository roleRepository, AuditSupport auditSupport) {
		this.roleRepository = roleRepository;
		this.auditSupport = auditSupport;
	}

	@Override
	@Transactional
	public Role create(CreateRole command) {
		if (roleRepository.existsByName(command.name())) {
			throw new RoleNameAlreadyExistsException(command.name());
		}

		var now = Timestamps.now();
		RoleEntity role = new RoleEntity(
				UUID.randomUUID(),
				command.name(),
				command.description(),
				RoleStatus.ACTIVE,
				now,
				now);

		Role saved = toRole(roleRepository.save(role));
		auditSupport.record(
				AuditActions.SUBJECT_ROLE,
				saved.id().toString(),
				AuditActions.ROLE_CREATED,
				List.of("name", "description", "status"),
				null);
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> list() {
		return roleRepository.findAllByOrderByNameAsc().stream()
				.map(RoleApplicationService::toRole)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Role> findById(UUID roleId) {
		return roleRepository.findById(roleId)
				.map(RoleApplicationService::toRole);
	}

	@Override
	@Transactional
	public Role update(UUID roleId, UpdateRole command) {
		RoleEntity role = requireRole(roleId);
		List<String> changed = new ArrayList<>();
		if (!Objects.equals(role.getName(), command.name())) {
			changed.add("name");
		}
		if (!Objects.equals(role.getDescription(), command.description())) {
			changed.add("description");
		}
		role.update(command.name(), command.description(), Timestamps.now());
		Role saved = toRole(roleRepository.save(role));
		auditSupport.record(
				AuditActions.SUBJECT_ROLE,
				saved.id().toString(),
				AuditActions.ROLE_UPDATED,
				changed,
				null);
		return saved;
	}

	@Override
	@Transactional
	public Role retire(UUID roleId) {
		RoleEntity role = requireRole(roleId);
		role.retire(Timestamps.now());
		Role saved = toRole(roleRepository.save(role));
		auditSupport.record(
				AuditActions.SUBJECT_ROLE,
				saved.id().toString(),
				AuditActions.ROLE_RETIRED,
				List.of("status"),
				null);
		return saved;
	}

	private RoleEntity requireRole(UUID roleId) {
		return roleRepository.findById(roleId)
				.orElseThrow(() -> new UnknownRoleException(roleId));
	}

	static Role toRole(RoleEntity entity) {
		return new Role(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
