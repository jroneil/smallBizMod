package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRoleRepository extends JpaRepository<GroupRoleEntity, GroupRoleId> {

	List<GroupRoleEntity> findByGroupId(UUID groupId);

	boolean existsByGroupIdAndRoleId(UUID groupId, UUID roleId);

	void deleteByGroupIdAndRoleId(UUID groupId, UUID roleId);
}
