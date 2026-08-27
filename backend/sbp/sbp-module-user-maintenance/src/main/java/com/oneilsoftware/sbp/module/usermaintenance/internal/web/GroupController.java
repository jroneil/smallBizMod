package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.CreateGroup;
import com.oneilsoftware.sbp.module.usermaintenance.api.Group;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupNameAlreadyExistsException;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownGroupException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateGroup;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

	private final GroupOperations groupOperations;

	public GroupController(GroupOperations groupOperations) {
		this.groupOperations = groupOperations;
	}

	@PostMapping
	public ResponseEntity<Group> create(@Valid @RequestBody CreateGroupRequest request) {
		Group group = groupOperations.create(new CreateGroup(request.name(), request.description()));
		return ResponseEntity.created(URI.create("/api/groups/" + group.id())).body(group);
	}

	@GetMapping
	public List<Group> list() {
		return groupOperations.list();
	}

	@GetMapping("/{groupId}")
	public ResponseEntity<Group> get(@PathVariable UUID groupId) {
		return ResponseEntity.of(groupOperations.findById(groupId));
	}

	@PutMapping("/{groupId}")
	public Group update(@PathVariable UUID groupId, @Valid @RequestBody UpdateGroupRequest request) {
		return groupOperations.update(groupId, new UpdateGroup(request.name(), request.description()));
	}

	@PostMapping("/{groupId}/retire")
	public Group retire(@PathVariable UUID groupId) {
		return groupOperations.retire(groupId);
	}

	@GetMapping("/{groupId}/members")
	public List<User> members(@PathVariable UUID groupId) {
		return groupOperations.listMembers(groupId);
	}

	@GetMapping("/{groupId}/roles")
	public List<Role> listRoles(@PathVariable UUID groupId) {
		return groupOperations.listRoles(groupId);
	}

	@PostMapping("/{groupId}/roles/{roleId}")
	public List<Role> assignRole(@PathVariable UUID groupId, @PathVariable UUID roleId) {
		return groupOperations.assignRole(groupId, roleId);
	}

	@DeleteMapping("/{groupId}/roles/{roleId}")
	public List<Role> removeRole(@PathVariable UUID groupId, @PathVariable UUID roleId) {
		return groupOperations.removeRole(groupId, roleId);
	}

	@ExceptionHandler({UnknownGroupException.class, UnknownRoleException.class})
	ResponseEntity<Void> unknownGroupOrRole() {
		return ResponseEntity.notFound().build();
	}

	@ExceptionHandler({GroupNameAlreadyExistsException.class, GroupRetiredException.class, RoleRetiredException.class})
	ResponseEntity<Void> conflictingState() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}
}
