package com.oneilsoftware.sbp.module.usermaintenance.internal.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.BulkCreateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.BulkUserImportResult;
import com.oneilsoftware.sbp.module.usermaintenance.api.CreateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.Group;
import com.oneilsoftware.sbp.module.usermaintenance.api.GroupRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.Role;
import com.oneilsoftware.sbp.module.usermaintenance.api.RoleRetiredException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownGroupException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownRoleException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UnknownUserException;
import com.oneilsoftware.sbp.module.usermaintenance.api.UpdateUser;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserFilter;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserStatus;
import com.oneilsoftware.sbp.module.usermaintenance.api.UsernameAlreadyExistsException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserOperations userOperations;

	public UserController(UserOperations userOperations) {
		this.userOperations = userOperations;
	}

	@PostMapping
	public ResponseEntity<User> create(@Valid @RequestBody CreateUserRequest request) {
		User user = userOperations.create(new CreateUser(
				request.username(),
				request.email(),
				request.givenName(),
				request.familyName(),
				request.phone()));
		return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
	}

	@PostMapping("/bulk")
	public BulkUserImportResult bulk(@RequestBody List<BulkCreateUserRequest> requests) {
		List<BulkCreateUser> commands = requests.stream()
				.map(request -> new BulkCreateUser(
						request.username(),
						request.email(),
						request.givenName(),
						request.familyName(),
						request.phone()))
				.toList();
		return userOperations.bulkCreate(commands);
	}

	@GetMapping("/export")
	public List<User> export() {
		return userOperations.search(new UserFilter(null, null, null, null)).stream()
				.map(UserController::mask)
				.toList();
	}

	@GetMapping
	public List<User> list(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) UserStatus status,
			@RequestParam(required = false) UUID roleId,
			@RequestParam(required = false) UUID groupId) {
		return userOperations.search(new UserFilter(query, status, roleId, groupId)).stream()
				.map(UserController::mask)
				.toList();
	}

	@GetMapping("/{userId}")
	public ResponseEntity<User> get(@PathVariable UUID userId) {
		return ResponseEntity.of(userOperations.findById(userId));
	}

	@PutMapping("/{userId}")
	public User update(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
		return userOperations.update(userId, new UpdateUser(
				request.email(),
				request.givenName(),
				request.familyName(),
				request.phone()));
	}

	@PostMapping("/{userId}/deactivate")
	public User deactivate(@PathVariable UUID userId) {
		return userOperations.deactivate(userId);
	}

	@PostMapping("/{userId}/activate")
	public User activate(@PathVariable UUID userId) {
		return userOperations.activate(userId);
	}

	@GetMapping("/{userId}/roles")
	public List<Role> listRoles(@PathVariable UUID userId) {
		return userOperations.listRoles(userId);
	}

	@GetMapping("/{userId}/effective-roles")
	public List<Role> listEffectiveRoles(@PathVariable UUID userId) {
		return userOperations.listEffectiveRoles(userId);
	}

	@PostMapping("/{userId}/roles/{roleId}")
	public List<Role> assignRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
		return userOperations.assignRole(userId, roleId);
	}

	@DeleteMapping("/{userId}/roles/{roleId}")
	public List<Role> removeRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
		return userOperations.removeRole(userId, roleId);
	}

	@GetMapping("/{userId}/groups")
	public List<Group> listGroups(@PathVariable UUID userId) {
		return userOperations.listGroups(userId);
	}

	@PostMapping("/{userId}/groups/{groupId}")
	public List<Group> assignGroup(@PathVariable UUID userId, @PathVariable UUID groupId) {
		return userOperations.assignGroup(userId, groupId);
	}

	@DeleteMapping("/{userId}/groups/{groupId}")
	public List<Group> removeGroup(@PathVariable UUID userId, @PathVariable UUID groupId) {
		return userOperations.removeGroup(userId, groupId);
	}

	@ExceptionHandler({UnknownUserException.class, UnknownRoleException.class, UnknownGroupException.class})
	ResponseEntity<Void> unknownUserRoleOrGroup() {
		return ResponseEntity.notFound().build();
	}

	@ExceptionHandler({UsernameAlreadyExistsException.class, RoleRetiredException.class, GroupRetiredException.class})
	ResponseEntity<Void> conflictingState() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}

	/**
	 * Masks personal contact fields for the list/search view (NFR-UM-04). The detail endpoint
	 * returns full values, which is the explicit reveal action.
	 */
	private static User mask(User user) {
		return new User(
				user.id(),
				user.username(),
				maskEmail(user.email()),
				user.givenName(),
				user.familyName(),
				maskPhone(user.phone()),
				user.status(),
				user.createdAt(),
				user.updatedAt());
	}

	private static String maskEmail(String email) {
		if (email == null) {
			return null;
		}
		int at = email.indexOf('@');
		if (at <= 0) {
			return "***";
		}
		return email.substring(0, 1) + "***" + email.substring(at);
	}

	private static String maskPhone(String phone) {
		if (phone == null) {
			return null;
		}
		if (phone.length() <= 4) {
			return "***";
		}
		return "***" + phone.substring(phone.length() - 4);
	}
}
