package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.AuditActions;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateGroup;
import com.oneilsoftware.sbp.module.usermaintenance.api.Group;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupNameAlreadyExistsException;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownGroupException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateGroup;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRoleEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRoleRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserGroupEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserGroupRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserRepository;
import org.springframework.transaction.annotation.Transactional;

public class GroupApplicationService implements GroupOperations {

	private final GroupRepository groupRepository;
	private final UserGroupRepository userGroupRepository;
	private final UserRepository userRepository;
	private final GroupRoleRepository groupRoleRepository;
	private final RoleRepository roleRepository;
	private final AuditSupport auditSupport;
	private final UserMapper userMapper;

	public GroupApplicationService(
			GroupRepository groupRepository,
			UserGroupRepository userGroupRepository,
			UserRepository userRepository,
			GroupRoleRepository groupRoleRepository,
			RoleRepository roleRepository,
			AuditSupport auditSupport,
			UserMapper userMapper) {
		this.groupRepository = groupRepository;
		this.userGroupRepository = userGroupRepository;
		this.userRepository = userRepository;
		this.groupRoleRepository = groupRoleRepository;
		this.roleRepository = roleRepository;
		this.auditSupport = auditSupport;
		this.userMapper = userMapper;
	}

	@Override
	@Transactional
	public Group create(CreateGroup command) {
		if (groupRepository.existsByName(command.name())) {
			throw new GroupNameAlreadyExistsException(command.name());
		}

		var now = Timestamps.now();
		GroupEntity group = new GroupEntity(
				UUID.randomUUID(),
				command.name(),
				command.description(),
				GroupStatus.ACTIVE,
				now,
				now);

		Group saved = toGroup(groupRepository.save(group));
		auditSupport.record(
				AuditActions.SUBJECT_GROUP,
				saved.id().toString(),
				AuditActions.GROUP_CREATED,
				List.of("name", "description", "status"),
				null);
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Group> list() {
		return groupRepository.findAllByOrderByNameAsc().stream()
				.map(GroupApplicationService::toGroup)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Group> findById(UUID groupId) {
		return groupRepository.findById(groupId)
				.map(GroupApplicationService::toGroup);
	}

	@Override
	@Transactional
	public Group update(UUID groupId, UpdateGroup command) {
		GroupEntity group = requireGroup(groupId);
		List<String> changed = new ArrayList<>();
		if (!Objects.equals(group.getName(), command.name())) {
			changed.add("name");
		}
		if (!Objects.equals(group.getDescription(), command.description())) {
			changed.add("description");
		}
		group.update(command.name(), command.description(), Timestamps.now());
		Group saved = toGroup(groupRepository.save(group));
		auditSupport.record(
				AuditActions.SUBJECT_GROUP,
				saved.id().toString(),
				AuditActions.GROUP_UPDATED,
				changed,
				null);
		return saved;
	}

	@Override
	@Transactional
	public Group retire(UUID groupId) {
		GroupEntity group = requireGroup(groupId);
		group.retire(Timestamps.now());
		Group saved = toGroup(groupRepository.save(group));
		auditSupport.record(
				AuditActions.SUBJECT_GROUP,
				saved.id().toString(),
				AuditActions.GROUP_RETIRED,
				List.of("status"),
				null);
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> listMembers(UUID groupId) {
		requireGroup(groupId);
		List<UUID> userIds = userGroupRepository.findByGroupId(groupId).stream()
				.map(UserGroupEntity::getUserId)
				.toList();
		if (userIds.isEmpty()) {
			return List.of();
		}
		return userRepository.findAllById(userIds).stream()
				.map(userMapper::toUser)
				.sorted(Comparator.comparing(User::familyName).thenComparing(User::givenName))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> listRoles(UUID groupId) {
		requireGroup(groupId);
		List<UUID> roleIds = groupRoleRepository.findByGroupId(groupId).stream()
				.map(GroupRoleEntity::getRoleId)
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
	public List<Role> assignRole(UUID groupId, UUID roleId) {
		GroupEntity group = requireGroup(groupId);
		if (group.getStatus() == GroupStatus.RETIRED) {
			throw new GroupRetiredException(groupId);
		}
		RoleEntity role = requireRole(roleId);
		if (role.getStatus() == RoleStatus.RETIRED) {
			throw new RoleRetiredException(roleId);
		}
		if (!groupRoleRepository.existsByGroupIdAndRoleId(groupId, roleId)) {
			groupRoleRepository.save(new GroupRoleEntity(groupId, roleId, Timestamps.now()));
			auditSupport.record(
					AuditActions.SUBJECT_GROUP,
					groupId.toString(),
					AuditActions.GROUP_ROLE_ASSIGNED,
					List.of("roles"),
					roleId.toString());
		}
		return listRoles(groupId);
	}

	@Override
	@Transactional
	public List<Role> removeRole(UUID groupId, UUID roleId) {
		requireGroup(groupId);
		boolean existed = groupRoleRepository.existsByGroupIdAndRoleId(groupId, roleId);
		groupRoleRepository.deleteByGroupIdAndRoleId(groupId, roleId);
		if (existed) {
			auditSupport.record(
					AuditActions.SUBJECT_GROUP,
					groupId.toString(),
					AuditActions.GROUP_ROLE_REMOVED,
					List.of("roles"),
					roleId.toString());
		}
		return listRoles(groupId);
	}

	private GroupEntity requireGroup(UUID groupId) {
		return groupRepository.findById(groupId)
				.orElseThrow(() -> new UnknownGroupException(groupId));
	}

	private RoleEntity requireRole(UUID roleId) {
		return roleRepository.findById(roleId)
				.orElseThrow(() -> new UnknownRoleException(roleId));
	}

	static Group toGroup(GroupEntity entity) {
		return new Group(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
