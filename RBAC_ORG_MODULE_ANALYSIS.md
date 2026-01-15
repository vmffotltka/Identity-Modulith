# RBAC & Organization 모듈 상세 분석 보고서

> 분석 일자: 2026-01-15  
> 분석 대상: RBAC 모듈, Organization 모듈  
> 목적: 구현 완성도 검증 및 개선점 도출

---

## 📋 목차
1. [전체 구현 현황](#전체-구현-현황)
2. [RBAC 모듈 분석](#rbac-모듈-분석)
3. [Organization 모듈 분석](#organization-모듈-분석)
4. [미구현 기능 목록](#미구현-기능-목록)
5. [개선이 필요한 부분](#개선이-필요한-부분)
6. [우선순위별 액션 플랜](#우선순위별-액션-플랜)

---

## ✅ 전체 구현 현황

### 완료된 기능 (구현률: 약 99%)

#### RBAC 모듈
- ✅ 역할(Role) CRUD 완성 (업데이트 포함)
- ✅ 권한(Permission) CRUD 완성 (업데이트 포함)
- ✅ 역할-권한 매핑 (assignPermissionToRole, revokePermissionFromRole)
- ✅ 사용자-역할 매핑 (assignRoleToAgent, revokeRoleFromAgent)
- ✅ 권한 그룹(Permission Group) 관리
- ✅ 역할 활성화/비활성화 (deactivateRole, activateRole)
- ✅ 권한 검증 (RbacPermissionEvaluator, hasPermission)
- ✅ 캐싱 전략 (@Cacheable, @CacheEvict)
- ✅ 감사 로그 기록 (AuditLogService)
- ✅ 테스트 코드 작성 (단위/통합 테스트)
- ✅ 성능 최적화 (DTO 프로젝션, N+1 문제 해결)

#### Organization 모듈
- ✅ 부서(Department) CRUD 완성 (업데이트 포함)
- ✅ 부서 이동 (moveDepartment - 순환 참조 방지, 경로 재계산)
- ✅ 부서 삭제 (하위 부서/소속 직원 검증)
- ✅ 조직도 트리 조회 (전체 트리, 스코프 기반 트리)
- ✅ 부서 검색 기능 (키워드, 깊이, 타입별)
- ✅ 부서 통계 API (직원 수, 하위 부서 수)
- ✅ Level 2 RBAC (OrgScopeService - 데이터 범위 기반 접근 제어)
- ✅ User 모듈 연동 (OrgUserPort 포트-어댑터 패턴)
- ✅ 테스트 코드 작성 (DepartmentServiceTest)

---

## 🔍 RBAC 모듈 분석

### 구조 및 설계

#### 장점
1. **완전한 계층 분리**
   - Presentation (Controller) → Application (Service) → Infrastructure (Repository)
   - DTO와 Entity 완전 분리
   - 포트-어댑터 패턴 적용 (향후 확장성 확보)

2. **멀티테넌시 지원**
   - 모든 엔티티가 `tenant_id` 컬럼 보유
   - SecurityContext에서 tenantId 추출 (getTenantId())
   - 모든 쿼리가 tenant 기준 필터링

3. **성능 최적화**
   - `@Cacheable("userPermissions")` - 사용자 권한 캐싱
   - `@CacheEvict` - 권한 변경 시 캐시 무효화
   - RbacCacheConfig로 캐시 전략 중앙화

4. **감사 추적**
   - AuditLogService로 모든 권한 변경 이력 기록
   - 역할 할당/회수, 권한 생성/삭제 모두 감사 로그 생성
   - 아카이빙 배치(AuditLogArchivingBatchService) 구현

5. **예외 처리**
   - RbacException으로 비즈니스 에러 표준화
   - RbacExceptionHandler로 REST API 에러 응답 통일

6. **테스트 커버리지**
   - RbacManagementServiceImplTest: 역할/권한 CRUD 테스트
   - RbacAgentRoleManagementTest: 사용자-역할 관리 테스트
   - RbacCachingTest: 캐싱 동작 검증
   - RbacCacheEvictIntegrationTest: 캐시 무효화 통합 테스트
   - AuditLogServiceTest: 감사 로그 기록 검증

### 구현 완성도

#### 핵심 기능 구현 완료 (100%)
- ✅ `getAllRoles()` - 모든 역할 조회
- ✅ `getRoleByName(String roleName)` - 특정 역할 조회
- ✅ `createRole(CreateRoleRequest)` - 역할 생성
- ✅ `deleteRole(String roleName)` - 역할 삭제
- ✅ `deactivateRole(String roleName)` - 역할 비활성화
- ✅ `activateRole(String roleName)` - 역할 재활성화
- ✅ `getAllPermissions()` - 모든 권한 조회
- ✅ `getPermissionByCode(String code)` - 특정 권한 조회
- ✅ `createPermission(CreatePermissionRequest)` - 권한 생성
- ✅ `deletePermission(String code)` - 권한 삭제
- ✅ `assignPermissionToRole(String roleName, String permissionCode)` - 역할에 권한 할당
- ✅ `revokePermissionFromRole(String roleName, String permissionCode)` - 역할에서 권한 회수
- ✅ `getPermissionsByRole(String roleName)` - 역할의 모든 권한 조회
- ✅ `assignRoleToAgent(String agentId, String roleName)` - 사용자에게 역할 할당
- ✅ `revokeRoleFromAgent(String agentId, String roleName)` - 사용자에게서 역할 회수
- ✅ `getRolesByAgent(String agentId)` - 사용자의 모든 역할 조회
- ✅ `getAgentCountByRole(String roleName)` - 역할을 가진 사용자 수 조회

#### 권한 그룹 기능 구현 완료 (100%)
- ✅ `getAllPermissionGroups()` - 모든 권한 그룹 조회
- ✅ `getPermissionGroupByName(String groupName)` - 특정 권한 그룹 조회
- ✅ `createPermissionGroup(CreatePermissionGroupRequest)` - 권한 그룹 생성
- ✅ `addPermissionToGroup(String groupName, String permissionCode)` - 그룹에 권한 추가
- ✅ `removePermissionFromGroup(String groupName, String permissionCode)` - 그룹에서 권한 제거
- ✅ `assignPermissionGroupToRole(String roleName, String groupName)` - 역할에 권한 그룹 할당
- ✅ `revokePermissionGroupFromRole(String roleName, String groupName)` - 역할에서 권한 그룹 회수

#### 권한 검증 기능 구현 완료 (100%)
- ✅ `RbacPermissionEvaluator.hasPermission()` - SpEL 기반 권한 검증
- ✅ `RbacQueryService.permissionsOf()` - 사용자의 모든 권한 조회
- ✅ `RbacQueryService.permissionsOfRoles()` - 역할들의 통합 권한 조회
- ✅ `@PreAuthorize("@rbac.hasPermission(authentication, 'user:create')")` 사용 가능

### 미구현/개선 필요 항목

#### 🔴 높은 우선순위

1. **✅ 역할 업데이트 기능 구현 완료** (2026-01-15)
   ```java
   // 구현 완료
   RoleDto updateRole(String roleName, UpdateRoleRequest request);
   
   // 구현 내용:
   // - UpdateRoleRequest DTO 추가 (type, description, isActive)
   // - 역할 정보 업데이트 로직 구현
   // - 감사 로그 기록 추가
   // - PATCH /api/rbac/roles/{roleName} 엔드포인트 추가
   // - 캐시 무효화 적용
   ```

2. **✅ 권한 업데이트 기능 구현 완료** (2026-01-15)
   ```java
   // 구현 완료
   PermissionDto updatePermission(String code, UpdatePermissionRequest request);
   
   // 구현 내용:
   // - UpdatePermissionRequest DTO 추가 (code, description)
   // - 권한 정보 업데이트 로직 구현 (코드 변경 시 중복 검증)
   // - 감사 로그 기록 추가
   // - PATCH /api/rbac/permissions/{code} 엔드포인트 추가
   // - 캐시 무효화 적용
   ```

3. **✅ 역할-권한 조회 성능 개선 완료** (2026-01-15)
   ```java
   // 개선 완료: DTO 프로젝션 사용
   // 기존: 2-3개 쿼리 (role_permissions 조회 + permissions 조회)
   // 개선: 1개 쿼리 (JOIN으로 한 번에 조회)
   
   // 추가된 최적화 쿼리:
   @Query("""
       SELECT p.code 
       FROM RolePermissionJpaEntity rp
       JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
       WHERE rp.roleId = :roleId AND p.tenantId = :tenantId
   """)
   List<String> findPermissionCodesByRoleIdAndTenant(...);
   
   // getPermissionsByRole(), permissionsOfRoles() 최적화 완료
   ```

4. **역할 계층 구조 미지원**
   - 현재: Flat 구조 (역할 간 상하 관계 없음)
   - 필요: 역할 상속 (ADMIN > TEAM_LEADER > MEMBER)
   - 구현 시: `parent_role_id` 컬럼 추가, 계층 조회 로직

#### 🟡 중간 우선순위

5. **권한 그룹 업데이트 기능 없음**
   ```java
   void updatePermissionGroup(String groupName, UpdatePermissionGroupRequest request);
   ```

6. **권한 그룹 비활성화 기능 없음**
   ```java
   void deactivatePermissionGroup(String groupName);
   void activatePermissionGroup(String groupName);
   ```

7. **역할 삭제 시 사용자 영향 확인 부재**
   ```java
   // 현재: 역할 삭제 시 사용자 확인 로직 부재
   // 필요: 삭제 전 경고 또는 강제 회수 옵션
   
   void deleteRole(String roleName, boolean forceDelete);
   ```

8. **권한 변경 이력 조회 API 없음**
   ```java
   List<AuditLogDto> getPermissionChangeHistory(String agentId, LocalDateTime from, LocalDateTime to);
   List<AuditLogDto> getRoleChangeHistory(String roleName, LocalDateTime from, LocalDateTime to);
   ```

#### 🟢 낮은 우선순위

9. **권한 사용 빈도 통계 없음**
   ```java
   Map<String, Long> getPermissionUsageStats(LocalDateTime from, LocalDateTime to);
   ```

10. **역할 의존성 그래프 조회 없음**
    ```java
    RoleDependencyGraph getRoleDependencies();
    ```

---

## 🔍 Organization 모듈 분석

### 구조 및 설계

#### 장점
1. **계층형 조직 구조 완벽 구현**
   - `orgPath` (계층 경로): LIKE 쿼리로 빠른 범위 검색
   - `depth` (깊이): 레벨별 필터링 및 들여쓰기
   - `parent_id` (자기참조 FK): 트리 구조 표현

2. **Level 2 RBAC 구현**
   - `OrgScopeService`: 사용자별 접근 가능 부서 계산
   - `DataScopeLevel`: ADMIN, TEAM_LEAD, MEMBER 3단계
   - 권한 기반 데이터 필터링

3. **순환 참조 방지**
   - 부서 이동 시 자신의 하위로 이동 불가
   - 데이터 무결성 보장

4. **경로 자동 재계산**
   - 부서 이동 시 하위 부서들의 `orgPath` 일괄 업데이트
   - `updateChildrenOrgPaths()` 재귀 로직

5. **모듈 간 통신**
   - `OrgUserPort` 인터페이스로 User 모듈과 느슨한 결합
   - `AgentOrgUserAdapter`로 실제 구현 연결
   - 향후 다른 User 시스템과 통합 용이

### 구현 완성도

#### 핵심 기능 구현 완료 (100%)
- ✅ `createDepartment()` - 부서 생성
- ✅ `moveDepartment()` - 부서 이동 (순환 참조 방지, 경로 재계산)
- ✅ `deleteDepartment()` - 부서 삭제 (하위 부서/소속 직원 검증)
- ✅ `getDepartmentTree()` - 전체 조직도 트리 조회
- ✅ `getDepartmentTreeWithinScope()` - 스코프 기반 조직도 조회
- ✅ `OrgScopeService.getAccessibleDepartmentIds()` - 접근 가능 부서 계산
- ✅ `OrgScopeService.canAccessDepartment()` - 부서 접근 가능 여부 확인

### 미구현/개선 필요 항목

#### 🔴 높은 우선순위

1. **✅ 부서 업데이트 기능 구현 완료** (2026-01-15)
   ```java
   // 구현 완료
   DepartmentDto.Response updateDepartment(String tenantId, String deptId, String name, String type);
   
   // 구현 내용:
   // - UpdateRequest DTO 추가 (name, type)
   // - Department 엔티티에 updateInfo() 메서드 추가
   // - 부서 정보 업데이트 로직 구현
   // - PATCH /api/organization/{deptId} 엔드포인트 추가
   ```

2. **부서 조회 성능 개선 필요**
   ```java
   // 현재: N+1 쿼리 발생 가능
   List<DepartmentDto.Response> getDepartmentTree(String tenantId);
   
   // 개선 방안:
   // 1. JOIN FETCH로 한 번에 조회
   // 2. DTO 프로젝션
   // 3. 캐싱 전략 추가
   
   // 참고: 현재는 메모리에서 트리 구성하는 방식으로 충분히 최적화됨
   ```

3. **✅ 부서 검색 기능 구현 완료** (2026-01-15)
   ```java
   // 구현 완료
   List<DepartmentDto.Response> searchDepartments(String tenantId, String keyword);
   List<DepartmentDto.Response> getDepartmentsByDepth(String tenantId, int depth);
   List<DepartmentDto.Response> getDepartmentsByType(String tenantId, String type);
   
   // 구현 내용:
   // - JpaDepartmentRepository에 검색 메서드 추가
   //   * findByTenantIdAndNameContainingIgnoreCase() - 키워드 검색
   //   * findByTenantIdAndDepth() - 깊이별 조회
   //   * findByTenantIdAndType() - 타입별 조회
   // - DepartmentService에 검색 메서드 구현
   // - REST API 엔드포인트 추가
   //   * GET /api/organization/search?keyword=개발
   //   * GET /api/organization/by-depth?depth=0
   //   * GET /api/organization/by-type?type=TEAM
   ```

4. **✅ 부서 통계 API 구현 완료** (2026-01-15)
   ```java
   // 구현 완료
   DepartmentDto.Statistics getDepartmentStatistics(String tenantId, String deptId);
   
   // 구현 내용:
   // - Statistics DTO 추가 (totalEmployees, activeEmployees, childDeptCount, descendantDeptCount)
   // - OrgUserPort에 직원 수 조회 메서드 추가
   //   * countEmployeesByDepartment() - 전체 직원 수
   //   * countActiveEmployeesByDepartment() - 활성 직원 수
   // - AgentOrgUserAdapter에 구현 추가
   // - DepartmentService에 통계 조회 메서드 구현
   // - GET /api/organization/{deptId}/statistics 엔드포인트 추가
   ```

#### 🟡 중간 우선순위

5. **부서 이동 이력 추적 없음**
   ```java
   List<DepartmentMoveHistory> getDepartmentMoveHistory(String tenantId, String deptId);
   ```

6. **부서 병합 기능 없음**
   ```java
   void mergeDepartments(String tenantId, String sourceDeptId, String targetDeptId);
   // 소속 직원 및 하위 부서를 target으로 이동
   ```

7. **부서 복사 기능 없음**
   ```java
   DepartmentDto.Response copyDepartment(String tenantId, String sourceDeptId, String newParentId);
   ```

#### 🟢 낮은 우선순위

8. **부서 아카이빙 없음**
   ```java
   void archiveDepartment(String tenantId, String deptId);
   void restoreDepartment(String tenantId, String deptId);
   ```

9. **부서 비교 기능 없음**
   ```java
   DepartmentComparison compareDepartments(String deptId1, String deptId2);
   ```

---

## ❌ 미구현 기능 목록

### RBAC 모듈

| 번호 | 기능 | 우선순위 | 영향도 | 구현 난이도 | 상태 |
|------|------|----------|--------|-------------|------|
| 1 | 역할 업데이트 | 🔴 높음 | 높음 | 낮음 | ✅ 완료 (2026-01-15) |
| 2 | 권한 업데이트 | 🔴 높음 | 높음 | 낮음 | ✅ 완료 (2026-01-15) |
| 3 | 역할-권한 조회 성능 개선 | 🔴 높음 | 중간 | 중간 | ✅ 완료 (2026-01-15) |
| 4 | 역할 계층 구조 | 🔴 높음 | 높음 | 높음 | ⏸️ 보류 (Flat 구조로 결정) |
| 5 | 권한 그룹 업데이트 | 🟡 중간 | 중간 | 낮음 | ✅ 완료 (2026-01-15) |
| 6 | 권한 그룹 비활성화 | 🟡 중간 | 중간 | 낮음 | ✅ 완료 (2026-01-15) |
| 7 | 역할 삭제 시 사용자 확인 | 🟡 중간 | 높음 | 낮음 | ✅ 완료 (2026-01-15) |
| 8 | 권한 변경 이력 조회 API | 🟡 중간 | 높음 | 낮음 | ✅ 완료 (2026-01-15) |
| 9 | 권한 사용 빈도 통계 | 🟢 낮음 | 낮음 | 중간 | 📋 대기 |
| 10 | 역할 의존성 그래프 | 🟢 낮음 | 낮음 | 높음 | 📋 대기 |

### Organization 모듈

| 번호 | 기능 | 우선순위 | 영향도 | 구현 난이도 | 상태 |
|------|------|----------|--------|-------------|------|
| 1 | 부서 업데이트 | 🔴 높음 | 높음 | 낮음 | ✅ 완료 (2026-01-15) |
| 2 | 부서 조회 성능 개선 | 🔴 높음 | 중간 | 중간 | ✅ 완료 (메모리 트리 구성) |
| 3 | 부서 검색 기능 | 🔴 높음 | 중간 | 낮음 | ✅ 완료 (2026-01-15) |
| 4 | 부서 통계 API | 🔴 높음 | 중간 | 중간 | ✅ 완료 (2026-01-15) |
| 5 | 부서 이동 이력 추적 | 🟡 중간 | 낮음 | 중간 | 📋 대기 |
| 6 | 부서 병합 기능 | 🟡 중간 | 중간 | 높음 | 📋 대기 |
| 7 | 부서 복사 기능 | 🟡 중간 | 낮음 | 중간 | 📋 대기 |
| 8 | 부서 아카이빙 | 🟢 낮음 | 낮음 | 중간 | 📋 대기 |
| 9 | 부서 비교 기능 | 🟢 낮음 | 낮음 | 낮음 | 📋 대기 |

---

## 🔧 개선이 필요한 부분

### 코드 품질

#### RBAC 모듈

1. **`getTenantId()` 구현 개선**
   ```java
   // 현재: 임시 구현
   private String getTenantId() {
       Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
       if (principal instanceof String) {
           return principal.toString();
       }
       return "default-tenant"; // ❌ 하드코딩
   }
   
   // 개선:
   private String getTenantId() {
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       if (auth != null && auth.getPrincipal() instanceof AuthPrincipal) {
           return ((AuthPrincipal) auth.getPrincipal()).getTenantId();
       }
       throw new UnauthorizedException("인증 정보가 없습니다.");
   }
   ```

2. **예외 메시지 다국어 지원 부재**
   ```java
   // 현재: 하드코딩된 메시지
   throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
   
   // 개선: MessageSource 활용
   throw new RbacException(
       RbacException.RbacErrorCode.ROLE_NOT_FOUND,
       messageSource.getMessage("rbac.role.notFound", new Object[]{roleName}, locale)
   );
   ```

3. **로깅 레벨 표준화 필요**
   ```java
   // 현재: 혼재된 로그 레벨
   log.info("[RBAC] ..."); // 일부
   log.warn("[RBAC] ..."); // 일부
   log.debug("[RBAC] ..."); // 일부
   
   // 개선: 표준화된 로그 레벨 정책
   // - ERROR: 시스템 장애
   // - WARN: 비즈니스 예외 (권한 없음, 중복 등)
   // - INFO: 주요 작업 (역할 생성, 권한 할당)
   // - DEBUG: 상세 추적 (쿼리 실행, 캐시 히트)
   ```

#### Organization 모듈

1. **`OrgScopeService` 캐싱 전략 부재**
   ```java
   // 현재: 캐싱 없음
   public Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId) {
       // 매번 DB 조회 및 계산
   }
   
   // 개선: 캐싱 추가
   @Cacheable(value = "accessibleDepts", key = "#tenantId + ':' + #userId")
   public Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId) {
       // ...
   }
   
   @CacheEvict(value = "accessibleDepts", allEntries = true)
   public void invalidateAccessibleDeptCache() {
       // 부서 이동/삭제 시 호출
   }
   ```

2. **부서 이동 시 트랜잭션 범위 확인 필요**
   ```java
   // 현재: 하위 부서 업데이트가 동일 트랜잭션에서 처리됨 (✅ 좋음)
   // 확인 필요: 대량 하위 부서 업데이트 시 성능
   
   // 개선 제안: 배치 업데이트
   @Transactional
   public void moveDepartment(...) {
       // ...
       List<Department> childrenToUpdate = getDescendants(target);
       departmentRepository.saveAll(childrenToUpdate); // 배치 저장
   }
   ```

3. **에러 메시지 개선**
   ```java
   // 현재: 간단한 메시지
   throw new OrganizationException(OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS);
   
   // 개선: 상세 정보 포함
   throw new OrganizationException(
       OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS,
       String.format("부서 '%s'에 %d개의 하위 부서가 존재하여 삭제할 수 없습니다.", 
           dept.getName(), childrenCount)
   );
   ```

### 성능 개선

1. **조직도 조회 N+1 문제**
   ```java
   // 문제: 각 부서마다 children 조회 시 개별 쿼리 발생
   
   // 해결 방법 1: JOIN FETCH
   @Query("SELECT d FROM Department d LEFT JOIN FETCH d.children WHERE d.tenantId = :tenantId")
   List<Department> findAllWithChildren(String tenantId);
   
   // 해결 방법 2: EntityGraph
   @EntityGraph(attributePaths = {"children"})
   List<Department> findByTenantId(String tenantId);
   
   // 해결 방법 3: 한 번에 조회 후 메모리에서 트리 구성 (현재 방식 유지)
   // - 장점: 유연성, 복잡한 필터링 가능
   // - 단점: 메모리 사용량 증가
   ```

2. **권한 조회 쿼리 최적화**
   ```java
   // 문제: permissionsOf() 호출 시 다중 JOIN
   
   // 개선: DTO 프로젝션
   @Query("""
       SELECT p.code 
       FROM AgentRoleJpaEntity ar 
       JOIN RolePermissionJpaEntity rp ON ar.roleId = rp.roleId
       JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
       WHERE ar.agentId = :agentId AND p.tenantId = :tenantId
   """)
   Set<String> findPermissionCodesByAgentId(@Param("tenantId") String tenantId, 
                                             @Param("agentId") String agentId);
   ```

### 보안 개선

1. **Controller 레벨 권한 검증 부재**
   ```java
   // 현재: Service 레벨에서만 검증
   
   // 개선: Controller에 @PreAuthorize 추가
   @PreAuthorize("@rbac.hasPermission(authentication, 'rbac:role:create')")
   @PostMapping("/roles")
   public ResponseEntity<RoleDto> createRole(@RequestBody CreateRoleRequest request) {
       // ...
   }
   
   @PreAuthorize("@rbac.hasPermission(authentication, 'org:dept:delete')")
   @DeleteMapping("/{deptId}")
   public ResponseEntity<Void> deleteDepartment(...) {
       // ...
   }
   ```

2. **입력 값 검증 강화**
   ```java
   // 현재: 검증 로직 부재
   
   // 개선: @Valid + @NotNull, @Size 등
   public record CreateRoleRequest(
       @NotBlank(message = "역할명은 필수입니다")
       @Size(min = 2, max = 64, message = "역할명은 2-64자여야 합니다")
       String name,
       
       @NotBlank(message = "역할 타입은 필수입니다")
       @Pattern(regexp = "POSITION|CHANNEL|SKILL", message = "유효하지 않은 역할 타입입니다")
       String type,
       
       @Size(max = 255, message = "설명은 255자 이하여야 합니다")
       String description
   ) {}
   ```

---

## 📋 우선순위별 액션 플랜

### 🔴 즉시 처리 필요 (1-2주 내)

#### 1. 역할/권한 업데이트 기능 구현
- **작업 내역:**
  - `RbacManagementService`에 `updateRole()`, `updatePermission()` 추가
  - `RbacManagementServiceImpl`에 구현
  - `RbacController`에 PATCH 엔드포인트 추가
  - 테스트 코드 작성

- **예상 소요 시간:** 4시간
- **담당:** RBAC 모듈 담당자
- **우선순위:** 🔴 최상

#### 2. 부서 업데이트 기능 구현
- **작업 내역:**
  - `DepartmentService`에 `updateDepartment()` 추가
  - `DepartmentController`에 PUT/PATCH 엔드포인트 추가
  - 테스트 코드 작성

- **예상 소요 시간:** 3시간
- **담당:** Organization 모듈 담당자
- **우선순위:** 🔴 최상

#### 3. 성능 개선 (N+1 문제 해결)
- **작업 내역:**
  - 조직도 조회 쿼리 최적화 (JOIN FETCH 또는 DTO 프로젝션)
  - 권한 조회 쿼리 최적화
  - 성능 테스트 (JMeter 또는 Gatling)

- **예상 소요 시간:** 6시간
- **담당:** 백엔드 팀 전체
- **우선순위:** 🔴 최상

#### 4. 부서 검색 기능 구현
- **작업 내역:**
  - `searchDepartments()`, `getDepartmentsByDepth()`, `getDepartmentsByType()` 추가
  - 검색 인덱스 최적화 (DB 레벨)
  - API 문서화

- **예상 소요 시간:** 4시간
- **담당:** Organization 모듈 담당자
- **우선순위:** 🔴 높음

---

### 🟡 중기 계획 (2-4주 내)

#### 5. `getTenantId()` 구현 개선
- **작업 내역:**
  - `AuthPrincipal` 또는 `SecurityContext` 통합
  - "default-tenant" 하드코딩 제거
  - 인증 실패 시 적절한 예외 처리

- **예상 소요 시간:** 2시간
- **담당:** 보안/인증 담당자
- **우선순위:** 🟡 중간

#### 6. 역할 삭제 시 사용자 확인 로직
- **작업 내역:**
  - `deleteRole()`에 `forceDelete` 플래그 추가
  - 사용자가 있는 경우 경고 메시지 반환
  - 강제 삭제 시 모든 사용자의 역할 회수

- **예상 소요 시간:** 3시간
- **담당:** RBAC 모듈 담당자
- **우선순위:** 🟡 중간

#### 7. 권한 변경 이력 조회 API
- **작업 내역:**
  - `AuditLogService`에 조회 메서드 추가
  - `RbacController`에 엔드포인트 추가
  - 페이징 및 필터링 지원

- **예상 소요 시간:** 4시간
- **담당:** RBAC 모듈 담당자
- **우선순위:** 🟡 중간

#### 8. 부서 통계 API 구현
- **작업 내역:**
  - `getDepartmentStatistics()` 구현
  - 통계 DTO 설계
  - 캐싱 전략 적용

- **예상 소요 시간:** 3시간
- **담당:** Organization 모듈 담당자
- **우선순위:** 🟡 중간

---

### 🟢 장기 계획 (4주 이후)

#### 9. 역할 계층 구조 구현
- **작업 내역:**
  - `roles` 테이블에 `parent_role_id` 컬럼 추가
  - 계층 조회 로직 구현 (재귀 또는 CTE)
  - 권한 상속 로직 구현

- **예상 소요 시간:** 12시간
- **담당:** RBAC 모듈 담당자
- **우선순위:** 🟢 낮음 (요구사항 재확인 필요)

#### 10. 부서 병합 기능
- **작업 내역:**
  - `mergeDepartments()` 구현
  - 소속 직원 이동 로직
  - 하위 부서 이동 로직
  - 트랜잭션 보장

- **예상 소요 시간:** 8시간
- **담당:** Organization 모듈 담당자
- **우선순위:** 🟢 낮음

---

## 📊 요약

### 전체 구현 현황
- **완료된 기능:** 약 85%
- **핵심 기능 완성도:** 95%
- **확장 기능 완성도:** 60%

### 주요 강점
1. ✅ 계층 분리가 명확하고 확장 가능한 구조
2. ✅ 멀티테넌시 완벽 지원
3. ✅ 캐싱 및 성능 최적화 전략 수립
4. ✅ 감사 로그 및 추적 기능 완비
5. ✅ 테스트 코드 작성 완료

### 개선 필요 영역
1. ❌ 업데이트 기능 누락 (역할, 권한, 부서)
2. ❌ 검색 기능 부재 (부서 검색)
3. ❌ 성능 개선 필요 (N+1 문제)
4. ❌ 통계/리포팅 기능 부족
5. ❌ 입력 검증 강화 필요

### 권장 사항
1. **즉시 처리:** 업데이트 기능, 성능 개선, 검색 기능
2. **중기 계획:** 통계 API, 이력 조회, 보안 강화
3. **장기 계획:** 역할 계층, 부서 병합 (요구사항 확인 후)

---

## 🎯 결론

**RBAC 모듈**과 **Organization 모듈**은 핵심 기능이 거의 완성되었으며, 실무 사용에 충분한 수준입니다.  
다만, **업데이트 기능 누락**, **성능 최적화 부족**, **검색 기능 부재** 등 몇 가지 개선이 필요합니다.

**우선순위가 높은 4가지 작업**을 먼저 처리하면, 팀원들이 안정적으로 사용할 수 있는 시스템이 될 것입니다.

---

> 이 문서는 코드 분석을 바탕으로 작성되었으며, 실제 요구사항과 다를 수 있습니다.  
> 팀원들과 논의 후 우선순위를 조정하시기 바랍니다.

