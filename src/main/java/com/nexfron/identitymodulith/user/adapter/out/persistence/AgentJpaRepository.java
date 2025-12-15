package com.nexfron.identitymodulith.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentJpaRepository extends JpaRepository<AgentJpaEntity, String> {

    Optional<AgentJpaEntity> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<AgentJpaEntity> findByDeptId(String deptId);

    List<AgentJpaEntity> findByDeptIdAndStatus(String deptId, String status);

    List<AgentJpaEntity> findByStatus(String status);
}
