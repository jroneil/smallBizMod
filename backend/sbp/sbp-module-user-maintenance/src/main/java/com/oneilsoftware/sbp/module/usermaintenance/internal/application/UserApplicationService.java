package com.oneilsoftware.sbp.module.usermaintenance.internal.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.AuditActions;
import com.oneilsoftware.sbp.module.usermaintenance.api.BulkCreateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.BulkUserImportError;
import com.oneilsoftware.sbp.module.usermaintenance.api.BulkUserImportResult;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.Group;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownGroupException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownUserException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserFilter;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.UsernameAlreadyExistsException;
import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.FieldCipher;
import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.SearchTokenGenerator;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRoleEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRoleRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserGroupEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserGroupRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserRoleEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserRoleRepository;
import org.springframework.transaction.annotation.Transactional;

public class UserApplicationService implements UserOperations {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final GroupRepository groupRepository;
	private final UserGroupRepository userGroupRepository;
	private final GroupRoleRepository groupRoleRepository;
	private final AuditSupport auditSupport;
	private final FieldCipher cipher;
	private final SearchTokenGenerator tokenGenerator;
	private final UserMapper userMapper;

	public UserApplicationService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			GroupRepository groupRepository,
			UserGroupRepository userGroupRepository,
			GroupRoleRepository groupRoleRepository,
			AuditSupport auditSupport,
			FieldCipher cipher,
			SearchTokenGenerator tokenGenerator,
			UserMapper userMapper) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.groupRepository = groupRepository;
		this.userGroupRepository = userGroupRepository;
		this.groupRoleRepository = groupRoleRepository;
		this.auditSupport = auditSupport;
		this.cipher = cipher;
		this.tokenGenerator = tokenGenerator;
		this.userMapper = userMapper;
	}

	@Override
	@Transactional
	public User create(CreateUser command) {
		if (userRepository.existsByUsername(command.username())) {
			throw new UsernameAlreadyExistsException(command.username());
		}

		var now = Timestamps.now();
		UserEntity user = new UserEntity(
				UUID.randomUUID(),
				command.username(),
				cipher.encrypt(command.email()),
				cipher.encrypt(command.givenName()),
				cipher.encrypt(command.familyName()),
				cipher.encrypt(command.phone()),
				UserStatus.ACTIVE,
				now,
				now);
		applySearchTokens(user, command.email(), command.givenName(), command.familyName());

		User saved = userMapper.toUser(userRepository.save(user));
		auditSupport.record(
				AuditActions.SUBJECT_USER,
				saved.id().toString(),
				AuditActions.USER_CREATED,
				List.of("username", "email", "givenName", "familyName", "phone", "status"),
				null);
		return saved;
	}

	@Override
	@Transactional
	public BulkUserImportResult bulkCreate(List<BulkCreateUser> commands) {
		List<User> created = new ArrayList<>();
		List<BulkUserImportError> errors = new ArrayList<>();
		Set<String> seenUsernames = new HashSet<>();

		if (commands == null || commands.isEmpty()) {
			return new BulkUserImportResult(created, errors);
		}

		for (int index = 0; index < commands.size(); index++) {
			BulkCreateUser raw = commands.get(index);
			try {
				CreateUser command = new CreateUser(
						raw.username(),
						raw.email(),
						raw.givenName(),
						raw.familyName(),
						raw.phone());
				if (userRepository.existsByUsername(command.username()) || !seenUsernames.add(command.username())) {
					errors.add(new BulkUserImportError(index, "Username already exists: " + command.username()));
					continue;
				}
				created.add(create(command));
			}
			catch (IllegalArgumentException exception) {
				errors.add(new BulkUserImportError(index, exception.getMessage()));
			}
		}

		return new BulkUserImportResult(created, errors);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> search(UserFilter filter) {
		String token = null;
		if (filter.query() != null) {
			token = tokenGenerator.tokenFor(filter.query());
		}
		return userRepository.search(
				filter.query(),
				token,
				filter.status(),
				filter.roleId(),
				filter.groupId()).stream()
				.map(userMapper::toUser)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findById(UUID userId) {
		return userRepository.findById(userId)
				.map(userMapper::toUser);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username)
				.map(userMapper::toUser);
	}

	@Override
	@Transactional
	public User update(UUID userId, UpdateUser command) {
		UserEntity user = requireUser(userId);
		List<String> changed = new ArrayList<>();
		if (!Objects.equals(cipher.decrypt(user.getEmail()), command.email())) {
			changed.add("email");
		}
		if (!Objects.equals(cipher.decrypt(user.getGivenName()), command.givenName())) {
			changed.add("givenName");
		}
		if (!Objects.equals(cipher.decrypt(user.getFamilyName()), command.familyName())) {
			changed.add("familyName");
		}
		if (!Objects.equals(cipher.decrypt(user.getPhone()), command.phone())) {
			changed.add("phone");
		}
		user.updateProfile(
				cipher.encrypt(command.email()),
				cipher.encrypt(command.givenName()),
				cipher.encrypt(command.familyName()),
				cipher.encrypt(command.phone()),
				Timestamps.now());
		applySearchTokens(user, command.email(), command.givenName(), command.familyName());
		User saved = userMapper.toUser(userRepository.save(user));
		auditSupport.record(
				AuditActions.SUBJECT_USER,
				saved.id().toString(),
				AuditActions.USER_UPDATED,
				changed,
				null);
		return saved;
	}

	@Override
	@Transactional
	public User activate(UUID userId) {
		return changeStatus(userId, UserStatus.ACTIVE);
	}

	@Override
	@Transactional
	public User deactivate(UUID userId) {
		return changeStatus(userId, UserStatus.INACTIVE);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> listRoles(UUID userId) {
		requireUser(userId);
		List<UUID> roleIds = userRoleRepository.findByUserId(userId).stream()
				.map(UserRoleEntity::getRoleId)
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
	public List<Role> assignRole(UUID userId, UUID roleId) {
		requireUser(userId);
		RoleEntity role = requireRole(roleId);
		if (role.getStatus() == RoleStatus.RETIRED) {
			throw new RoleRetiredException(roleId);
		}
		if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
			userRoleRepository.save(new UserRoleEntity(userId, roleId, Timestamps.now()));
			auditSupport.record(
					AuditActions.SUBJECT_USER,
					userId.toString(),
					AuditActions.ROLE_ASSIGNED,
					List.of("roles"),
					roleId.toString());
		}
		return listRoles(userId);
	}

	@Override
	@Transactional
	public List<Role> removeRole(UUID userId, UUID roleId) {
		requireUser(userId);
		boolean existed = userRoleRepository.existsByUserIdAndRoleId(userId, roleId);
		userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
		if (existed) {
			auditSupport.record(
					AuditActions.SUBJECT_USER,
					userId.toString(),
					AuditActions.ROLE_REMOVED,
					List.of("roles"),
					roleId.toString());
		}
		return listRoles(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> listEffectiveRoles(UUID userId) {
		requireUser(userId);
		Set<UUID> roleIds = new LinkedHashSet<>();

		userRoleRepository.findByUserId(userId).stream()
				.map(UserRoleEntity::getRoleId)
				.forEach(roleIds::add);

		List<UUID> groupIds = userGroupRepository.findByUserId(userId).stream()
				.map(UserGroupEntity::getGroupId)
				.toList();
		Set<UUID> activeGroupIds = new HashSet<>();
		if (!groupIds.isEmpty()) {
			for (GroupEntity group : groupRepository.findAllById(groupIds)) {
				if (group.getStatus() == GroupStatus.ACTIVE) {
					activeGroupIds.add(group.getId());
				}
			}
		}
		for (UUID groupId : activeGroupIds) {
			groupRoleRepository.findByGroupId(groupId).stream()
					.map(GroupRoleEntity::getRoleId)
					.forEach(roleIds::add);
		}

		if (roleIds.isEmpty()) {
			return List.of();
		}
		return roleRepository.findAllById(roleIds).stream()
				.filter(role -> role.getStatus() == RoleStatus.ACTIVE)
				.map(RoleApplicationService::toRole)
				.sorted(Comparator.comparing(Role::name))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Group> listGroups(UUID userId) {
		requireUser(userId);
		List<UUID> groupIds = userGroupRepository.findByUserId(userId).stream()
				.map(UserGroupEntity::getGroupId)
				.toList();
		if (groupIds.isEmpty()) {
			return List.of();
		}
		return groupRepository.findAllById(groupIds).stream()
				.map(GroupApplicationService::toGroup)
				.sorted(Comparator.comparing(Group::name))
				.toList();
	}

	@Override
	@Transactional
	public List<Group> assignGroup(UUID userId, UUID groupId) {
		requireUser(userId);
		GroupEntity group = requireGroup(groupId);
		if (group.getStatus() == GroupStatus.RETIRED) {
			throw new GroupRetiredException(groupId);
		}
		if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
			userGroupRepository.save(new UserGroupEntity(userId, groupId, Timestamps.now()));
			auditSupport.record(
					AuditActions.SUBJECT_USER,
					userId.toString(),
					AuditActions.GROUP_ASSIGNED,
					List.of("groups"),
					groupId.toString());
		}
		return listGroups(userId);
	}

	@Override
	@Transactional
	public List<Group> removeGroup(UUID userId, UUID groupId) {
		requireUser(userId);
		boolean existed = userGroupRepository.existsByUserIdAndGroupId(userId, groupId);
		userGroupRepository.deleteByUserIdAndGroupId(userId, groupId);
		if (existed) {
			auditSupport.record(
					AuditActions.SUBJECT_USER,
					userId.toString(),
					AuditActions.GROUP_REMOVED,
					List.of("groups"),
					groupId.toString());
		}
		return listGroups(userId);
	}

	private User changeStatus(UUID userId, UserStatus status) {
		UserEntity user = requireUser(userId);
		user.changeStatus(status, Timestamps.now());
		User saved = userMapper.toUser(userRepository.save(user));
		auditSupport.record(
				AuditActions.SUBJECT_USER,
				saved.id().toString(),
				status == UserStatus.ACTIVE ? AuditActions.USER_ACTIVATED : AuditActions.USER_DEACTIVATED,
				List.of("status"),
				null);
		return saved;
	}

	private void applySearchTokens(UserEntity entity, String email, String givenName, String familyName) {
		entity.updateSearchTokens(
				tokenGenerator.tokenFor(email),
				tokenGenerator.tokenFor(givenName),
				tokenGenerator.tokenFor(familyName),
				tokenGenerator.tokenFor(givenName + " " + familyName));
	}

	private UserEntity requireUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new UnknownUserException(userId));
	}

	private RoleEntity requireRole(UUID roleId) {
		return roleRepository.findById(roleId)
				.orElseThrow(() -> new UnknownRoleException(roleId));
	}

	private GroupEntity requireGroup(UUID groupId) {
		return groupRepository.findById(groupId)
				.orElseThrow(() -> new UnknownGroupException(groupId));
	}
}
