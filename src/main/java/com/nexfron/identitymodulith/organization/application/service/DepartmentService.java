package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.domain.model.DepartmentType;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;

import java.util.List;
import java.util.UUID;

/**
 * 부서 관리 서비스 인터페이스
 *
 * <h2>주요 기능:</h2>
 * <ul>
 *   <li>부서 CRUD (생성, 조회, 수정, 삭제)</li>
 *   <li>부서 이동 (재조직)</li>
 *   <li>조직도 트리 조회</li>
 *   <li>Level 1 RBAC 권한 기반 접근 제어</li>
 * </ul>
 *
 * @see DepartmentServiceImpl
 * @author Identity System Team
 * @version 1.0
 */
public interface DepartmentService {

    // ============================================================
    // 부서 CRUD
    // ============================================================

    /**
     * 부서 생성
     *
     * @param tenantId 테넌트 ID
     * @param name 부서명 (필수)
     * @param type 부서 타입 (COMPANY, DIVISION, TEAM, GROUP, CUSTOM)
     * @param parentId 상위 부서 ID (null이면 루트 부서)
     * @return 생성된 부서 정보
     */
    DepartmentDto.Response createDepartment(
            String tenantId,
            String name,
            DepartmentType type,
            String parentId);

    /**
     * 부서 정보 수정 (name, type만 수정 가능)
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @param name 새 부서명 (null이면 변경 안 함)
     * @param type 새 부서 타입 (null이면 변경 안 함)
     * @return 수정된 부서 정보
     */
    DepartmentDto.Response updateDepartment(
            String tenantId,
            String deptId,
            String name,
            DepartmentType type);

    /**
     * 부서 삭제
     *
     * <h3>삭제 조건:</h3>
     * <ul>
     *   <li>하위 부서가 없어야 함</li>
     *   <li>소속 활성 직원이 없어야 함</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID
     * @param deptId 삭제할 부서 ID
     */
    void deleteDepartment(String tenantId, UUID actorUserId, String deptId);

    // ============================================================
    // 부서 상태 관리
    // ============================================================

    /**
     * 부서 비활성화
     *
     * <h3>비활성화 조건:</h3>
     * <ul>
     *   <li>활성 하위 부서가 없어야 함</li>
     *   <li>소속 활성 직원이 있으면 경고 (비활성화는 가능)</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID
     * @param deptId 비활성화할 부서 ID
     */
    void deactivateDepartment(String tenantId, UUID actorUserId, String deptId);

    /**
     * 부서 활성화
     *
     * <h3>활성화 조건:</h3>
     * <ul>
     *   <li>상위 부서가 활성 상태여야 함</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID
     * @param deptId 활성화할 부서 ID
     */
    void activateDepartment(String tenantId, UUID actorUserId, String deptId);

    // ============================================================
    // 부서 이동 (재조직)
    // ============================================================

    /**
     * 부서 이동 (상위 부서 변경)
     *
     * <h3>제약사항:</h3>
     * <ul>
     *   <li>자기 자신으로 이동 불가</li>
     *   <li>자신의 하위 부서로 이동 불가 (순환 참조 방지)</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID
     * @param userId 작업 수행 사용자 ID
     * @param deptId 이동할 부서 ID
     * @param newParentId 새 상위 부서 ID (null이면 루트로 이동)
     */
    void moveDepartment(String tenantId, UUID userId, String deptId, String newParentId);

    // ============================================================
    // 조직도 조회
    // ============================================================

    /**
     * 전체 조직도 조회 (트리 구조)
     *
     * @param tenantId 테넌트 ID
     * @return 전체 부서 목록 (org_path 정렬)
     */
    List<DepartmentDto.Response> getDepartmentTree(String tenantId);

    /**
     * 스코프 기반 조직도 조회
     * - 사용자의 역할에 따라 접근 가능한 부서만 조회
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 접근 가능한 부서 목록
     */
    List<DepartmentDto.Response> getDepartmentTreeWithinScope(String tenantId, UUID userId);

    /**
     * 부서 검색 (키워드 기반)
     *
     * @param tenantId 테넌트 ID
     * @param keyword 검색 키워드 (부서명 포함)
     * @return 검색된 부서 목록
     */
    List<DepartmentDto.Response> searchDepartments(String tenantId, String keyword);

    /**
     * 특정 깊이(depth)의 부서 조회
     *
     * @param tenantId 테넌트 ID
     * @param depth 트리 깊이 (0: 루트, 1: 1단계 하위, ...)
     * @return 해당 깊이의 부서 목록
     */
    List<DepartmentDto.Response> getDepartmentsByDepth(String tenantId, int depth);

    /**
     * 특정 타입의 부서 조회
     *
     * @param tenantId 테넌트 ID
     * @param type 부서 타입 (TEAM, CALL_CENTER, SUPPORT 등)
     * @return 해당 타입의 부서 목록
     */
    /**
     * 특정 타입의 부서 조회
     *
     * @param tenantId 테넌트 ID
     * @param type 부서 타입 (COMPANY, DIVISION, TEAM, GROUP, CUSTOM)
     * @return 해당 타입의 부서 목록
     */
    List<DepartmentDto.Response> getDepartmentsByType(String tenantId, DepartmentType type);

    // ============================================================
    // 부서 통계 및 구성원
    // ============================================================

    /**
     * 부서 통계 정보 조회
     * - 직원 수, 하위 부서 수 등
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @return 부서 통계 정보
     */
    DepartmentDto.Statistics getDepartmentStatistics(String tenantId, String deptId);

    /**
     * 부서 구성원 조회
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @param includeSubDepts 하위 부서 포함 여부
     * @return 부서 구성원 목록
     */
    DepartmentDto.DepartmentMembers getDepartmentMembers(
            String tenantId,
            String deptId,
            boolean includeSubDepts);

    // ============================================================
    // 데이터 범위 기반 접근 제어
    // ============================================================

    /**
     * 사용자가 접근 가능한 부서 ID 집합 조회
     *
     * <p>역할별 부서 접근 범위:</p>
     * <ul>
     *   <li><b>ADMIN</b>: 전체 조직 조회 가능</li>
     *   <li><b>TEAM_LEAD</b>: 자신의 부서 + 하위 부서 조회 가능</li>
     *   <li><b>MEMBER</b>: 자신의 부서만 조회 가능</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 접근 가능한 부서 ID 집합
     */
    java.util.Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId);
}
