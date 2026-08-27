package com.acmeco.reference.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.framework.audit.AuditActorResolver;
import com.oneilsoftware.sbp.framework.audit.AuditEvent;
import com.oneilsoftware.sbp.framework.audit.AuditRecorder;
import com.oneilsoftware.sbp.module.usermaintenance.api.User;
import com.oneilsoftware.sbp.module.usermaintenance.api.UserOperations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acme-owned local credential management. It stores only one-way password hashes and failed-attempt
 * accounting, keyed by the stable SBP User id, and never exposes password material through any User
 * domain API. The reusable User Maintenance module remains authentication-free.
 */
@Service
public class AcmeCredentialService {

	static final int MAX_FAILED_ATTEMPTS = 5;

	private static final String SUBJECT_USER = "USER";
	private static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
	private static final String CREDENTIAL_RESET = "CREDENTIAL_RESET";
	private static final String ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED";

	private final AcmeCredentialRepository credentialRepository;
	private final UserOperations userOperations;
	private final PasswordEncoder passwordEncoder;
	private final AcmePasswordPolicy passwordPolicy;
	private final AuditRecorder auditRecorder;
	private final AuditActorResolver actorResolver;

	public AcmeCredentialService(
			AcmeCredentialRepository credentialRepository,
			UserOperations userOperations,
			PasswordEncoder passwordEncoder,
			AcmePasswordPolicy passwordPolicy,
			AuditRecorder auditRecorder,
			AuditActorResolver actorResolver) {
		this.credentialRepository = credentialRepository;
		this.userOperations = userOperations;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.auditRecorder = auditRecorder;
		this.actorResolver = actorResolver;
	}

	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		User user = userOperations.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("No such user"));
		AcmeCredentialEntity credential = credentialRepository.findById(user.id())
				.orElseThrow(() -> new UsernameNotFoundException("No credential for user"));
		return new AcmeUserDetails(
				user.username(),
				credential.getPasswordHash(),
				credential.getFailedAttempts() < MAX_FAILED_ATTEMPTS,
				credential.isPasswordChangeRequired(),
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Transactional
	public void ensureCredential(UUID userId, String rawPassword) {
		if (credentialRepository.findById(userId).isEmpty()) {
			credentialRepository.save(new AcmeCredentialEntity(
					userId, passwordEncoder.encode(rawPassword), 0, Instant.now()));
		}
	}

	@Transactional
	public void changePassword(UUID userId, String currentPassword, String newPassword) {
		passwordPolicy.validate(newPassword);
		AcmeCredentialEntity credential = requireCredential(userId);
		if (!passwordEncoder.matches(currentPassword, credential.getPasswordHash())) {
			throw new InvalidCurrentPasswordException();
		}
		credential.setPassword(passwordEncoder.encode(newPassword), Instant.now());
		credentialRepository.save(credential);
		audit(PASSWORD_CHANGED, userId);
	}

	@Transactional
	public void resetPassword(UUID userId, String newPassword, boolean requireChange) {
		passwordPolicy.validate(newPassword);
		AcmeCredentialEntity credential = credentialRepository.findById(userId)
				.orElseGet(() -> new AcmeCredentialEntity(userId, null, 0, Instant.now()));
		credential.setPassword(passwordEncoder.encode(newPassword), Instant.now());
		credential.setPasswordChangeRequired(requireChange, Instant.now());
		credentialRepository.save(credential);
		audit(CREDENTIAL_RESET, userId);
	}

	@Transactional
	public void unlock(UUID userId) {
		credentialRepository.findById(userId).ifPresent(credential -> {
			credential.resetFailures(Instant.now());
			credentialRepository.save(credential);
			audit(ACCOUNT_UNLOCKED, userId);
		});
	}

	@Transactional(readOnly = true)
	public AcmeCredentialStatus status(UUID userId) {
		return credentialRepository.findById(userId)
				.map(credential -> new AcmeCredentialStatus(
						true,
						credential.getFailedAttempts() >= MAX_FAILED_ATTEMPTS,
						credential.getFailedAttempts(),
						credential.isPasswordChangeRequired()))
				.orElseGet(() -> new AcmeCredentialStatus(false, false, 0, false));
	}

	@Transactional
	public void recordFailedLogin(String username) {
		userOperations.findByUsername(username).ifPresent(user ->
				credentialRepository.findById(user.id()).ifPresent(credential -> {
					credential.recordFailure(Instant.now());
					credentialRepository.save(credential);
				}));
	}

	@Transactional
	public void recordSuccessfulLogin(String username) {
		userOperations.findByUsername(username).ifPresent(user ->
				credentialRepository.findById(user.id()).ifPresent(credential -> {
					if (credential.getFailedAttempts() > 0) {
						credential.resetFailures(Instant.now());
						credentialRepository.save(credential);
					}
				}));
	}

	private AcmeCredentialEntity requireCredential(UUID userId) {
		return credentialRepository.findById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("No credential for user"));
	}

	private void audit(String action, UUID userId) {
		auditRecorder.record(new AuditEvent(
				actorResolver.currentActor(),
				SUBJECT_USER,
				userId.toString(),
				action,
				List.of("password"),
				null,
				Instant.now()));
	}
}
