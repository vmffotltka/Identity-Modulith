# API 테스트 시나리오 - Organization Management

## 🎯 테스트 환경
- **Base URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **기본 인증**: Mock 인증 (자동)
- **테넌트**: `default-tenant`

---

## 📋 초기 데이터 (SQL 자동 삽입)

### 부서 (5개)
| dept_id | name | code | type | depth | parent |
|---------|------|------|------|-------|--------|
| 000...001 | 넥스프론 | NEXFRON | COMPANY | 0 | null |
| 000...002 | 개발본부 | DEV-DIV | DIVISION | 1 | 넥스프론 |
| 000...003 | 영업본부 | SALES-DIV | DIVISION | 1 | 넥스프론 |
| 000...004 | 백엔드팀 | DEV-BE | TEAM | 2 | 개발본부 |
| 000...005 | 프론트엔드팀 | DEV-FE | TEAM | 2 | 개발본부 |

### 사용자 (3명)
| login_id | name | role | dept |
|----------|------|------|------|
| admin | 시스템관리자 | ADMIN | - |
| dev.lead | 김팀장 | TEAM_LEAD | 백엔드팀 |
| dev.member | 이개발 | MEMBER | 백엔드팀 |

---

## 🧪 테스트 시나리오

### Scenario 1: 전체 조직도 조회 ✅

**권한**: 모든 사용자 가능 (org:read 권한)

**GET** `/api/org/departments`

**선택적 헤더** (조회는 어떤 사용자든 가능):
```
X-User-Id: 10000000-0000-0000-0000-000000000001  (ADMIN)
X-User-Id: 10000000-0000-0000-0000-000000000002  (TEAM_LEAD)
X-User-Id: 10000000-0000-0000-0000-000000000003  (MEMBER)
```

**예상 응답 (200 OK)**:
```json
[
  {
    "deptId": "00000000-0000-0000-0000-000000000001",
    "name": "넥스프론",
    "code": "NEXFRON",
    "type": "COMPANY",
    "depth": 0,
    "orgPath": "/00000000-0000-0000-0000-000000000001",
    "status": "ACTIVE",
    "children": [
      {
        "deptId": "00000000-0000-0000-0000-000000000002",
        "name": "개발본부",
        "code": "DEV-DIV",
        "type": "DIVISION",
        "depth": 1,
        "children": [
          {
            "name": "백엔드팀",
            "code": "DEV-BE"
          },
          {
            "name": "프론트엔드팀",
            "code": "DEV-FE"
          }
        ]
      },
      {
        "name": "영업본부",
        "code": "SALES-DIV",
        "type": "DIVISION"
      }
    ]
  }
]
```

**검증 항목**:
- ✅ 5개 부서 모두 조회됨
- ✅ 트리 구조 (children)로 표시됨
- ✅ depth가 정확함 (0, 1, 2)
- ✅ code 필드가 포함됨
- ✅ 모든 사용자(ADMIN, TEAM_LEAD, MEMBER)가 조회 가능

---

### Scenario 2: 새로운 부서 생성 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/org/departments`

#### 2-1. DevOps팀 생성 (개발본부 하위)

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "DevOps팀",
  "type": "TEAM",
  "code": "DEV-OPS",
  "parentId": "00000000-0000-0000-0000-000000000002"
}
```

**예상 응답 (201 Created)**:
```json
{
  "deptId": "uuid-generated",
  "name": "DevOps팀",
  "code": "DEV-OPS",
  "type": "TEAM",
  "depth": 2,
  "orgPath": "/00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002/uuid",
  "parentId": "00000000-0000-0000-0000-000000000002",
  "status": "ACTIVE"
}
```

**검증 항목**:
- ✅ depth = 2 (개발본부의 자식이므로)
- ✅ orgPath에 부모 경로 포함
- ✅ status = ACTIVE

#### 2-2. 국내영업팀 생성 (영업본부 하위)
```json
{
  "name": "국내영업팀",
  "type": "TEAM",
  "code": "SALES-DOM",
  "parentId": "00000000-0000-0000-0000-000000000003"
}
```

#### 2-3. 해외영업팀 생성 (영업본부 하위)
```json
{
  "name": "해외영업팀",
  "type": "TEAM",
  "code": "SALES-INTL",
  "parentId": "00000000-0000-0000-0000-000000000003"
}
```

#### 2-4. 커스텀 타입 부서 생성 (예: 센터)
```json
{
  "name": "서울센터",
  "type": "CUSTOM",
  "code": "CENTER-SEL",
  "customTypeName": "센터",
  "parentId": "00000000-0000-0000-0000-000000000001"
}
```

**검증 항목**:
- ✅ customTypeName = "센터"가 저장됨
- ✅ type = CUSTOM 정상 처리

#### 2-5. 권한 없는 사용자로 생성 시도 (TEAM_LEAD) ❌

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000002
```

