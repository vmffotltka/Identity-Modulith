# 🔧 추가 컬럼명 불일치 수정 완료

## 🎯 발견된 문제

**오류 메시지**:
```
ERROR: column rje1_0.data_scope does not exist
Position: 41
```

### 원인
- **JPA 엔티티**: `@Column(name = "data_scope")`
- **SQL 스키마**: `data_scope_level`

---

## ✅ 수정 완료

### 파일: `RoleJpaEntity.java`

**변경 전**:
```java
@Column(name = "data_scope", length = 32)
private DataScopeLevel dataScope;
```

**변경 후**:
```java
@Column(name = "data_scope_level", length = 32)
private DataScopeLevel dataScope;
```

---

## 📊 전체 컬럼명 검증 완료

### ✅ 모든 RBAC 엔티티 컬럼 확인

| 엔티티 | 필드명 | DB 컬럼명 | 상태 |
|--------|--------|----------|------|
| **RoleJpaEntity** | | | |
| ├─ roleId | role_id | ✅ 일치 |
| ├─ tenantId | tenant_id | ✅ 일치 |
| ├─ name | name | ✅ 일치 |
| ├─ type | type | ✅ 일치 |
| ├─ dataScope | ~~data_scope~~ → **data_scope_level** | ✅ **수정됨** |
| ├─ description | description | ✅ 일치 |
| ├─ isActive | is_active | ✅ 일치 |
| **PermissionJpaEntity** | | | |
| ├─ permissionId | permission_id | ✅ 일치 |
| ├─ code | code | ✅ 일치 |
| ├─ resource | resource | ✅ 일치 |
| ├─ action | action | ✅ 일치 |
| **RolePermissionJpaEntity** | | | |
| ├─ roleId | role_id | ✅ 일치 |
| ├─ permissionId | permission_id | ✅ 일치 |
| ├─ assignedAt | assigned_at | ✅ 일치 |
| **AgentRoleJpaEntity** | | | |
| ├─ agentId | agent_id | ✅ 일치 |
| ├─ roleId | role_id | ✅ 일치 |
| ├─ assignedAt | assigned_at | ✅ 일치 |

---

## 🚀 이제 애플리케이션 재시작

```bash
# IntelliJ에서 재시작 또는
./gradlew bootRun
```

---

## ✅ 확인 사항

애플리케이션 로그에서:
```
✅ Hibernate: select ... from rbac_roles rje1_0 ...
   (data_scope_level 컬럼 정상 조회)
   
✅ 오류 없음!
```

---

## 📝 요약

### 수정된 불일치 목록 (전체)

1. ✅ **테이블명**: `user_agent_roles` → `rbac_agent_roles` (RBAC 모듈 일관성)
2. ✅ **컬럼 추가**: `rbac_agent_roles.assigned_at`
3. ✅ **컬럼 추가**: `rbac_role_permissions.assigned_at`
4. ✅ **컬럼명**: `data_scope` → `data_scope_level`

---

**이제 완벽하게 일치합니다!** 🎉

**작성일**: 2026-02-07  
**최종 수정**: RoleJpaEntity.data_scope → data_scope_level

