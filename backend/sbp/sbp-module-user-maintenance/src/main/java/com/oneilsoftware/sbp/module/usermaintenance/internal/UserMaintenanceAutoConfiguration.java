package com.oneilsoftware.sbp.module.usermaintenance.internal;

import com.oneilsoftware.sbp.framework.audit.AuditActorResolver;
import com.oneilsoftware.sbp.framework.audit.AuditRecorder;
import com.oneilsoftware.sbp.module.usermaintenance.api.AuditOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.PermissionOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.AuditApplicationService;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.AuditSupport;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.GroupApplicationService;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.PermissionApplicationService;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.RoleApplicationService;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.UserApplicationService;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.UserMaintenanceAuditRecorder;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.UserMapper;
import com.oneilsoftware.sbp.module.usermaintenance.internal.application.UserSearchTokenBackfill;
import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.AesGcmFieldCipher;
import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.FieldCipher;
import com.oneilsoftware.sbp.module.usermaintenance.internal.encryption.SearchTokenGenerator;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.GroupRoleRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.PermissionRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RolePermissionRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.RoleRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserAuditEventRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserEntity;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserGroupRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.persistence.UserRoleRepository;
import com.oneilsoftware.sbp.module.usermaintenance.internal.web.AuditController;
import com.oneilsoftware.sbp.module.usermaintenance.internal.web.GroupController;
import com.oneilsoftware.sbp.module.usermaintenance.internal.web.PermissionController;
import com.oneilsoftware.sbp.module.usermaintenance.internal.web.RoleController;
import com.oneilsoftware.sbp.module.usermaintenance.internal.web.UserController;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EnableConfigurationProperties(UserMaintenanceProperties.class)
@EntityScan(basePackageClasses = UserEntity.class)
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
public class UserMaintenanceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AuditActorResolver.class)
	AuditActorResolver auditActorResolver() {
		return () -> "system";
	}

	@Bean
	AuditRecorder auditRecorder(UserAuditEventRepository userAuditEventRepository) {
		return new UserMaintenanceAuditRecorder(userAuditEventRepository);
	}

	@Bean
	AuditSupport auditSupport(AuditRecorder auditRecorder, AuditActorResolver auditActorResolver) {
		return new AuditSupport(auditRecorder, auditActorResolver);
	}

	@Bean
	FieldCipher fieldCipher(UserMaintenanceProperties properties) {
		return new AesGcmFieldCipher(properties.encryptionKey());
	}

	@Bean
	SearchTokenGenerator searchTokenGenerator(UserMaintenanceProperties properties) {
		return new SearchTokenGenerator(properties.searchKey());
	}

	@Bean
	ApplicationRunner userSearchTokenBackfill(
			UserRepository userRepository,
			FieldCipher fieldCipher,
			SearchTokenGenerator searchTokenGenerator) {
		return new UserSearchTokenBackfill(userRepository, fieldCipher, searchTokenGenerator);
	}

	@Bean
	UserMapper userMapper(FieldCipher fieldCipher) {
		return new UserMapper(fieldCipher);
	}

	@Bean
	UserOperations userOperations(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			GroupRepository groupRepository,
			UserGroupRepository userGroupRepository,
			GroupRoleRepository groupRoleRepository,
			AuditSupport auditSupport,
			FieldCipher fieldCipher,
			SearchTokenGenerator searchTokenGenerator,
			UserMapper userMapper) {
		return new UserApplicationService(
				userRepository,
				roleRepository,
				userRoleRepository,
				groupRepository,
				userGroupRepository,
				groupRoleRepository,
				auditSupport,
				fieldCipher,
				searchTokenGenerator,
				userMapper);
	}

	@Bean
	RoleOperations roleOperations(RoleRepository roleRepository, AuditSupport auditSupport) {
		return new RoleApplicationService(roleRepository, auditSupport);
	}

	@Bean
	GroupOperations groupOperations(
			GroupRepository groupRepository,
			UserGroupRepository userGroupRepository,
			UserRepository userRepository,
			GroupRoleRepository groupRoleRepository,
			RoleRepository roleRepository,
			AuditSupport auditSupport,
			UserMapper userMapper) {
		return new GroupApplicationService(
				groupRepository,
				userGroupRepository,
				userRepository,
				groupRoleRepository,
				roleRepository,
				auditSupport,
				userMapper);
	}

	@Bean
	AuditOperations auditOperations(UserAuditEventRepository userAuditEventRepository) {
		return new AuditApplicationService(userAuditEventRepository);
	}

	@Bean
	PermissionOperations permissionOperations(
			PermissionRepository permissionRepository,
			RolePermissionRepository rolePermissionRepository,
			RoleRepository roleRepository) {
		return new PermissionApplicationService(permissionRepository, rolePermissionRepository, roleRepository);
	}

	@Bean
	UserController userController(UserOperations userOperations) {
		return new UserController(userOperations);
	}

	@Bean
	RoleController roleController(RoleOperations roleOperations) {
		return new RoleController(roleOperations);
	}

	@Bean
	GroupController groupController(GroupOperations groupOperations) {
		return new GroupController(groupOperations);
	}

	@Bean
	AuditController auditController(AuditOperations auditOperations) {
		return new AuditController(auditOperations);
	}

	@Bean
	PermissionController permissionController(PermissionOperations permissionOperations) {
		return new PermissionController(permissionOperations);
	}
}