**예상 응답 (403 Forbidden)**:
```json
{
  "code": "INSUFFICIENT_PERMISSION",
  "message": "org:create 권한이 필요합니다"
}
```

---

### Scenario 3: 부서 정보 수정 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**PATCH** `/api/org/departments/{deptId}`

#### 3-1. 부서명 변경 (ADMIN)

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "백엔드개발팀"
}
```

**사용할 deptId**: `00000000-0000-0000-0000-000000000004` (백엔드팀)

**예상 응답 (200 OK)**:
```json
{
  "deptId": "00000000-0000-0000-0000-000000000004",
  "name": "백엔드개발팀",
  "code": "DEV-BE",
  "type": "TEAM"
}
```

**검증 항목**:
- ✅ name만 변경됨
- ✅ code는 그대로 유지

#### 3-2. 타입 변경 (ADMIN)
```json
{
  "type": "GROUP"
}
```

**검증 항목**:
- ✅ type이 TEAM → GROUP으로 변경됨

#### 3-3. 권한 없는 사용자로 시도 (MEMBER) ❌

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000003
```

**예상 응답 (403 Forbidden)**:
```json
{
  "code": "INSUFFICIENT_PERMISSION",
  "message": "권한이 부족합니다"
}
```

---

### Scenario 4: 부서 이동 (재조직) ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**PUT** `/api/org/departments/{deptId}/move`

#### 4-1. 백엔드팀을 영업본부로 이동 (ADMIN만 가능)

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**Request Body**:
```json
{
  "newParentId": "00000000-0000-0000-0000-000000000003"
}
```

**사용할 deptId**: `00000000-0000-0000-0000-000000000004`

**예상 응답 (200 OK)**:
```json
{
  "deptId": "00000000-0000-0000-0000-000000000004",
  "name": "백엔드팀",
  "parentId": "00000000-0000-0000-0000-000000000003",
  "depth": 2,
  "orgPath": "/00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000003/00000000-0000-0000-0000-000000000004"
}
```

**검증 항목**:
- ✅ parentId가 변경됨
- ✅ depth는 그대로 2 (영업본부 depth=1의 자식)
- ✅ orgPath가 새로운 부모 경로로 업데이트됨

#### 4-2. 권한 없는 사용자로 시도 (TEAM_LEAD) ❌

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000002
```

**예상 응답 (403 Forbidden)**:
```json
{
  "code": "INSUFFICIENT_PERMISSION",
  "message": "권한이 부족합니다"
}
```

---

### Scenario 5: 하위 부서 트리 조회 ✅

**권한**: 모든 사용자 가능 (org:read 권한)

**GET** `/api/org/departments/{deptId}/subtree`

**사용할 deptId**: `00000000-0000-0000-0000-000000000002` (개발본부)

**예상 응답 (200 OK)**:
```json
{
  "deptId": "00000000-0000-0000-0000-000000000002",
  "name": "개발본부",
  "code": "DEV-DIV",
  "children": [
    {
      "name": "프론트엔드팀",
      "code": "DEV-FE"
    },
    {
      "name": "DevOps팀",
      "code": "DEV-OPS"
    }
  ]
}
```

**검증 항목**:
- ✅ 개발본부의 직속 하위 부서만 표시
- ✅ 백엔드팀은 제외됨 (영업본부로 이동했으므로)

---

### Scenario 6: 부서 검색 ✅

**권한**: 모든 사용자 가능 (org:read 권한)

**GET** `/api/org/departments/search?keyword={keyword}`

#### 6-1. "팀"으로 검색
```
GET /api/org/departments/search?keyword=팀
```

**예상 응답 (200 OK)**:
```json
[
  {
    "name": "프론트엔드팀",
    "code": "DEV-FE"
  },
  {
    "name": "DevOps팀",
    "code": "DEV-OPS"
  },
  {
    "name": "국내영업팀",
    "code": "SALES-DOM"
  },
  {
    "name": "해외영업팀",
    "code": "SALES-INTL"
  },
  {
    "name": "백엔드개발팀",
    "code": "DEV-BE"
  }
]
```

**검증 항목**:
- ✅ 이름에 "팀"이 포함된 모든 부서 조회
- ✅ 정렬됨

#### 6-2. "DEV"로 검색 (코드 기준)
```
GET /api/org/departments/search?keyword=DEV
```

**예상 결과**: 개발본부, 백엔드팀, 프론트엔드팀, DevOps팀

---

### Scenario 7: 타입별 조회 ✅

**권한**: 모든 사용자 가능 (org:read 권한)

**GET** `/api/org/departments/by-type?type={type}`

#### 7-1. TEAM 타입 조회
```
GET /api/org/departments/by-type?type=TEAM
```

**예상 응답 (200 OK)**:
```json
[
  {"name": "백엔드개발팀", "code": "DEV-BE", "type": "TEAM"},
  {"name": "프론트엔드팀", "code": "DEV-FE", "type": "TEAM"},
  {"name": "DevOps팀", "code": "DEV-OPS", "type": "TEAM"},
  {"name": "국내영업팀", "code": "SALES-DOM", "type": "TEAM"},
  {"name": "해외영업팀", "code": "SALES-INTL", "type": "TEAM"}
]
```

---

### Scenario 8: 깊이별 조회 ✅

**권한**: 모든 사용자 가능 (org:read 권한)

**GET** `/api/org/departments/by-depth?depth={depth}`

#### 8-1. Depth 1 조회 (본부급)
```
GET /api/org/departments/by-depth?depth=1
```

**예상 응답 (200 OK)**:
```json
[
  {"name": "개발본부", "depth": 1},
  {"name": "영업본부", "depth": 1}
]
```

---

### Scenario 9: 부서 통계 조회 ✅

**권한**: 모든 사용자 가능 (org:read 권한)

**GET** `/api/org/departments/{deptId}/statistics`

**사용할 deptId**: `00000000-0000-0000-0000-000000000002` (개발본부)

**예상 응답 (200 OK)**:
```json
{
  "deptId": "00000000-0000-0000-0000-000000000002",
  "name": "개발본부",
  "totalSubDepartments": 2,
  "totalMembers": 2,
  "activeMembers": 2,
  "avgDepth": 2.0
}
```

---

### Scenario 10: 부서 비활성화 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/org/departments/{deptId}/deactivate`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 deptId**: `00000000-0000-0000-0000-000000000005` (프론트엔드팀)

