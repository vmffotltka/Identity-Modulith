# Department Scenarios - 조직 시나리오 상세 정의

조직(Department) 관리 전체 플로우 및 비즈니스 규칙 상세 정의

---

## 1. Department 데이터 모델 (상세)

### 1.1 Department Aggregate

```
┌─────────────────────────────────────────────────────────────────────┐
│  Department (Aggregate Root)                                        │
├─────────────────────────────────────────────────────────────────────┤
│  [식별]                                                             │
│  - id: Long (PK, Auto-generated)                                    │
│  - tenantId: String                                                 │
│  - code: String (UK per tenant)                                     │
│                                                                     │
│  [기본 정보]                                                        │
│  - name: String                                                     │
│  - type: DepartmentType                                             │
│  - customTypeName: String? (type=CUSTOM일 때)                       │
│                                                                     │
│  [계층]                                                             │
│  - parentId: Long? (self-referential, null=루트)                    │
│                                                                     │
│  [상태]                                                             │
│  - status: DepartmentStatus                                         │
│  - deactivatedAt: DateTime?                                         │
│                                                                     │
│  [감사]                                                             │
│  - createdAt: DateTime                                              │
│  - updatedAt: DateTime                                              │
│  - createdBy: UUID?                                                 │
│  - updatedBy: UUID?                                                 │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Value Objects

```java
public enum DepartmentType {
    COMPANY,    // 회사 (루트 전용)
    DIVISION,   // 본부/사업부
    TEAM,       // 팀
    GROUP,      // 그룹/파트
    CUSTOM      // 커스텀 (customTypeName 필수)
}

public enum DepartmentStatus {
    ACTIVE,     // 활성
    INACTIVE    // 비활성 (운영 중지)
}
```

---

## 2. 트리 구조 규칙

### 2.1 기본 구조

```
┌────────────────────────────────────────────────────────────────────┐
│  Tenant: nexfron                                                   │
│                                                                    │
│  넥스프론 (COMPANY, 루트)     ← parentId = null                    │
│      │                                                             │
│      ├── 고객서비스본부 (DIVISION)                                 │
│      │       │                                                     │
│      │       ├── 서울센터 (CUSTOM: "센터")                         │
│      │       │       ├── 인바운드팀 (TEAM)                         │
│      │       │       └── 아웃바운드팀 (TEAM)                       │
│      │       │                                                     │
│      │       └── 부산센터 (CUSTOM: "센터")                         │
│      │               └── 통합상담팀 (TEAM)                         │
│      │                                                             │
│      └── 기술본부 (DIVISION)                                       │
│              └── 개발팀 (TEAM)                                     │
│                      └── 백엔드파트 (GROUP)                        │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 2.2 핵심 규칙

| ID | 규칙 | 설명 |
|----|------|------|
| T-001 | 테넌트당 루트 부서 1개 | parentId=null인 부서는 1개만 |
| T-002 | 루트는 COMPANY 타입 권장 | 최상위 부서는 COMPANY 타입 |
| T-003 | 순환 참조 금지 | 자신/하위를 부모로 설정 불가 |
| T-004 | 타입은 계층 위치 제한 없음 | TEAM이 루트 바로 아래 가능 |

---

## 3. 시나리오: 부서 생성 (Create)

