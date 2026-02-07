# Organization API 테스트 가이드 (Swagger UI)

## 📋 목차
1. [사전 준비](#사전-준비)
2. [기본 테스트 순서](#기본-테스트-순서)
3. [시나리오별 테스트](#시나리오별-테스트)
4. [테스트 데이터](#테스트-데이터)

---

## 사전 준비

### 1. Swagger UI 접속
- URL: `http://localhost:8080/swagger-ui/index.html`
- Organization Management 섹션 찾기

### 2. 필요한 헤더 정보
Swagger UI에서 각 API 테스트 시 다음 헤더가 필요합니다:
- `X-Tenant-Id`: 테넌트 ID (자동 추출되지만 필요시 지정)
- `X-User-Id`: 사용자 ID (권한 검증이 필요한 API에서 사용)

**참고**: 현재 구현에서는 `TenantContextHolder`가 자동으로 테넌트 ID를 추출합니다.

---

## 기본 테스트 순서

### Phase 1: 부서 생성 (CRUD - Create)

#### 1-1. 최상위 부서(루트) 생성
**API**: `POST /api/org/departments`

**요청 Body**:
```json
{
  "name": "본사",
  "type": "COMPANY",
  "parentId": null
}
```

**예상 응답** (201 Created):
```json
{
  "deptId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "본사",
  "type": "COMPANY",
  "orgPath": "/550e8400-e29b-41d4-a716-446655440000",
  "depth": 0,
  "parentId": null,
  "children": []
}
```

**✅ 체크포인트**:
- `deptId` 복사해두기 (다음 단계에서 사용)
- `orgPath`가 자동 생성되었는지 확인
- `depth`가 0인지 확인

---

#### 1-2. 하위 부서 생성 (1단계)
**API**: `POST /api/org/departments`

**요청 Body** (위에서 복사한 deptId를 parentId로 사용):
```json
{
  "name": "개발본부",
  "type": "DIVISION",
  "parentId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**예상 응답** (201 Created):
```json
{
  "deptId": "660e8400-e29b-41d4-a716-446655440001",
  "name": "개발본부",
  "type": "DIVISION",
  "orgPath": "/550e8400-e29b-41d4-a716-446655440000/660e8400-e29b-41d4-a716-446655440001",
  "depth": 1,
  "parentId": "550e8400-e29b-41d4-a716-446655440000",
  "children": []
}
```

**✅ 체크포인트**:
- `orgPath`에 상위 부서 ID가 포함되어 있는지 확인
- `depth`가 1로 증가했는지 확인
- `parentId`가 올바른지 확인

---

#### 1-3. 하위 부서 생성 (2단계 - 팀)
**API**: `POST /api/org/departments`

**요청 Body**:
```json
{
  "name": "백엔드팀",
  "type": "TEAM",
  "parentId": "660e8400-e29b-41d4-a716-446655440001"
}
```

**✅ 체크포인트**:
- `depth`가 2가 되었는지 확인
- 3단계 계층 구조가 올바르게 생성되었는지 확인

---

#### 1-4. 추가 부서 생성 (병렬 구조 만들기)
다음 부서들을 차례로 생성하여 조직도를 풍부하게 만듭니다:

**영업본부 (본사 하위)**:
```json
{
  "name": "영업본부",
  "type": "DIVISION",
  "parentId": "{본사_deptId}"
}
```

**프론트엔드팀 (개발본부 하위)**:
```json
{
  "name": "프론트엔드팀",
  "type": "TEAM",
  "parentId": "{개발본부_deptId}"
}
```

**DevOps팀 (개발본부 하위)**:
```json
{
  "name": "DevOps팀",
  "type": "TEAM",
  "parentId": "{개발본부_deptId}"
}
```

---

### Phase 2: 부서 조회 (CRUD - Read)

#### 2-1. 전체 조직도 조회
**API**: `GET /api/org/departments`

**예상 응답**: 트리 구조로 모든 부서 반환
```json
[
  {
    "deptId": "550e8400-...",
    "name": "본사",
    "type": "COMPANY",
    "children": [
      {
        "deptId": "660e8400-...",
        "name": "개발본부",
        "type": "DIVISION",
        "children": [
          {
            "deptId": "770e8400-...",
            "name": "백엔드팀",
            "type": "TEAM",
            "children": []
          }
        ]
      }
    ]
  }
]
```

**✅ 체크포인트**:
- 모든 생성한 부서가 표시되는지 확인
- 트리 구조가 올바른지 확인
- `children` 배열이 올바르게 구성되었는지 확인

---

#### 2-2. 키워드로 부서 검색
**API**: `GET /api/org/departments/search?keyword=개발`

**예상 응답**:
```json
[
  {
    "deptId": "660e8400-...",
    "name": "개발본부",
    "type": "DIVISION"
  }
]
```

**다양한 검색 테스트**:
- `keyword=팀` → 모든 팀 조직 검색
- `keyword=본부` → 모든 본부 검색
- `keyword=존재하지않는이름` → 빈 배열 반환 확인

---

#### 2-3. 하위 부서 트리 조회
**API**: `GET /api/org/departments/{deptId}/subtree`

**예시**: 개발본부 ID로 요청
**예상 응답**: 개발본부와 그 하위의 모든 팀들

**✅ 체크포인트**:
- 지정한 부서와 그 하위만 반환되는지 확인
- 다른 본부의 부서는 포함되지 않는지 확인

---

#### 2-4. 깊이별 부서 조회
**API**: `GET /api/org/departments/by-depth?depth=1`

**예상 응답**: depth=1인 부서들만 (개발본부, 영업본부 등)

**다양한 깊이 테스트**:
- `depth=0` → 루트 부서만 (본사)
- `depth=1` → 1단계 부서들 (본부급)
- `depth=2` → 2단계 부서들 (팀급)

---

#### 2-5. 타입별 부서 조회
**API**: `GET /api/org/departments/by-type?type=TEAM`

**예상 응답**: 모든 TEAM 타입 부서들

**다양한 타입 테스트**:
- `type=COMPANY` → 최상위 조직
- `type=DIVISION` → 본부급 조직
- `type=TEAM` → 팀급 조직

---

#### 2-6. 부서 통계 조회
**API**: `GET /api/org/departments/{deptId}/statistics`

**예상 응답**:
```json
{
  "deptId": "660e8400-...",
  "deptName": "개발본부",
  "totalEmployees": 0,
  "activeEmployees": 0,
  "directChildCount": 3,
  "totalChildCount": 3
}
```

**✅ 체크포인트**:
- `directChildCount`: 직속 하위 부서 수
- `totalChildCount`: 모든 하위 부서 수 (재귀적)
- 직원 수는 아직 0 (직원 배치 전)

---

### Phase 3: 부서 수정 (CRUD - Update)

#### 3-1. 부서 정보 수정
**API**: `PATCH /api/org/departments/{deptId}`

**요청 Body** (백엔드팀 이름 변경):
```json
{
  "name": "백엔드개발팀",
  "type": "TEAM"
}
```

**예상 응답** (200 OK): 수정된 부서 정보

**✅ 체크포인트**:
- 이름이 변경되었는지 확인
- 다른 필드(orgPath, depth 등)는 유지되는지 확인

---

#### 3-2. 부서 이동 (조직 구조 변경)
**API**: `PUT /api/org/departments/{deptId}/move`

**헤더 추가 필요**:
- `X-User-Id`: 사용자 ID (권한 검증용)

**요청 Body** (백엔드팀을 영업본부로 이동):
```json
{
  "newParentId": "{영업본부_deptId}"
}
```

**예상 응답** (204 No Content)

**검증 방법**:
1. 전체 조직도 다시 조회 (`GET /api/org/departments`)
2. 백엔드팀이 영업본부 하위로 이동했는지 확인
3. `orgPath`가 자동으로 재계산되었는지 확인

**✅ 체크포인트**:
- 부서가 새로운 위치로 이동했는지 확인
- 하위 부서들의 경로도 함께 업데이트되었는지 확인
- 이전 위치에서는 제거되었는지 확인

---

### Phase 4: 고급 기능 테스트

#### 4-1. 스코프 기반 조직도 조회 (RBAC 통합)
**API**: `GET /api/org/departments/scoped`

**헤더 추가 필요**:
- `X-User-Id`: 사용자 ID

**사용자 권한에 따른 결과**:
- **ADMIN**: 전체 조직도 반환
- **TEAM_LEAD**: 자신 부서 + 하위 부서만 반환
- **MEMBER**: 자신 부서만 반환

**✅ 체크포인트**:
- 사용자 권한에 따라 다른 결과가 반환되는지 확인
- 접근 권한이 없는 부서는 표시되지 않는지 확인

---

#### 4-2. 부서별 사용자 목록 조회
**API**: `GET /api/org/departments/{deptId}/members?includeSubDepartments=true`

**쿼리 파라미터**:
- `includeSubDepartments`: true → 하위 부서 포함, false → 해당 부서만

**예상 응답**:
```json
{
  "deptId": "660e8400-...",
  "deptName": "개발본부",
  "directMembers": [],
  "allMembers": [],
  "includeSubDepartments": true
}
```

**참고**: 직원 데이터가 없으면 빈 배열 반환

---

#### 4-3. 부서 상태 관리

**부서 비활성화**:
**API**: `POST /api/org/departments/{deptId}/deactivate`

**헤더 추가 필요**:
- `X-User-Id`: 사용자 ID

**조건**:
- 활성 하위 부서가 없어야 함
- 소속 직원이 있어도 비활성화 가능 (경고 로그 출력)

**예상 응답** (204 No Content)

---

**부서 활성화**:
**API**: `POST /api/org/departments/{deptId}/activate`

**조건**:
- 상위 부서가 활성 상태여야 함

**예상 응답** (204 No Content)

**✅ 체크포인트**:
1. 비활성화 후 전체 조직도 조회 시 상태 확인
2. 비활성화된 부서는 신규 직원 배치 불가
3. 활성화 시 정상 작동으로 복구

---

### Phase 5: 부서 삭제 (CRUD - Delete)

#### 5-1. 하위 부서 삭제 (리프 노드부터)
**API**: `DELETE /api/org/departments/{deptId}`

**헤더 추가 필요**:
- `X-User-Id`: 사용자 ID

**삭제 조건**:
1. ❌ 하위 부서가 있으면 삭제 불가
2. ❌ 소속 활성 직원이 있으면 삭제 불가

**테스트 순서**:
1. 백엔드팀 삭제 시도 (하위 부서 없음 → 성공)
2. 개발본부 삭제 시도 (하위 부서 있음 → 실패, 400 Bad Request)
3. 하위 부서들을 모두 삭제한 후 개발본부 삭제 (성공)

**예상 응답**:
- 성공: 204 No Content
- 실패: 400 Bad Request (하위 부서 있음)

---

## 시나리오별 테스트

### 시나리오 1: 신규 회사 조직도 구축

**목표**: 처음부터 완전한 조직도 생성

**단계**:
1. ✅ 회사 생성 (COMPANY)
2. ✅ 각 본부 생성 (DIVISION) - 개발, 영업, 마케팅, 인사
3. ✅ 각 본부 하위에 팀 생성 (TEAM)
4. ✅ 전체 조직도 조회로 구조 확인
5. ✅ 각 레벨별 조회로 계층 구조 검증

---

### 시나리오 2: 조직 개편 (부서 이동)

**목표**: 부서를 다른 상위 부서로 이동

**단계**:
1. ✅ 현재 조직도 조회 (이동 전)
2. ✅ 부서 이동 API 호출 (`PUT /move`)
3. ✅ 전체 조직도 다시 조회 (이동 후)
4. ✅ 이동한 부서의 `orgPath` 변경 확인
5. ✅ 하위 부서들의 경로도 자동 업데이트 확인

**테스트 케이스**:
- ✅ 정상 이동 (팀을 다른 본부로)
- ❌ 순환 참조 방지 (자신의 하위로 이동 시도)
- ❌ 존재하지 않는 부서로 이동 시도

---

### 시나리오 3: 부서 통폐합

**목표**: 불필요한 부서 삭제 및 통합

**단계**:
1. ✅ 삭제할 부서의 직원들을 다른 부서로 이동 (User API 사용)
2. ✅ 하위 부서들을 상위 부서로 이동
3. ✅ 빈 부서 삭제
4. ✅ 조직도 재조회로 구조 확인

---

### 시나리오 4: 권한 기반 조회 테스트

**목표**: 사용자별로 다른 조직도 보기

**단계**:
1. ✅ ADMIN 사용자로 스코프 조회 → 전체 조직도
2. ✅ TEAM_LEAD 사용자로 스코프 조회 → 자신 부서 + 하위
3. ✅ MEMBER 사용자로 스코프 조회 → 자신 부서만

**필요 사항**:
- User API로 다양한 권한의 사용자 생성
- RBAC API로 역할 할당

---

## 테스트 데이터

### 추천 조직 구조

```
본사 (COMPANY)
├── 개발본부 (DIVISION)
│   ├── 백엔드팀 (TEAM)
│   ├── 프론트엔드팀 (TEAM)
│   └── DevOps팀 (TEAM)
├── 영업본부 (DIVISION)
│   ├── 국내영업팀 (TEAM)
│   └── 해외영업팀 (TEAM)
├── 마케팅본부 (DIVISION)
│   ├── 디지털마케팅팀 (TEAM)
│   └── 브랜드전략팀 (TEAM)
└── 인사본부 (DIVISION)
    ├── 채용팀 (TEAM)
    └── 교육팀 (TEAM)
```

### 부서 타입 (DepartmentType Enum)

| 타입 | 설명 | 사용 예시 |
|------|------|----------|
| `COMPANY` | 최상위 조직 | 본사, 계열사 |
| `DIVISION` | 본부급 조직 | 개발본부, 영업본부 |
| `TEAM` | 팀급 조직 | 백엔드팀, 프론트엔드팀 |
| `DEPARTMENT` | 부서 | 일반 부서 |
| `GROUP` | 그룹 | 프로젝트 그룹 |
| `CENTER` | 센터 | 연구센터, 지원센터 |

---

## 에러 케이스 테스트

### 1. 잘못된 부서 생성
- ❌ 존재하지 않는 `parentId` 사용
  - 예상: 400 Bad Request

### 2. 순환 참조 방지
- ❌ 부서를 자신의 하위로 이동
  - 예상: 400 Bad Request, "Cannot move to descendant"

### 3. 삭제 제약 조건
- ❌ 하위 부서가 있는 부서 삭제
  - 예상: 400 Bad Request
- ❌ 소속 직원이 있는 부서 삭제
  - 예상: 400 Bad Request

### 4. 권한 없음
- ❌ X-User-Id 헤더 없이 권한 필요한 API 호출
  - 예상: 401 Unauthorized
- ❌ 권한이 없는 사용자가 다른 부서 접근
  - 예상: 403 Forbidden

---

## 통합 테스트 체크리스트

### ✅ 기본 CRUD
- [ ] 부서 생성 (루트)
- [ ] 부서 생성 (하위)
- [ ] 전체 조직도 조회
- [ ] 부서 정보 수정
- [ ] 부서 삭제

### ✅ 조회 기능
- [ ] 키워드 검색
- [ ] 하위 부서 트리 조회
- [ ] 깊이별 조회
- [ ] 타입별 조회
- [ ] 부서 통계 조회
- [ ] 부서별 사용자 목록 조회

### ✅ 고급 기능
- [ ] 부서 이동
- [ ] 스코프 기반 조회
- [ ] 부서 비활성화/활성화

### ✅ 에러 처리
- [ ] 유효성 검증 실패
- [ ] 순환 참조 방지
- [ ] 삭제 제약 조건
- [ ] 권한 검증

---

## 다음 단계

Organization API 테스트 완료 후:
1. **User API 테스트**: 직원 생성 및 부서 배치
2. **RBAC API 테스트**: 역할 및 권한 관리
3. **통합 시나리오 테스트**: 조직, 직원, 권한을 모두 활용한 실제 업무 시나리오

---

## 문제 발생 시

### Swagger UI에서 헤더 추가 방법
1. API 엔드포인트 클릭
2. "Try it out" 버튼 클릭
3. Parameters 섹션에서 헤더 값 입력
4. "Execute" 버튼 클릭

### 공통 문제 해결
- **404 Not Found**: URL 경로 확인 (`/api/org/departments`)
- **400 Bad Request**: 요청 Body JSON 형식 확인
- **500 Internal Server Error**: 서버 로그 확인, DB 연결 상태 확인

---

**테스트 시작**: Phase 1부터 순서대로 진행하시면 됩니다! 🚀