**예상 응답 (200 OK)**:
```json
{
  "deptId": "00000000-0000-0000-0000-000000000005",
  "name": "프론트엔드팀",
  "status": "INACTIVE",
  "deactivatedAt": "2026-02-06T16:30:00"
}
```

**검증 항목**:
- ✅ status = INACTIVE
- ✅ deactivatedAt 설정됨

---

### Scenario 11: 부서 활성화 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/org/departments/{deptId}/activate`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 deptId**: `00000000-0000-0000-0000-000000000005`

**예상 응답 (200 OK)**:
```json
{
  "deptId": "00000000-0000-0000-0000-000000000005",
  "name": "프론트엔드팀",
  "status": "ACTIVE",
  "deactivatedAt": null
}
```

---

### Scenario 12: 부서별 사용자 목록 조회 ✅

**권한**: 모든 사용자 가능 (org:read 권한)

**GET** `/api/org/departments/{deptId}/members`

**사용할 deptId**: `00000000-0000-0000-0000-000000000004` (백엔드팀)

**예상 응답 (200 OK)**:
```json
[
  {
    "agentId": "10000000-0000-0000-0000-000000000002",
    "loginId": "dev.lead",
    "name": "김팀장",
    "deptId": "00000000-0000-0000-0000-000000000004",
    "departmentName": "백엔드개발팀"
  },
  {
    "agentId": "10000000-0000-0000-0000-000000000003",
    "loginId": "dev.member",
    "name": "이개발",
    "deptId": "00000000-0000-0000-0000-000000000004",
    "departmentName": "백엔드개발팀"
  }
]
```

---

### Scenario 13: 부서 삭제 (실패 케이스) ❌

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**DELETE** `/api/org/departments/{deptId}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**사용할 deptId**: `00000000-0000-0000-0000-000000000002` (개발본부)

**예상 응답 (409 Conflict)**:
```json
{
  "code": "CHILD_DEPT_EXISTS",
  "message": "하위 부서가 존재하여 삭제할 수 없습니다"
}
```

**검증 항목**:
- ✅ 하위 부서가 있으면 삭제 불가

---

### Scenario 14: 부서 삭제 (성공 케이스) ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

#### 사전 준비: DevOps팀 삭제 (하위 부서 없음)

**DELETE** `/api/org/departments/{DevOps팀_deptId}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**예상 응답 (204 No Content)**

**검증 항목**:
- ✅ 하위 부서 없음
- ✅ 소속 직원 없음
- ✅ 삭제 성공

---

## 🎯 테스트 체크리스트

