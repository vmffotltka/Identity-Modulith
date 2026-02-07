# Organization API 실전 테스트 시나리오

## 📋 테스트 환경
- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- 테스트 날짜: 2026-02-06

---

## Phase 1: 부서 생성 테스트

### 1️⃣ 최상위 부서(본사) 생성
**API**: `POST /api/org/departments`

**Request Body**:
```json
{
  "name": "넥스프론",
  "type": "COMPANY",
  "parentId": null
}
```

**기대 결과**:
- Status: `201 Created`
- `deptId` 생성됨 (UUID)
- `orgPath`: `/[deptId]`
- `depth`: 0
- `parentId`: null

**✅ 생성된 deptId 기록**: `_________________`

---

### 2️⃣ 개발본부 생성 (1차 하위)
**API**: `POST /api/org/departments`

**Request Body** (위의 deptId를 parentId에 입력):
```json
{
  "name": "개발본부",
  "type": "DIVISION",
  "parentId": "[본사_deptId]"
}
```

**기대 결과**:
- Status: `201 Created`
- `depth`: 1
- `orgPath`: `/[본사_deptId]/[개발본부_deptId]`
- `parentId`: [본사_deptId]

**✅ 생성된 deptId 기록**: `_________________`

---

### 3️⃣ 영업본부 생성 (1차 하위)
**API**: `POST /api/org/departments`

**Request Body**:
```json
{
  "name": "영업본부",
  "type": "DIVISION",
  "parentId": "[본사_deptId]"
}
```

**✅ 생성된 deptId 기록**: `_________________`

---

### 4️⃣ 백엔드팀 생성 (2차 하위)
**API**: `POST /api/org/departments`

**Request Body**:
```json
{
  "name": "백엔드팀",
  "type": "TEAM",
  "parentId": "[개발본부_deptId]"
}
```

**기대 결과**:
- `depth`: 2
- `orgPath`: `/[본사]/[개발본부]/[백엔드팀]`

**✅ 생성된 deptId 기록**: `_________________`

---

### 5️⃣ 프론트엔드팀 생성 (2차 하위)
**API**: `POST /api/org/departments`

**Request Body**:
```json
{
  "name": "프론트엔드팀",
  "type": "TEAM",
  "parentId": "[개발본부_deptId]"
}
```

**✅ 생성된 deptId 기록**: `_________________`

---

### 6️⃣ DevOps팀 생성 (2차 하위)
**API**: `POST /api/org/departments`

**Request Body**:
```json
{
  "name": "DevOps팀",
  "type": "TEAM",
  "parentId": "[개발본부_deptId]"
}
```

**✅ 생성된 deptId 기록**: `_________________`

---

### 7️⃣ 국내영업팀 생성 (2차 하위)
**API**: `POST /api/org/departments`

**Request Body**:
```json
{
  "name": "국내영업팀",
  "type": "TEAM",
  "parentId": "[영업본부_deptId]"
}
```

**✅ 생성된 deptId 기록**: `_________________`

---

### 8️⃣ 해외영업팀 생성 (2차 하위)
**API**: `POST /api/org/departments`

**Request Body**:
```json
{
  "name": "해외영업팀",
  "type": "TEAM",
  "parentId": "[영업본부_deptId]"
}
```

**✅ 생성된 deptId 기록**: `_________________`

---

## Phase 2: 부서 조회 테스트

### 9️⃣ 전체 조직도 조회
**API**: `GET /api/org/departments`

**기대 결과**:
```json
[
  {
    "deptId": "...",
    "name": "넥스프론",
    "type": "COMPANY",
    "children": [
      {
        "name": "개발본부",
        "children": [
          {"name": "백엔드팀"},
          {"name": "프론트엔드팀"},
          {"name": "DevOps팀"}
        ]
      },
      {
        "name": "영업본부",
        "children": [
          {"name": "국내영업팀"},
          {"name": "해외영업팀"}
        ]
      }
    ]
  }
]
```

**✅ 검증 포인트**:
- [ ] 모든 8개 부서가 표시됨
- [ ] 트리 구조가 올바름
- [ ] children 배열이 정확함

---

### 🔟 부서 검색 (키워드: "개발")
**API**: `GET /api/org/departments/search?keyword=개발`

**기대 결과**:
```json
[
  {
    "deptId": "...",
    "name": "개발본부",
    "type": "DIVISION"
  }
]
```

**추가 테스트**:
- `keyword=팀` → 5개 팀 조직 반환
- `keyword=본부` → 2개 본부 반환
- `keyword=없는이름` → 빈 배열 `[]`

---

### 1️⃣1️⃣ 하위 부서 트리 조회
**API**: `GET /api/org/departments/[개발본부_deptId]/subtree`

