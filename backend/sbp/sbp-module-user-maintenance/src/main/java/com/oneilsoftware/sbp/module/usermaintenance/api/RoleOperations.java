package com.oneilsoftware.sbp.module.usermaintenance.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleOperations {

	Role create(CreateRole command);

	List<Role> list();

	Optional<Role> findById(UUID roleId);

	Role update(UUID roleId, UpdateRole command);

	Role retire(UUID roleId);
}
