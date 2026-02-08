# 🔍 500 오류 디버깅 가이드

## 🎯 현재 상황

**API 요청**: `PUT /api/org/departments/{deptId}/move`
**응답**: `500 Internal Server Error`
**콘솔**: 오류 로그 없음 ❌

---

## ✅ 수정 완료

### 1. 예외 핸들러에 로깅 추가

**파일**: `OrganizationExceptionHandler.java`

**변경 전**:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
    // 클라이언트에는 상세 정보 미노출
    ErrorResponse response = ErrorResponse.builder()
            .code("INTERNAL_ERROR")
            .message("서버 오류가 발생했습니다")
            .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
}
```

**변경 후**:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
    // 🔴 실제 오류 로깅 추가
    log.error("[ORG] 예상치 못한 오류 발생: {}", e.getMessage(), e);
    
    // 클라이언트에는 상세 정보 미노출
    ErrorResponse response = ErrorResponse.builder()
            .code("INTERNAL_ERROR")
            .message("서버 오류가 발생했습니다")
            .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
}
```

---

## 🚀 다음 단계: 오류 확인

### 1. 애플리케이션 재시작

**IntelliJ에서**:
```
1. Stop 버튼 (빨간 네모)
2. Run 버튼 (녹색 화살표)
```

**Gradle에서**:
```bash
# Ctrl+C로 중지 후
./gradlew bootRun
```

---

### 2. API 다시 호출

**Swagger UI** 또는 **Postman**에서:

```http
PUT http://localhost:8080/api/org/departments/00000000-0000-0000-0000-000000000004/move
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "newParentId": "00000000-0000-0000-0000-000000000003"
}
```

---

### 3. 콘솔에서 오류 로그 확인

이제 콘솔에 **빨간색 ERROR 로그**가 출력됩니다:

```
2026-02-07 23:14:05.123 ERROR [http-nio-8080-exec-3] [ORG] 예상치 못한 오류 발생: ...
java.lang.NullPointerException: ...
    at com.nexfron.identitymodulith.organization.application.service.DepartmentServiceImpl.getAccessibleDepartmentIds(...)
    at com.nexfron.identitymodulith.organization.application.service.DepartmentServiceImpl.validateMoveDepartment(...)
    ...
```

---

## 🔍 예상되는 오류 원인

### 가능성 1: DataScopeLevel이 NULL

**증상**:
```java
DataScopeLevel level = userView.getRoleLevel();  // null 반환
if (level.canSeeWholeTenant()) {  // NullPointerException!
```

**원인**: 사용자의 역할에 `data_scope_level`이 설정되지 않음

**해결책**: 
```sql
-- rbac_roles 테이블 확인
SELECT role_id, name, type, data_scope_level 
FROM rbac_roles 
WHERE tenant_id = 'default-tenant';

-- NULL이면 업데이트
UPDATE rbac_roles 
SET data_scope_level = 'ALL' 
WHERE name = 'ADMIN' AND data_scope_level IS NULL;
```

---

### 가능성 2: 사용자 정보 조회 실패

**증상**:
```
OrgUserView userView = orgUserPort.findOrgInfoByUserId(...)
    .orElseThrow(...);  // 예외 발생
```

**원인**: User 모듈 연동 오류

**해결책**: 로그에서 정확한 예외 메시지 확인 필요

---

### 가능성 3: 부서 정보 없음

**증상**:
```java
String myDeptId = userView.getDeptId();  // null 반환
if (myDeptId == null) {
    throw new OrganizationException(...);  // 예외 발생
}
```

**원인**: ADMIN 사용자가 특정 부서에 소속되지 않음

**해결책**: 
```sql
-- user_agents 확인
SELECT agent_id, name, dept_id 
FROM user_agents 
WHERE agent_id = '10000000-0000-0000-0000-000000000001';

-- dept_id가 NULL이면 업데이트
UPDATE user_agents 
SET dept_id = '00000000-0000-0000-0000-000000000001'  -- 넥스프론
WHERE agent_id = '10000000-0000-0000-0000-000000000001';
```

---

## 📋 체크리스트

재시작 후 다음을 확인하세요:

- [ ] 콘솔에 ERROR 로그 출력되는가?
- [ ] 로그에 스택 트레이스가 포함되어 있는가?
- [ ] 어떤 메서드에서 오류가 발생했는가?
- [ ] NULL 값 때문인가? 데이터 누락 때문인가?

---

## 🎯 다음 단계

1. **애플리케이션 재시작**
2. **API 다시 호출**
3. **콘솔 로그 확인**
4. **로그 내용을 복사해서 알려주세요**

그러면 정확한 원인을 파악하고 해결하겠습니다! 🚀

---

**작성일**: 2026-02-07  
**수정 사항**: OrganizationExceptionHandler에 상세 로깅 추가

