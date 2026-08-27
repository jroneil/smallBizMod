package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<GroupEntity, UUID> {

	boolean existsByName(String name);

	List<GroupEntity> findAllByOrderByNameAsc();
}
