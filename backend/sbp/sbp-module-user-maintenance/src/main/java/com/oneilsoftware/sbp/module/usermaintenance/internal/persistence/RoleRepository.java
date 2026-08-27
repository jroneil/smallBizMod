package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

	boolean existsByName(String name);

	List<RoleEntity> findAllByOrderByNameAsc();
}