### 3.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│     CreateDepartment        │       │    DepartmentCreated        │
│     (Command)               │──────▶│    (Event)                  │
├─────────────────────────────┤       ├─────────────────────────────┤
│ tenantId: String            │       │ departmentId: Long          │
│ name: String                │       │ tenantId: String            │
│ code: String                │       │ name: String                │
│ type: DepartmentType        │       │ code: String                │
│ customTypeName: String?     │       │ type: DepartmentType        │
│ parentId: Long?             │       │ parentId: Long?             │
│ actorId: UUID               │       │ createdAt: DateTime         │
└─────────────────────────────┘       └─────────────────────────────┘
```

### 3.2 플로우

```
요청 수신
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 검증                                               │
│     - code 형식 검증 (영문+숫자+하이픈, 3-30자)        │
│     - code 중복 검사 (테넌트 내)                       │
│     - type=CUSTOM이면 customTypeName 필수              │
│     - parentId 검증 (있으면 존재 및 ACTIVE 확인)       │
├────────────────────────────────────────────────────────┤
│  2. 루트 부서 검증 (parentId가 null인 경우)            │
│     - 테넌트 내 기존 루트 부서 존재 여부 확인          │
│     - 이미 있으면 ROOT_ALREADY_EXISTS 에러             │
├────────────────────────────────────────────────────────┤
│  3. DataScope 권한 검증                                │
│     - 행위자가 부모 부서에 접근 권한 있는지 확인       │
│     - 루트 생성은 ADMIN만 가능                         │
├────────────────────────────────────────────────────────┤
│  4. Department 생성                                    │
│     - status = ACTIVE                                  │
│     - createdAt = now()                                │
├────────────────────────────────────────────────────────┤
│  5. 이벤트 발행                                        │
│     - DepartmentCreated 이벤트                         │
└────────────────────────────────────────────────────────┘
```

### 3.3 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| CD-001 | code는 테넌트 내 유일 | DUPLICATE_DEPT_CODE |
| CD-002 | 테넌트당 루트 부서 1개 | ROOT_ALREADY_EXISTS |
| CD-003 | 부모 부서는 ACTIVE 상태여야 함 | PARENT_DEPT_INACTIVE |
| CD-004 | type=CUSTOM이면 customTypeName 필수 | CUSTOM_TYPE_NAME_REQUIRED |
| CD-005 | 루트 생성은 ADMIN만 가능 | INSUFFICIENT_PERMISSION |

### 3.4 API 요청/응답

```json
// POST /api/departments

// Request - 일반 부서
{
  "name": "인바운드팀",
  "code": "INBOUND-01",
  "type": "TEAM",
  "parentId": 5
}

// Request - 커스텀 타입
{
  "name": "서울센터",
  "code": "SEOUL-CENTER",
  "type": "CUSTOM",
  "customTypeName": "센터",
  "parentId": 3
}

// Request - 루트 부서 (최초 생성)
{
  "name": "넥스프론",
  "code": "NEXFRON",
  "type": "COMPANY"
  // parentId 없음 = 루트
}

// Response (201 Created)
{
  "departmentId": 10,
  "name": "인바운드팀",
  "code": "INBOUND-01",
  "type": "TEAM",
  "parentId": 5,
  "status": "ACTIVE",
  "createdAt": "2026-01-13T10:00:00"
}
```

---

## 4. 시나리오: 부서 이동 (Move)

### 4.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│     MoveDepartment          │       │    DepartmentMoved          │
│     (Command)               │──────▶│    (Event)                  │
├─────────────────────────────┤       ├─────────────────────────────┤
│ tenantId: String            │       │ departmentId: Long          │
│ departmentId: Long          │       │ tenantId: String            │
│ newParentId: Long?          │       │ fromParentId: Long?         │
│ actorId: UUID               │       │ toParentId: Long?           │
│ includeMembers: Boolean     │       │ movedAt: DateTime           │
│ (기본값: true)              │       │ membersMoved: Boolean       │
└─────────────────────────────┘       └─────────────────────────────┘
```

### 4.2 플로우

