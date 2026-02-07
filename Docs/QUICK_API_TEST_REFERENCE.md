# Organization API 빠른 테스트 참조

## 🚀 빠른 시작 (5분 테스트)

### Step 1: 본사 생성
```json
POST /api/org/departments
{
  "name": "넥스프론",
  "type": "COMPANY",
  "parentId": null
}
```
→ **deptId 복사**: `_________________`

---

### Step 2: 개발본부 생성
```json
POST /api/org/departments
{
  "name": "개발본부",
  "type": "DIVISION",
  "parentId": "[위에서 복사한 deptId]"
}
```
→ **deptId 복사**: `_________________`

---

### Step 3: 백엔드팀 생성
```json
POST /api/org/departments
{
  "name": "백엔드팀",
  "type": "TEAM",
  "parentId": "[개발본부 deptId]"
}
```

---

### Step 4: 전체 조직도 확인
```
GET /api/org/departments
```
→ 3단계 트리 구조 확인!

---

## 📋 전체 테스트 데이터 (복사해서 사용)

### 1️⃣ 본사
```json
{"name": "넥스프론", "type": "COMPANY", "parentId": null}
```

### 2️⃣ 개발본부 (본사 하위)
```json
{"name": "개발본부", "type": "DIVISION", "parentId": "[본사_ID]"}
```

### 3️⃣ 영업본부 (본사 하위)
```json
{"name": "영업본부", "type": "DIVISION", "parentId": "[본사_ID]"}
```

### 4️⃣ 백엔드팀 (개발본부 하위)
```json
{"name": "백엔드팀", "type": "TEAM", "parentId": "[개발본부_ID]"}
```

### 5️⃣ 프론트엔드팀 (개발본부 하위)
```json
{"name": "프론트엔드팀", "type": "TEAM", "parentId": "[개발본부_ID]"}
```

### 6️⃣ DevOps팀 (개발본부 하위)
```json
{"name": "DevOps팀", "type": "TEAM", "parentId": "[개발본부_ID]"}
```

### 7️⃣ 국내영업팀 (영업본부 하위)
```json
{"name": "국내영업팀", "type": "TEAM", "parentId": "[영업본부_ID]"}
```

### 8️⃣ 해외영업팀 (영업본부 하위)
```json
{"name": "해외영업팀", "type": "TEAM", "parentId": "[영업본부_ID]"}
```

---

## 🔍 조회 API 빠른 테스트

### 전체 조직도
```
GET /api/org/departments
```

### 키워드 검색
```
GET /api/org/departments/search?keyword=개발
GET /api/org/departments/search?keyword=팀
```

### 깊이별 조회
```
GET /api/org/departments/by-depth?depth=0  (본사만)
GET /api/org/departments/by-depth?depth=1  (본부들)
GET /api/org/departments/by-depth?depth=2  (팀들)
```

### 타입별 조회
```
GET /api/org/departments/by-type?type=COMPANY
GET /api/org/departments/by-type?type=DIVISION
GET /api/org/departments/by-type?type=TEAM
```

### 하위 부서 트리
```
GET /api/org/departments/[개발본부_ID]/subtree
```

### 부서 통계
```
GET /api/org/departments/[개발본부_ID]/statistics
```

---

## 🛠️ 수정/이동 API

### 부서 이름 변경
```json
PATCH /api/org/departments/[백엔드팀_ID]
{
  "name": "백엔드개발팀",
  "type": "TEAM"
}
```

### 부서 이동
```json
PUT /api/org/departments/[백엔드팀_ID]/move
Headers: X-User-Id: [사용자ID]
{
  "newParentId": "[영업본부_ID]"
}
```

---

## 🔐 상태 관리 API

### 비활성화
```
POST /api/org/departments/[부서ID]/deactivate
Headers: X-User-Id: [사용자ID]
```

### 활성화
```
POST /api/org/departments/[부서ID]/activate
Headers: X-User-Id: [사용자ID]
```

---

## 🗑️ 삭제 API

### 부서 삭제
```
DELETE /api/org/departments/[부서ID]
Headers: X-User-Id: [사용자ID]
```

**주의**: 
- 하위 부서가 있으면 삭제 불가
- 리프 노드(최하위)부터 삭제 가능

---

## 📊 기대 조직 구조

```
넥스프론 (COMPANY)
├── 개발본부 (DIVISION)
│   ├── 백엔드팀 (TEAM)
│   ├── 프론트엔드팀 (TEAM)
│   └── DevOps팀 (TEAM)
└── 영업본부 (DIVISION)
    ├── 국내영업팀 (TEAM)
    └── 해외영업팀 (TEAM)
```

---

## 🎯 필수 테스트 체크리스트

- [ ] ✅ 부서 생성 (8개)
- [ ] ✅ 전체 조직도 조회
- [ ] ✅ 키워드 검색
- [ ] ✅ 깊이별 조회
- [ ] ✅ 타입별 조회
- [ ] ✅ 하위 부서 트리 조회
- [ ] ✅ 부서 통계 조회
- [ ] ✅ 부서 이름 수정
- [ ] ✅ 부서 이동
- [ ] ✅ 부서 비활성화
- [ ] ✅ 부서 활성화
- [ ] ✅ 부서 삭제

---

## 💡 Swagger UI 사용 팁

1. **Try it out** 버튼 클릭
2. **Request body**에 JSON 붙여넣기
3. **Execute** 버튼 클릭
4. **Response body**에서 `deptId` 복사
5. 다음 요청의 `parentId`에 붙여넣기

---

## 🐛 자주 발생하는 오류

### 400 Bad Request
- JSON 형식 오류 확인
- `parentId` 값이 존재하는 부서 ID인지 확인

### 404 Not Found
- deptId가 올바른지 확인
- URL 경로 확인

### 500 Internal Server Error
- 서버 로그 확인
- DB 연결 상태 확인

---

**즐거운 테스트 되세요!** 🎉

