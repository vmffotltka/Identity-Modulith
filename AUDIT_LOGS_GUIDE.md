# 📋 감사 로그(Audit Logs) 가이드

## 개요

감사 로그는 RBAC 시스템에서 발생하는 모든 보안 관련 작업을 기록하는 시스템입니다.
권한 변경, 역할 할당, 사용자 생성 등 중요한 모든 작업을 추적할 수 있습니다.

---

## 📊 감사 로그 테이블 구조

### 1. audit_logs (활성 감사 로그)
```
최근 6개월 정도의 활성 감사 로그를 저장합니다.
필드:
- audit_id: 감사 로그 고유 ID (UUID)
- tenant_id: 테넌트 ID (멀티테넌시)
- action: 수행된 작업 (CREATE, UPDATE, DELETE, ASSIGN, REVOKE)
- resource_type: 대상 리소스 타입 (ROLE, PERMISSION, AGENT, DEPARTMENT, ROLE_PERMISSION, AGENT_ROLE)
- resource_id: 대상 리소스 ID
- operator_id: 작업 수행자 ID
- changes: 변경 내용 (JSON 형식)
- timestamp: 작업 수행 일시
- remarks: 추가 정보/설명
- ip_address: 클라이언트 IP 주소
```

### 2. audit_logs_archive (아카이브된 감사 로그)
```
6개월 이상 된 감사 로그를 아카이브로 이동합니다.
구조는 audit_logs와 유사하며, archived_at 필드가 추가됩니다.
필드:
- id: 로그 ID
- tenant_id: 테넌트 ID
- action: 작업 유형
- resource_type: 리소스 타입
- resource_id: 리소스 ID
- operator_id: 작업 수행자
- changes: 변경 내용 (JSON)
- timestamp: 작업 발생 일시
- archived_at: 아카이브 일시
```

---

## 🔐 ACTION 타입 표준

### CREATE
```
새로운 리소스가 생성되었을 때 기록
- 권한 생성: CREATE PERMISSION
- 역할 생성: CREATE ROLE
- 사용자 생성: CREATE AGENT
- 부서 생성: CREATE DEPARTMENT
```

### UPDATE
```
기존 리소스가 수정되었을 때 기록
- 역할 설명 변경: UPDATE ROLE
- 사용자 상태 변경: UPDATE AGENT
- 권한 비활성화: UPDATE PERMISSION
```

### DELETE
```
리소스가 삭제되었을 때 기록 (논리적 삭제 권장)
- 역할 삭제: DELETE ROLE
- 사용자 삭제: DELETE AGENT
```

### ASSIGN
```
권한/역할을 할당했을 때 기록
- 역할에 권한 할당: ASSIGN ROLE_PERMISSION
- 사용자에게 역할 할당: ASSIGN AGENT_ROLE
```

### REVOKE
```
권한/역할을 회수했을 때 기록
- 역할에서 권한 회수: REVOKE ROLE_PERMISSION
- 사용자에서 역할 회수: REVOKE AGENT_ROLE
```

---

## 📝 RESOURCE_TYPE 표준

| 리소스 타입 | 설명 | 관련 액션 |
|-----------|------|---------|
| PERMISSION | 권한 | CREATE, UPDATE, DELETE |
| ROLE | 역할 | CREATE, UPDATE, DELETE |
| AGENT | 사용자 | CREATE, UPDATE, DELETE |
| DEPARTMENT | 부서 | CREATE, UPDATE, DELETE |
| ROLE_PERMISSION | 역할-권한 매핑 | ASSIGN, REVOKE |
| AGENT_ROLE | 사용자-역할 매핑 | ASSIGN, REVOKE |
| PERMISSION_GROUP | 권한 그룹 | CREATE, UPDATE, DELETE |

---

## 💾 Changes 필드 JSON 형식

### CREATE 작업 예시
```json
{
  "code": "user:create",
  "created_at": "2026-01-15T10:00:00Z"
}
```

### UPDATE 작업 예시
```json
{
  "field": "is_active",
  "old_value": false,
  "new_value": true,
  "reason": "권한 조정 중"
}
```

### ASSIGN 작업 예시
```json
{
  "role": "ADMIN",
  "permission": "user:create",
  "action": "assigned"
}
```

### REVOKE 작업 예시
```json
{
  "agent": "user-123",
  "role": "OLD_ROLE",
  "action": "revoked"
}
```

---

## 🔍 감사 로그 조회 예시

### 1. 특정 사용자의 모든 작업 조회
```sql
SELECT * 
FROM audit_logs
WHERE operator_id = 'admin-user'
  AND tenant_id = 'tenant-001'
ORDER BY timestamp DESC;
```

### 2. 특정 기간의 감사 로그 조회
```sql
SELECT * 
FROM audit_logs
WHERE timestamp BETWEEN '2026-01-01' AND '2026-01-15'
  AND tenant_id = 'tenant-001'
ORDER BY timestamp DESC;
```

### 3. 특정 역할의 변경 이력 조회
```sql
SELECT * 
FROM audit_logs
WHERE resource_type = 'ROLE'
  AND resource_id = '{role_id}'
  AND tenant_id = 'tenant-001'
ORDER BY timestamp DESC;
```

### 4. 특정 사용자에게 할당된 역할 이력 조회
```sql
SELECT * 
FROM audit_logs
WHERE action = 'ASSIGN'
  AND resource_type = 'AGENT_ROLE'
  AND resource_id = '{agent_id}'
  AND tenant_id = 'tenant-001'
ORDER BY timestamp DESC;
```