```
요청 수신
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 대상 부서 검증                                     │
│     - 부서 존재 확인                                   │
│     - 루트 부서 이동 불가                              │
├────────────────────────────────────────────────────────┤
│  2. 새 부모 부서 검증                                  │
│     - newParentId 존재 및 ACTIVE 확인                  │
│     - 동일 부서로 이동 불가 (현재 parent와 동일)       │
├────────────────────────────────────────────────────────┤
│  3. 순환 참조 검사 ★ 핵심                              │
│     - newParentId의 모든 조상(ancestors) 조회          │
│     - 조상 중에 departmentId가 있으면 에러             │
│     (자기 자신의 하위로 이동하려는 시도 차단)          │
├────────────────────────────────────────────────────────┤
│  4. DataScope 권한 검증                                │
│     - 행위자가 이동 대상 부서에 접근 권한 있는지       │
│     - 행위자가 새 부모 부서에 접근 권한 있는지         │
├────────────────────────────────────────────────────────┤
│  5. 이동 실행                                          │
│     - department.parentId = newParentId                │
│     - updatedAt = now()                                │
├────────────────────────────────────────────────────────┤
│  6. 소속 상담사 처리 (includeMembers=true)             │
│     - 소속 상담사들도 함께 이동 (변경 없음)            │
│     - 상담사의 departmentId는 변경 없음 (부서가 이동)  │
├────────────────────────────────────────────────────────┤
│  7. 이벤트 발행                                        │
│     - DepartmentMoved 이벤트                           │
└────────────────────────────────────────────────────────┘
```

### 4.3 순환 참조 검사 상세

```
예시: "본부A"를 "팀1" 아래로 이동 시도

현재 구조:
회사
  └── 본부A
        └── 팀1
              └── 파트1

이동 시도: 본부A → 팀1 아래로
  - 팀1의 조상: [본부A, 회사]
  - 본부A가 팀1의 조상에 포함됨 → CIRCULAR_REFERENCE 에러!

SQL (PostgreSQL CTE):
WITH RECURSIVE ancestors AS (
    SELECT parent_id FROM departments
    WHERE dept_id = :newParentId AND tenant_id = :tenantId
    UNION ALL
    SELECT d.parent_id FROM departments d
    JOIN ancestors a ON d.dept_id = a.parent_id
    WHERE d.parent_id IS NOT NULL
)
SELECT parent_id FROM ancestors;

검사: ancestors에 departmentId가 포함되어 있으면 에러
```

### 4.4 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| MD-001 | 루트 부서는 이동 불가 | CANNOT_MOVE_ROOT |
| MD-002 | 새 부모는 ACTIVE 상태여야 함 | PARENT_DEPT_INACTIVE |
| MD-003 | 순환 참조 금지 | CIRCULAR_REFERENCE |
| MD-004 | 동일 위치로 이동 불가 | SAME_PARENT |
| MD-005 | 양쪽 부서에 접근 권한 필요 | INSUFFICIENT_PERMISSION |

### 4.5 API 요청/응답

```json
// POST /api/departments/{deptId}/move

// Request
{
  "newParentId": 10,
  "includeMembers": true  // 기본값: true
}

// Response (200 OK)
{
  "departmentId": 5,
  "name": "인바운드팀",
  "fromParentId": 3,
  "toParentId": 10,
  "movedAt": "2026-01-13T10:00:00",
  "membersMoved": true
}
```

---

## 5. 시나리오: 부서 비활성화 (Deactivate)

### 5.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│   DeactivateDepartment      │       │  DepartmentDeactivated      │
│     (Command)               │──────▶│    (Event)                  │
├─────────────────────────────┤       ├─────────────────────────────┤
│ tenantId: String            │       │ departmentId: Long          │
│ departmentId: Long          │       │ tenantId: String            │
│ actorId: UUID               │       │ deactivatedAt: DateTime     │
└─────────────────────────────┘       │ deactivatedBy: UUID         │
                                      └─────────────────────────────┘
