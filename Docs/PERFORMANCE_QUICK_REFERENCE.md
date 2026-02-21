# 성능 최적화 Quick Reference

> 개발 시 빠르게 참고할 수 있는 성능 최적화 체크리스트

## 🚀 성능 최적화 Top 5

### 1위: N+1 쿼리 해결 ⭐⭐⭐
**효과**: 쿼리 85~96% ↓, 응답 시간 82% ↓

```java
// ❌ 나쁜 예
list.stream()
    .map(item -> repository.findById(item.getId()))  // N+1 발생!
    
// ✅ 좋은 예  
repository.findByIdsWithJoin(ids);  // 1개 쿼리
```

---

### 2위: Materialized Path (트리 조회) ⭐⭐⭐
**효과**: 재귀 쿼리 제거, 단일 쿼리화

```sql
-- ✅ 단일 쿼리로 모든 하위 노드 조회
SELECT * FROM org_departments 
WHERE org_path LIKE '/parent-id%';
```

---

### 3위: 복합 인덱스 ⭐⭐⭐
**효과**: 조회 성능 10~100배 ↑

```sql
-- ✅ 멀티테넌시 복합 인덱스
CREATE UNIQUE INDEX uk_tenant_login 
ON user_agents(tenant_id, login_id);
```

---

### 4위: 낙관적 잠금 ⭐⭐⭐
**효과**: 락 대기 제거, 처리량 10배 ↑

```java
@Version
@Column(name = "version")
private Long version;
```

---

### 5위: Batch API ⭐⭐
**효과**: API 호출 90% ↓, 응답 시간 84% ↓

```java
// ✅ 일괄 처리 API
batchAssignPermissionsToRole(roleName, permissionCodes);
```

---

## 📋 개발 체크리스트

### 쿼리 작성 시
- [ ] JOIN으로 한 번에 조회할 수 있는지 확인
- [ ] 필요한 컬럼만 SELECT (DTO 프로젝션)
- [ ] 존재 여부만 확인할 때는 `EXISTS` 사용
- [ ] Bulk 처리 가능한지 확인 (일괄 INSERT/DELETE)

### 트랜잭션 설정 시
- [ ] 조회 메소드는 `@Transactional(readOnly = true)`
- [ ] 트랜잭션 범위 최소화
- [ ] 불필요한 Lazy Loading 방지

### 인덱스 설계 시
- [ ] WHERE 절의 컬럼에 인덱스 존재 확인
- [ ] 복합 인덱스 순서 최적화 (카디널리티 높은 것 우선)
- [ ] Partial Index 고려 (조건부 인덱싱)

### 동시성 제어 시
- [ ] 낙관적 잠금 우선 고려 (`@Version`)
- [ ] 비관적 잠금은 필수 시에만 사용
- [ ] 데드락 가능성 검토

---

## 🎯 성능 측정 기준

### 응답 시간
- **우수**: 50ms 이하
- **양호**: 100ms 이하
- **개선 필요**: 200ms 이상

### 쿼리 수
- **우수**: 3개 이하
- **양호**: 5개 이하
- **개선 필요**: 10개 이상

### DB 커넥션
- **안전**: 70% 이하
- **주의**: 80% 이상
- **위험**: 90% 이상

---

## 🔗 상세 문서

- [PERFORMANCE_ANALYSIS.md](./PERFORMANCE_ANALYSIS.md) - 15가지 최적화 기법 종합 분석
- [PERFORMANCE_OPTIMIZATION_N_PLUS_1.md](./PERFORMANCE_OPTIMIZATION_N_PLUS_1.md) - N+1 쿼리 상세 가이드

---

**최종 업데이트**: 2026-02-22