### 5. 액션 타입별 통계
```sql
SELECT 
    action,
    COUNT(*) as count,
    COUNT(DISTINCT operator_id) as operators,
    MIN(timestamp) as earliest,
    MAX(timestamp) as latest
FROM audit_logs
WHERE tenant_id = 'tenant-001'
GROUP BY action
ORDER BY count DESC;
```

### 6. 리소스 타입별 통계
```sql
SELECT 
    resource_type,
    COUNT(*) as count,
    COUNT(DISTINCT operator_id) as operators
FROM audit_logs
WHERE tenant_id = 'tenant-001'
GROUP BY resource_type
ORDER BY count DESC;
```

### 7. IP 주소별 작업 조회
```sql
SELECT 
    ip_address,
    COUNT(*) as action_count,
    COUNT(DISTINCT operator_id) as operator_count,
    COUNT(DISTINCT action) as action_types
FROM audit_logs
WHERE tenant_id = 'tenant-001'
GROUP BY ip_address
ORDER BY action_count DESC;
```

---

## 📤 아카이빙 전략

### 자동 아카이빙 (배치 작업)
```sql
-- 6개월 이상 된 로그를 아카이브로 이동
INSERT INTO audit_logs_archive 
SELECT * FROM audit_logs
WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '6 months';

DELETE FROM audit_logs
WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '6 months';
```

### 아카이브된 로그 조회
```sql
-- 아카이브된 로그에서 특정 기간 조회
SELECT * 
FROM audit_logs_archive
WHERE timestamp BETWEEN '2025-01-01' AND '2025-06-30'
  AND tenant_id = 'tenant-001'
ORDER BY timestamp DESC;
```

---

## 🔒 보안 고려사항

### 1. 감사 로그 위변조 방지
- 감사 로그는 **INSERT ONLY** 정책 준수
- UPDATE/DELETE는 매우 제한적으로 (아카이빙 목적만)
- 중요한 감사 로그는 별도 보안 저장소에 복제

### 2. 민감한 정보 처리
- 비밀번호/토큰 등은 changes에 저장하지 않기
- 변경 내용은 구조화된 JSON으로 기록
- IP 주소 마스킹 고려 (예: 192.168.1.XXX)

### 3. 접근 제어
- 감사 로그 조회 권한: audit:view
- 감사 로그 내보내기 권한: audit:export
- 감사 로그 삭제 권한: 매우 제한적 (감사자만)

### 4. 감시 및 알림
- 특정 액션에 대한 실시간 알림 설정
- 의심스러운 활동 자동 감지
- 정기적인 감사 로그 리뷰

---

## 📊 현재 삽입된 예시 데이터

### 활성 감사 로그 (audit_logs)
```
✅ 10개의 실제 같은 감사 로그

1. 권한 생성: user:create, org:read
2. 역할 생성: ADMIN, TEAM_LEADER
3. 역할-권한 할당: ADMIN에 user:create, TEAM_LEADER에 user:read
4. 사용자 생성: admin-user, team-leader-01
5. 사용자-역할 할당: 각 사용자에게 해당 역할
6. 부서 생성: 영업부
7. 역할 업데이트: MEMBER 비활성화/활성화
8. 권한 회수: user:delete 회수
9. 사용자-역할 할당: phone-agent-01에게 PHONE_AGENT
```

### 아카이브 감사 로그 (audit_logs_archive)
```
✅ 5개의 오래된 감사 로그 (6개월 이상 전)

1. 시스템 초기화
2. 초기 역할 생성
3. 초기 사용자 생성
4. 3개월 전 권한 변경
5. 2개월 전 사용자 역할 회수
```

---

## 🎯 팀원들을 위한 가이드

### 감사 로그 조회 권한 필요
- 권한 코드: `audit:view` (기본 조회)
- 권한 코드: `audit:export` (데이터 내보내기)

### 감사 로그 기록 자동화
```java
// 권한 변경 시 자동으로 감사 로그 기록
@Transactional
public void assignRoleToAgent(String agentId, String roleId) {
    // 역할 할당 로직
    agentRoles.add(new AgentRole(agentId, roleId));
    
    // 감사 로그 기록
    auditLog.record(
        action = "ASSIGN",
        resourceType = "AGENT_ROLE",
        resourceId = agentId,
        changes = {"agent": agentId, "role": roleId, "action": "assigned"}
    );
}
```

### 감시 및 모니터링
```sql
-- 비정상 활동 감시: 1시간 내 100회 이상 권한 변경
SELECT 
    operator_id,
    COUNT(*) as change_count,
    COUNT(DISTINCT resource_type) as resource_types
FROM audit_logs
WHERE timestamp > CURRENT_TIMESTAMP - INTERVAL '1 hour'
  AND action IN ('ASSIGN', 'REVOKE')
  AND tenant_id = 'tenant-001'
GROUP BY operator_id
HAVING COUNT(*) > 100;
```

---

## 📚 관련 문서

- **STANDARD_DATA_GUIDE.md**: 권한, 역할 표준
- **DB_STRUCTURE.md**: 데이터베이스 전체 구조
- **DOCUMENTATION_GUIDE.md**: 전체 문서 인덱스

---

**작성일**: 2026-01-15
**버전**: 1.0