```

### 5.2 플로우

```
요청 수신
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 대상 부서 검증                                     │
│     - 부서 존재 확인                                   │
│     - 현재 ACTIVE 상태인지 확인                        │
│     - 루트 부서 비활성화 불가                          │
├────────────────────────────────────────────────────────┤
│  2. 하위 부서 검사 ★                                   │
│     - ACTIVE 상태인 하위 부서 존재 여부 확인           │
│     - 있으면 CHILD_DEPT_ACTIVE 에러                    │
├────────────────────────────────────────────────────────┤
│  3. 소속 사용자 검사 ★                                 │
│     - 해당 부서에 ACTIVE 상담사 존재 여부 확인         │
│     - 있으면 ACTIVE_USERS_EXIST 에러                   │
│     - (미리 이동/퇴사 처리 필수)                       │
├────────────────────────────────────────────────────────┤
│  4. DataScope 권한 검증                                │
├────────────────────────────────────────────────────────┤
│  5. 비활성화 실행                                      │
│     - status = INACTIVE                                │
│     - deactivatedAt = now()                            │
├────────────────────────────────────────────────────────┤
│  6. 이벤트 발행                                        │
│     - DepartmentDeactivated 이벤트                     │
└────────────────────────────────────────────────────────┘
```

### 5.3 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| DD-001 | 루트 부서 비활성화 불가 | CANNOT_DEACTIVATE_ROOT |
| DD-002 | ACTIVE 하위 부서 있으면 불가 | CHILD_DEPT_ACTIVE |
| DD-003 | ACTIVE 상담사 있으면 불가 | ACTIVE_USERS_EXIST |
| DD-004 | 이미 INACTIVE면 불가 | ALREADY_INACTIVE |

### 5.4 비활성화 후 제약

| 제약 | 설명 |
|------|------|
| 상담사 배치 불가 | INACTIVE 부서로 상담사 이동/생성 불가 |
| 하위 부서 생성 불가 | INACTIVE 부서 아래 새 부서 생성 불가 |
| 조회는 가능 | 히스토리 목적으로 조회 가능 |

---

## 6. 시나리오: 부서 활성화 (Activate)

### 6.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│    ActivateDepartment       │       │   DepartmentActivated       │
│     (Command)               │──────▶│    (Event)                  │
├─────────────────────────────┤       ├─────────────────────────────┤
│ tenantId: String            │       │ departmentId: Long          │
│ departmentId: Long          │       │ tenantId: String            │
│ actorId: UUID               │       │ activatedAt: DateTime       │
└─────────────────────────────┘       │ activatedBy: UUID           │
                                      └─────────────────────────────┘
```

### 6.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| AD-001 | INACTIVE 상태만 활성화 가능 | ALREADY_ACTIVE |
| AD-002 | 부모 부서가 ACTIVE여야 함 | PARENT_DEPT_INACTIVE |

---

## 7. 시나리오: 부서 삭제 (Delete)

### 7.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│    DeleteDepartment         │       │    DepartmentDeleted        │
│     (Command)               │──────▶│    (Event)                  │
├─────────────────────────────┤       ├─────────────────────────────┤
│ tenantId: String            │       │ departmentId: Long          │
│ departmentId: Long          │       │ tenantId: String            │
│ actorId: UUID               │       │ deletedAt: DateTime         │
└─────────────────────────────┘       │ deletedBy: UUID             │
                                      └─────────────────────────────┘
