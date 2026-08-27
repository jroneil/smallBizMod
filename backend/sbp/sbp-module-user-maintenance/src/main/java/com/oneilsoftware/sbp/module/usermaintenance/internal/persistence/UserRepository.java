package com.oneilsoftware.sbp.module.usermaintenance.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.oneilsoftware.sbp.module.usermaintenance.api.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	boolean existsByUsername(String username);

	Optional<UserEntity> findByUsername(String username);

	List<UserEntity> findByEmailTokenIsNull();

	@Query("""
			select u from UserEntity u
			where (:query is null
				or lower(u.username) like concat('%', lower(cast(:query as string)), '%')
				or u.emailToken = :token
				or u.givenNameToken = :token
				or u.familyNameToken = :token
				or u.fullNameToken = :token)
			  and (:status is null or u.status = :status)
			  and (:roleId is null or exists (
			      select 1 from UserRoleEntity ur where ur.userId = u.id and ur.roleId = :roleId))
			  and (:groupId is null or exists (
			      select 1 from UserGroupEntity ug where ug.userId = u.id and ug.groupId = :groupId))
			order by lower(u.familyName), lower(u.givenName), u.username
			""")
	List<UserEntity> search(
			@Param("query") String query,
			@Param("token") String token,
			@Param("status") UserStatus status,
			@Param("roleId") UUID roleId,
			@Param("groupId") UUID groupId);
}
