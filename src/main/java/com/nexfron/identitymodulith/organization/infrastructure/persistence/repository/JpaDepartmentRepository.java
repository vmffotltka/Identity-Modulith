package com.nexfron.identitymodulith.organization.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.organization.domain.model.DepartmentType;
import com.nexfron.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
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
 *
 * ID 타입:
 * - deptId는 UUID 문자열 (String, VARCHAR(36))
 * - 모든 메서드의 deptId 파라미터는 String 타입
 */
public interface JpaDepartmentRepository extends JpaRepository<DepartmentEntity, String> {

    /**
     * [트리 탐색]
     * 특정 테넌트에서 orgPath prefix 로 시작하는 모든 하위 부서 조회
     *
     * 예)
     *  orgPathPrefix = "/550e8400-e29b-41d4-a716-446655440000/550e8400-e29b-41d4-a716-446655440001"
     *   → "/550e8400-...", "/550e8400-.../550e8400-..." 등으로 시작하는 모든 부서
     *
     * 사용처:
     * - 부서 이동 시 하위 부서 orgPath 재계산
     * - 조직도 트리 구성
     *
     * @param tenantId 테넌트 ID
     * @param orgPathPrefix orgPath 접두사 (예: "/550e8400-...")
     * @return 조건에 맞는 부서 리스트
     */
    List<DepartmentEntity> findByTenantIdAndOrgPathStartsWith(String tenantId, String orgPathPrefix);

    /**
     * 특정 테넌트의 전체 부서 목록 조회
     *
     * 사용처:
     * - 전체 조직도 트리 구성
     * - 스코프 기반 조회 전 단계
     *
     * @param tenantId 테넌트 ID
     * @return 해당 테넌트의 모든 부서 리스트
     *
     * 주의:
     * - 데이터 규모가 커지면 IN 조회로 대체 고려
     */
    List<DepartmentEntity> findAllByTenantId(String tenantId);

    /**
     * 특정 부서를 parent로 가지는 하위 부서 존재 여부
     *
     * 사용처:
     * - 부서 삭제 시 "하위 부서 존재하면 삭제 불가" 정책 검증
     *
     * @param parent 대상 부서
     * @return true: 하위 부서 존재, false: 없음
     */
    boolean existsByParent(DepartmentEntity parent);

    /**
     * [권장]
     * tenantId를 포함한 단건 조회
     *
     * 목적:
     * - Service 계층에서 tenant 필터 누락 사고 방지
     * - 멀티테넌시 환경에서 데이터 격리 보장
     *
     * @param deptId 부서 ID (UUID 문자열)
     * @param tenantId 테넌트 ID
     * @return 해당 부서 (없으면 Empty)
     */
    Optional<DepartmentEntity> findByDeptIdAndTenantId(String deptId, String tenantId);


    /**
     * [부서 검색]
     * 키워드로 부서명 검색 (부분 일치)
     *
     * 사용처:
     * - 부서 검색 기능
     * - 부서명으로 빠르게 조회
     *
     * @param tenantId 테넌트 ID
     * @param keyword 검색 키워드 (부서명에 포함되어야 함)
     * @return 키워드가 포함된 부서 리스트
     */
    List<DepartmentEntity> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String keyword);

    /**
     * [부서 검색]
     * 특정 깊이(depth)의 부서 조회
     *
     * 사용처:
     * - 계층별 부서 조회
     * - 특정 레벨의 부서만 필터링
     *
     * @param tenantId 테넌트 ID
     * @param depth 깊이 (0: 루트, 1: 1단계, 2: 2단계...)
     * @return 해당 깊이의 부서 리스트
     */
    List<DepartmentEntity> findByTenantIdAndDepth(String tenantId, Integer depth);

    /**
     * [부서 검색]
     * 특정 타입의 부서 조회
     *
     * 사용처:
     * - 타입별 부서 필터링
     * - 예: TEAM 타입의 부서만 조회
     *
     * @param tenantId 테넌트 ID
     * @param type 부서 타입 (COMPANY, DIVISION, TEAM, GROUP, CUSTOM)
     * @return 해당 타입의 부서 리스트
     */
    List<DepartmentEntity> findByTenantIdAndType(String tenantId, DepartmentType type);
}
