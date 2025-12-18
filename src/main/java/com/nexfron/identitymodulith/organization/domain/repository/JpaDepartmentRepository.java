package com.nexfron.identitymodulith.organization.domain.repository;

import com.nexfron.identitymodulith.organization.domain.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * JpaDepartmentRepository
 *
 * 역할:
 * - Department 엔티티의 영속화(JPA) 담당
 * - 조직 트리 탐색을 위한 orgPath 기반 조회 제공
 *
 * 설계 원칙(팀 공통):
 * 1) 모든 조회는 tenantId 기준으로 격리한다.
 * 2) 트리 구조 탐색은 orgPath 문자열 prefix 전략을 사용한다.
 * 3) Service 계층에서 tenant 필터 실수를 막기 위해,
 *    가능한 한 tenant 포함 조회 메서드를 제공한다.
 *
 * 제약 사항:
 * - DB 스키마 변경 불가
 * - 따라서 JPA 메서드 네이밍과 서비스 로직으로 안전장치를 만든다.
 */
public interface JpaDepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * [트리 탐색]
     * 특정 테넌트에서 orgPath prefix 로 시작하는 모든 하위 부서 조회
     *
     * 예)
     *  orgPathPrefix = "/1/5"
     *   → "/1/5", "/1/5/10", "/1/5/11" ...
     *
     * 사용처:
     * - 부서 이동 시 하위 부서 orgPath 재계산
     */
    List<Department> findByTenantIdAndOrgPathStartsWith(String tenantId, String orgPathPrefix);

    /**
     * 특정 테넌트의 전체 부서 목록 조회
     *
     * 사용처:
     * - 전체 조직도 트리 구성
     * - 스코프 기반 조회 전 단계
     *
     * 주의:
     * - 데이터 규모가 커지면 IN 조회로 대체 고려
     */
    List<Department> findAllByTenantId(String tenantId);

    /**
     * 특정 부서를 parent 로 가지는 하위 부서 존재 여부
     *
     * 사용처:
     * - 부서 삭제 시 "하위 부서 존재하면 삭제 불가" 정책 검증
     */
    boolean existsByParent(Department parent);
    boolean existsByTenantIdAndParent_DeptId(String tenantId, Long parentDeptId);

    /**
     * 특정 테넌트의 최상위(root) 부서 조회
     *
     * 구현 방식:
     * - depth = 0 대신 parent IS NULL 조건 사용
     */
    List<Department> findByTenantIdAndParentIsNull(String tenantId);

    /**
     * [권장]
     * tenantId 를 포함한 단건 조회
     *
     * 목적:
     * - Service 계층에서 tenant 필터 누락 사고 방지
     */
    Optional<Department> findByDeptIdAndTenantId(Long deptId, String tenantId);

    /**
     * [성능 최적화]
     * tenant + 부서 ID 집합 기준 조회
     *
     * 사용처:
     * - RBAC 스코프 기반 조직도 트리 조회
     */
    List<Department> findAllByTenantIdAndDeptIdIn(String tenantId, List<Long> deptIds);
}