### 부서 생성
- [x] 루트 부서 생성
- [x] 하위 부서 생성
- [x] code 필드 필수 확인
- [x] customTypeName (CUSTOM 타입)

### 부서 조회
- [x] 전체 조직도 (트리)
- [x] 하위 트리
- [x] 검색 (이름, 코드)
- [x] 타입별 조회
- [x] 깊이별 조회
- [x] 부서 통계

### 부서 수정
- [x] 이름 변경
- [x] 타입 변경
- [x] 부서 이동

### 부서 상태 관리
- [x] 비활성화
- [x] 활성화

### 부서 삭제
- [x] 하위 부서 있을 때 실패
- [x] 소속 직원 있을 때 실패
- [x] 조건 충족 시 성공

---

## 📊 최종 조직 구조 (모든 테스트 완료 후)

```
넥스프론 (NEXFRON)
├── 개발본부 (DEV-DIV)
│   └── 프론트엔드팀 (DEV-FE)
├── 영업본부 (SALES-DIV)
│   ├── 백엔드개발팀 (DEV-BE) ← 이동됨
│   ├── 국내영업팀 (SALES-DOM)
│   └── 해외영업팀 (SALES-INTL)
└── 서울센터 (CENTER-SEL) [CUSTOM: "센터"]
```

---

## 🚀 빠른 시작

1. **애플리케이션 실행**
   ```bash
   .\gradlew bootRun
   ```

2. **Swagger UI 접속**
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

3. **Scenario 1부터 순차적으로 테스트**

4. **각 단계마다 응답 확인 및 검증**

---

## 🔑 사용자별 테스트 가이드

### ADMIN (10000000-0000-0000-0000-000000000001)
**가능한 작업**: 전체 권한
- ✅ 조회 (Scenario 1, 5-9, 12)
- ✅ 생성 (Scenario 2)
- ✅ 수정 (Scenario 3)
- ✅ 이동 (Scenario 4)
- ✅ 비활성화/활성화 (Scenario 10-11)
- ✅ 삭제 (Scenario 13-14)

**테스트 순서**:
```
1. Scenario 1 조회 (X-User-Id: 10000000-0000-0000-0000-000000000001)
2. Scenario 2 생성 (필수 헤더)
3. Scenario 3 수정 (필수 헤더)
4. Scenario 4 이동 (필수 헤더)
5. Scenario 10-11 상태 관리 (필수 헤더)
6. Scenario 13-14 삭제 (필수 헤더)
```

---

### TEAM_LEAD (10000000-0000-0000-0000-000000000002)
**가능한 작업**: 조회만 가능
- ✅ 조회 (Scenario 1, 5-9, 12)
- ❌ 생성, 수정, 이동, 삭제

**테스트 순서**:
```
1. Scenario 1 조회 (X-User-Id: 10000000-0000-0000-0000-000000000002) ✅
2. Scenario 2-5 생성 시도 (403 Forbidden 예상) ❌
3. Scenario 3-3 수정 시도 (403 Forbidden 예상) ❌
4. Scenario 4-2 이동 시도 (403 Forbidden 예상) ❌
5. Scenario 5-9 조회 (정상 작동) ✅
```

---

### MEMBER (10000000-0000-0000-0000-000000000003)
**가능한 작업**: 조회만 가능
- ✅ 조회 (Scenario 1, 5-9, 12)
- ❌ 생성, 수정, 이동, 삭제

**테스트 순서**:
```
1. Scenario 1 조회 (X-User-Id: 10000000-0000-0000-0000-000000000003) ✅
2. Scenario 2-5 생성 시도 (403 Forbidden 예상) ❌
3. Scenario 3-3 수정 시도 (403 Forbidden 예상) ❌
4. Scenario 5-9 조회 (정상 작동) ✅
```

---

## 📋 X-User-Id 헤더 사용법

### Swagger UI에서
1. **Authorize 버튼** 클릭 (자물쇠 아이콘)
2. **또는** 각 API 실행 시 **Parameters** 섹션에 헤더 추가

### cURL에서
```bash
curl -X GET "http://localhost:8080/api/org/departments" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000001"
```

### Postman에서
```
Headers 탭:
Key: X-User-Id
Value: 10000000-0000-0000-0000-000000000001
```

---

## ✅ 성공 기준

- 모든 API가 예상된 상태 코드 반환
- code, customTypeName 필드 정상 작동
- 트리 구조(children) 정확히 표시
- depth, orgPath 자동 계산
- 부서 이동 시 경로 업데이트
- 제약 조건 (하위 부서, 소속 직원) 정상 작동

🎉 **Happy Testing!**