```

### 7.2 플로우

```
요청 수신
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 대상 부서 검증                                     │
│     - 부서 존재 확인                                   │
│     - 루트 부서 삭제 불가                              │
├────────────────────────────────────────────────────────┤
│  2. 하위 부서 검사                                     │
│     - 하위 부서 존재 여부 확인 (상태 무관)             │
│     - 있으면 CHILD_DEPT_EXISTS 에러                    │
├────────────────────────────────────────────────────────┤
│  3. 소속 사용자 검사                                   │
│     - 소속 상담사 존재 여부 확인 (상태 무관)           │
│     - 있으면 USERS_EXIST 에러                          │
│     - (RETIRED 포함, 완전한 삭제를 위해)               │
├────────────────────────────────────────────────────────┤
│  4. DataScope 권한 검증                                │
│     - ADMIN만 삭제 가능                                │
├────────────────────────────────────────────────────────┤
│  5. 물리 삭제 실행                                     │
│     - DB에서 완전 삭제                                 │
├────────────────────────────────────────────────────────┤
│  6. 이벤트 발행                                        │
│     - DepartmentDeleted 이벤트                         │
└────────────────────────────────────────────────────────┘
```

### 7.3 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| DL-001 | 루트 부서 삭제 불가 | CANNOT_DELETE_ROOT |
| DL-002 | 하위 부서 존재 시 삭제 불가 | CHILD_DEPT_EXISTS |
| DL-003 | 소속 상담사 존재 시 삭제 불가 | USERS_EXIST |
| DL-004 | ADMIN만 삭제 가능 | INSUFFICIENT_PERMISSION |

### 7.4 삭제 vs 비활성화

| 구분 | 비활성화 | 삭제 |
|------|----------|------|
| 조건 | ACTIVE 하위/상담사 없음 | 모든 하위/상담사 없음 |
| 결과 | status = INACTIVE | DB에서 제거 |
| 복구 | 가능 (Activate) | 불가능 |
| 권장 | 일반적인 운영 종료 | 완전한 정리 필요 시 |

---

## 8. 시나리오: 부서 이름/정보 수정 (Update)

### 8.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│    UpdateDepartment         │       │    DepartmentUpdated        │
│     (Command)               │──────▶│    (Event)                  │
├─────────────────────────────┤       ├─────────────────────────────┤
│ tenantId: String            │       │ departmentId: Long          │
│ departmentId: Long          │       │ tenantId: String            │
│ name: String?               │       │ changes: Map<String,Object> │
│ type: DepartmentType?       │       │ updatedAt: DateTime         │
│ customTypeName: String?     │       │ updatedBy: UUID             │
│ actorId: UUID               │       └─────────────────────────────┘
└─────────────────────────────┘
```

### 8.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| UD-001 | code는 수정 불가 | CODE_CANNOT_BE_CHANGED |
| UD-002 | type 변경 시 검증 필요 | INVALID_TYPE_CHANGE |
| UD-003 | INACTIVE 부서도 수정 가능 | - |

---

## 9. 조회 시나리오

### 9.1 조직도 전체 조회

```
GET /api/departments/tree

응답: 전체 트리 구조 (권한 필터링 적용)
```

### 9.2 DataScope 적용 조회

```
┌────────────────────────────────────────────────────────────────────┐
│                    DataScope별 조회 범위                           │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ADMIN                                                             │
│  └── 테넌트 전체 조직도 조회 가능                                  │
│                                                                    │
│  TEAM_LEAD (예: 고객서비스본부 소속)                               │
│  └── 고객서비스본부 + 모든 하위 부서 조회 가능                     │
│      (기술본부 등 다른 본부는 조회 불가)                           │
│                                                                    │
│  MEMBER (예: 인바운드팀 소속)                                      │
│  └── 인바운드팀만 조회 가능                                        │
│      (같은 센터의 다른 팀도 조회 불가)                             │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 9.3 하위 부서 조회 (재귀)

```sql
-- PostgreSQL CTE로 하위 부서 조회
WITH RECURSIVE subtree AS (
    SELECT * FROM departments
    WHERE dept_id = :deptId AND tenant_id = :tenantId
    UNION ALL
    SELECT d.* FROM departments d
    JOIN subtree s ON d.parent_id = s.dept_id
)
SELECT * FROM subtree;
```

---

## 10. 상태 전이 다이어그램

```
                    CreateDepartment (루트)
                              │
                              ▼
       ┌──────────────────────────────────────────────────┐
       │                    ACTIVE                         │
       │                                                   │
       │  [가능한 작업]                                    │
       │  - UpdateDepartment (이름, 타입 수정)            │
       │  - MoveDepartment (이동)                         │
       │  - CreateDepartment (하위 부서 생성)             │
       │  - 상담사 배치                                   │
       └──────────────────────────────────────────────────┘
              │                           │
              │ DeactivateDepartment      │ DeleteDepartment
              │ (조건 충족 시)            │ (조건 충족 시)
              ▼                           ▼
       ┌──────────────┐           ┌──────────────────────┐
       │   INACTIVE   │           │      (삭제됨)        │
       │              │           │                       │
       │  [가능한 작업]│           │  - 복구 불가         │
       │  - 조회      │           │  - DB에서 제거       │
       │  - 수정      │           │                       │
       │  - Activate  │           │                       │
       └──────────────┘           └──────────────────────┘
              │
              │ ActivateDepartment
              │ (부모가 ACTIVE일 때)
              ▼
       ┌──────────────┐
       │    ACTIVE    │
       │    (복귀)    │
       └──────────────┘
