package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroupEntity, UserGroupId> {

	List<UserGroupEntity> findByUserId(UUID userId);

	List<UserGroupEntity> findByGroupId(UUID groupId);

	boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);

	void deleteByUserIdAndGroupId(UUID userId, UUID groupId);
}
