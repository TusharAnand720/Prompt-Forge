package com.prompt_forge.reposityory;

import com.prompt_forge.entity.ProjectMember;
import com.prompt_forge.entity.ProjectMemberId;
import com.prompt_forge.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    List<ProjectMember> findByProjectId(Long projectId);

    @Query("""
            SELECT pm.projectRole FROM ProjectMember pm
            WHERE pm.id.projectId = :projectId
            AND pm.id.userId = :userId
            """)
    Optional<Role> findRoleByProjectIdAndUserId(@Param("projectId") Long projectId,
                                                @Param("userId") Long userId);


    @Query("""
            SELECT COUNT(pm) FROM ProjectMember pm
            WHERE pm.id.userId = :userId
            AND pm.projectRole = 'OWNER'
            """)
    int countProjectOwnedByUser(@Param("userId") Long userId);
}