```

---

## 11. Edge Cases 및 예외 처리

### 11.1 동시성 이슈

| 시나리오 | 처리 방법 |
|---------|----------|
| 동시에 같은 부서 이동 시도 | Optimistic Locking |
| 부서 삭제 중 상담사 배치 | 부서 상태 재검증 |
| 부모 부서 비활성화 중 하위 생성 | 부모 상태 재검증 |

### 11.2 일관성 보장

| 시나리오 | 처리 방법 |
|---------|----------|
| 부서 이동 시 상담사 DataScope | 상담사 departmentId 변경 없음 |
| 루트 부서 실수로 생성 방지 | 기존 루트 존재 검사 |
| 순환 참조 | CTE로 조상 검사 |

---

## 12. API 엔드포인트 요약

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/departments` | 부서 생성 |
| GET | `/api/departments/{id}` | 부서 상세 조회 |
| GET | `/api/departments/tree` | 조직도 트리 조회 |
| GET | `/api/departments/{id}/subtree` | 하위 부서 트리 조회 |
| PATCH | `/api/departments/{id}` | 부서 정보 수정 |
| POST | `/api/departments/{id}/move` | 부서 이동 |
| POST | `/api/departments/{id}/deactivate` | 부서 비활성화 |
| POST | `/api/departments/{id}/activate` | 부서 활성화 |
| DELETE | `/api/departments/{id}` | 부서 삭제 |

---

## 13. 에러 코드

| Code | HTTP | 설명 |
|------|------|------|
| DEPT_NOT_FOUND | 404 | 부서를 찾을 수 없음 |
| DUPLICATE_DEPT_CODE | 409 | 이미 존재하는 부서 코드 |
| ROOT_ALREADY_EXISTS | 409 | 이미 루트 부서 존재 |
| CIRCULAR_REFERENCE | 400 | 순환 참조 발생 |
| CANNOT_MOVE_ROOT | 400 | 루트 부서 이동 불가 |
| CANNOT_DEACTIVATE_ROOT | 400 | 루트 부서 비활성화 불가 |
| CANNOT_DELETE_ROOT | 400 | 루트 부서 삭제 불가 |
| CHILD_DEPT_EXISTS | 409 | 하위 부서 존재 |
| CHILD_DEPT_ACTIVE | 409 | 활성 하위 부서 존재 |
| ACTIVE_USERS_EXIST | 409 | 활성 사용자 존재 |
| USERS_EXIST | 409 | 사용자 존재 (삭제 시) |
| PARENT_DEPT_INACTIVE | 400 | 부모 부서가 비활성 |
| SAME_PARENT | 400 | 동일 위치로 이동 |
| ALREADY_ACTIVE | 400 | 이미 활성 상태 |
| ALREADY_INACTIVE | 400 | 이미 비활성 상태 |
| CODE_CANNOT_BE_CHANGED | 400 | 코드 수정 불가 |
| CUSTOM_TYPE_NAME_REQUIRED | 400 | 커스텀 타입명 필수 |
| INSUFFICIENT_PERMISSION | 403 | 권한 부족 |

---

*문서 버전: 1.0*
*최종 수정: 2026-01-13*