**기대 결과**:
```json
{
  "deptId": "[개발본부_deptId]",
  "name": "개발본부",
  "children": [
    {"name": "백엔드팀"},
    {"name": "프론트엔드팀"},
    {"name": "DevOps팀"}
  ]
}
```

**✅ 검증 포인트**:
- [ ] 개발본부와 그 하위만 반환
- [ ] 영업본부 관련 부서는 없음

---

### 1️⃣2️⃣ 깊이별 부서 조회 (depth=1)
**API**: `GET /api/org/departments/by-depth?depth=1`

**기대 결과**:
```json
[
  {
    "name": "개발본부",
    "depth": 1
  },
  {
    "name": "영업본부",
    "depth": 1
  }
]
```

**추가 테스트**:
- `depth=0` → 본사만 (1개)
- `depth=2` → 모든 팀 (5개)

---

### 1️⃣3️⃣ 타입별 부서 조회 (type=TEAM)
**API**: `GET /api/org/departments/by-type?type=TEAM`

**기대 결과**: 5개 팀 반환
```json
[
  {"name": "백엔드팀", "type": "TEAM"},
  {"name": "프론트엔드팀", "type": "TEAM"},
  {"name": "DevOps팀", "type": "TEAM"},
  {"name": "국내영업팀", "type": "TEAM"},
  {"name": "해외영업팀", "type": "TEAM"}
]
```

**추가 테스트**:
- `type=COMPANY` → 본사 (1개)
- `type=DIVISION` → 개발본부, 영업본부 (2개)

---

### 1️⃣4️⃣ 부서 통계 조회
**API**: `GET /api/org/departments/[개발본부_deptId]/statistics`

**기대 결과**:
```json
{
  "deptId": "[개발본부_deptId]",
  "deptName": "개발본부",
  "totalEmployees": 0,
  "activeEmployees": 0,
  "directChildCount": 3,
  "totalChildCount": 3
}
```

**✅ 검증 포인트**:
- `directChildCount`: 3 (직속 팀)
- `totalChildCount`: 3 (하위에 더 없음)
- 직원 수는 0 (아직 배치 안됨)

---

## Phase 3: 부서 수정 및 이동

### 1️⃣5️⃣ 부서 정보 수정
**API**: `PATCH /api/org/departments/[백엔드팀_deptId]`

**Request Body**:
```json
{
  "name": "백엔드개발팀",
  "type": "TEAM"
}
```

**기대 결과**:
- Status: `200 OK`
- 이름이 "백엔드개발팀"으로 변경됨
- 다른 필드는 유지됨

**✅ 검증**: 전체 조직도 다시 조회해서 이름 변경 확인

---

### 1️⃣6️⃣ 부서 이동 테스트
**API**: `PUT /api/org/departments/[백엔드개발팀_deptId]/move`

**Headers**:
- `X-Tenant-Id`: [테넌트 ID]
- `X-User-Id`: [사용자 ID]

**Request Body** (백엔드팀을 영업본부로 이동):
```json
{
  "newParentId": "[영업본부_deptId]"
}
```

**기대 결과**:
- Status: `204 No Content`

**✅ 검증**:
1. 전체 조직도 다시 조회
2. 백엔드팀이 영업본부 하위로 이동했는지 확인
3. `orgPath` 자동 재계산 확인

---

### 1️⃣7️⃣ 부서 다시 원위치 이동
**API**: `PUT /api/org/departments/[백엔드개발팀_deptId]/move`

**Request Body** (다시 개발본부로 이동):
```json
{
  "newParentId": "[개발본부_deptId]"
}
```

**기대 결과**: Status `204`, 원래 위치로 복귀

---

## Phase 4: 고급 기능 테스트

### 1️⃣8️⃣ 스코프 기반 조직도 조회
**API**: `GET /api/org/departments/scoped`

**Headers**:
- `X-User-Id`: [사용자 ID]

**기대 결과**:
- 사용자 권한에 따라 다른 결과
- ADMIN: 전체 조직도
- TEAM_LEAD: 자신 부서 + 하위
- MEMBER: 자신 부서만

**✅ 참고**: 이 테스트는 User 및 RBAC 설정 후 가능

---

### 1️⃣9️⃣ 부서별 사용자 목록 조회
**API**: `GET /api/org/departments/[개발본부_deptId]/members?includeSubDepartments=true`

**기대 결과**:
```json
{
  "deptId": "[개발본부_deptId]",
  "deptName": "개발본부",
  "directMembers": [],
  "allMembers": [],
  "includeSubDepartments": true
}
```

**✅ 참고**: 직원이 없으면 빈 배열

