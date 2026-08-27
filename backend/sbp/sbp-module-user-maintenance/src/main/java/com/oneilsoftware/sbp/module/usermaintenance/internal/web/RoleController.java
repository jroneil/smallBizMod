package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.CreateRole;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleNameAlreadyExistsException;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

	private final RoleOperations roleOperations;

	public RoleController(RoleOperations roleOperations) {
		this.roleOperations = roleOperations;
	}

	@PostMapping
	public ResponseEntity<Role> create(@Valid @RequestBody CreateRoleRequest request) {
		Role role = roleOperations.create(new CreateRole(request.name(), request.description()));
		return ResponseEntity.created(URI.create("/api/roles/" + role.id())).body(role);
	}

	@GetMapping
	public List<Role> list() {
		return roleOperations.list();
	}

	@GetMapping("/{roleId}")
	public ResponseEntity<Role> get(@PathVariable UUID roleId) {
		return ResponseEntity.of(roleOperations.findById(roleId));
	}

	@PutMapping("/{roleId}")
	public Role update(@PathVariable UUID roleId, @Valid @RequestBody UpdateRoleRequest request) {
		return roleOperations.update(roleId, new UpdateRole(request.name(), request.description()));
	}

	@PostMapping("/{roleId}/retire")
	public Role retire(@PathVariable UUID roleId) {
		return roleOperations.retire(roleId);
	}

	@ExceptionHandler(UnknownRoleException.class)
	ResponseEntity<Void> unknownRole() {
		return ResponseEntity.notFound().build();
	}

	@ExceptionHandler(RoleNameAlreadyExistsException.class)
	ResponseEntity<Void> duplicateRoleName() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}
}