---

### 2️⃣0️⃣ 부서 비활성화
**API**: `POST /api/org/departments/[해외영업팀_deptId]/deactivate`

**Headers**:
- `X-User-Id`: [사용자 ID]

**기대 결과**:
- Status: `204 No Content`

**✅ 검증**:
1. 전체 조직도에서 상태 확인
2. 해당 부서에 직원 배치 시도 시 실패해야 함

---

### 2️⃣1️⃣ 부서 활성화
**API**: `POST /api/org/departments/[해외영업팀_deptId]/activate`

**기대 결과**:
- Status: `204 No Content`
- 정상 작동으로 복구

---

## Phase 5: 부서 삭제 테스트

### 2️⃣2️⃣ 하위 부서가 있는 부서 삭제 시도 (실패 예상)
**API**: `DELETE /api/org/departments/[개발본부_deptId]`

**Headers**:
- `X-User-Id`: [사용자 ID]

**기대 결과**:
- Status: `400 Bad Request`
- 에러 메시지: "하위 부서가 존재하여 삭제할 수 없습니다"

---

### 2️⃣3️⃣ 리프 노드 삭제 (성공)
**API**: `DELETE /api/org/departments/[해외영업팀_deptId]`

**기대 결과**:
- Status: `204 No Content`

**✅ 검증**: 전체 조직도에서 해당 부서가 사라졌는지 확인

---

### 2️⃣4️⃣ 순환 참조 방지 테스트 (실패 예상)
**API**: `PUT /api/org/departments/[개발본부_deptId]/move`

**Request Body** (자신의 하위인 백엔드팀을 부모로 시도):
```json
{
  "newParentId": "[백엔드팀_deptId]"
}
```

**기대 결과**:
- Status: `400 Bad Request`
- 에러 메시지: "Cannot move to descendant"

---

## 🎯 전체 테스트 체크리스트

### ✅ Phase 1: 부서 생성
- [ ] 1. 본사 생성
- [ ] 2. 개발본부 생성
- [ ] 3. 영업본부 생성
- [ ] 4. 백엔드팀 생성
- [ ] 5. 프론트엔드팀 생성
- [ ] 6. DevOps팀 생성
- [ ] 7. 국내영업팀 생성
- [ ] 8. 해외영업팀 생성

### ✅ Phase 2: 부서 조회
- [ ] 9. 전체 조직도 조회
- [ ] 10. 키워드 검색
- [ ] 11. 하위 부서 트리 조회
- [ ] 12. 깊이별 조회
- [ ] 13. 타입별 조회
- [ ] 14. 부서 통계 조회

### ✅ Phase 3: 수정 및 이동
- [ ] 15. 부서 정보 수정
- [ ] 16. 부서 이동
- [ ] 17. 부서 원위치 이동

### ✅ Phase 4: 고급 기능
- [ ] 18. 스코프 기반 조회
- [ ] 19. 부서별 사용자 목록
- [ ] 20. 부서 비활성화
- [ ] 21. 부서 활성화

### ✅ Phase 5: 삭제 및 에러 케이스
- [ ] 22. 하위 있는 부서 삭제 시도 (실패)
- [ ] 23. 리프 노드 삭제 (성공)
- [ ] 24. 순환 참조 방지 테스트

---

## 📝 테스트 결과 기록

| Phase | 테스트 항목 | 상태 | 비고 |
|-------|-----------|------|------|
| 1 | 부서 생성 | ⬜ | |
| 2 | 부서 조회 | ⬜ | |
| 3 | 부서 수정/이동 | ⬜ | |
| 4 | 고급 기능 | ⬜ | |
| 5 | 삭제/에러 | ⬜ | |

**범례**: ✅ 성공 / ❌ 실패 / ⚠️ 부분 성공 / ⬜ 미실행

---

## 🔧 트러블슈팅

### 문제 1: 404 Not Found
**원인**: URL 경로 오타  
**해결**: `/api/org/departments` 정확히 입력

### 문제 2: 400 Bad Request
**원인**: JSON 형식 오류 또는 필수 필드 누락  
**해결**: Request Body 다시 확인

### 문제 3: 500 Internal Server Error
**원인**: 서버 내부 오류  
**해결**: 서버 로그 확인, DB 연결 상태 확인

---

## 다음 단계

Organization API 테스트 완료 후:
1. **User API 테스트**: 직원 생성 및 부서 배치
2. **RBAC API 테스트**: 역할 및 권한 설정
3. **통합 시나리오 테스트**: 실제 업무 흐름 시뮬레이션

---

**테스트 시작!** 🚀

이 문서를 출력하여 각 단계를 체크하면서 진행하세요!

