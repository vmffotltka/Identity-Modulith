# 📋 프로젝트 변경 이력 (Changelog)

## 🎯 v2.0.0 - 2026-01-21

### ✨ 주요 개선 사항

#### 1️⃣ **데이터베이스 표준화**
- ✅ **모든 PK를 UUID로 통일** (VARCHAR(36))
- ✅ **명명 규칙 통일** (snake_case, 소문자)
- ✅ **핵심 테이블 8개 유지**
  ```
  1. departmentEntities         (조직 관리)
  2. agents             (사용자 관리)
  3. roles              (역할 관리)
  4. permissions        (권한 관리)
  5. role_permissions   (역할-권한 매핑)
  6. agent_roles        (사용자-역할 매핑)
  7. audit_logs         (감사 로그)
  8. audit_logs_archive (감사 로그 아카이브)
  ```
- ✅ **표준 데이터 자동 삽입**
  - 35개 권한 (RBAC 관리, 조직 관리, 보고서, 설정)
  - 8개 역할 (ADMIN, TEAM_LEADER, MEMBER 등)
  - 16개 사용자 (테스트용)
  - 16개 부서 (3단계 조직 구조)

#### 2️⃣ **RBAC 레벨 수정**
- ❌ **계층형 RBAC (Level 2)** 제거
- ✅ **Flat RBAC (Level 1)** 적용
  - 이유: 상담사 환경에서는 채널별/스킬별 권한이 계층보다 중요
  - 예: Inbound, Outbound, Chat, Phone 채널별 권한

#### 3️⃣ **코드 품질 개선**
- ✅ **주석 대폭 강화** - 모든 Entity, Service, Controller 상세 주석 추가
- ✅ **한글 인코딩 정상화** - 테스트 파일 한글 깨짐 해결
- ✅ **불필요한 코드 제거** - Deprecated 메서드, 미사용 클래스 정리
- ✅ **테스트 코드 정비** - 93개 테스트 모두 통과

#### 4️⃣ **공통 모듈(Common) 추가**
- ✅ **TenantContextHolder** - 멀티테넌시 컨텍스트 관리
- ✅ **AuthPrincipal** - 인증 사용자 정보
- ✅ **UnauthorizedException** - 권한 예외 처리
- ✅ **CacheKeyGenerator** - 캐시 키 생성 유틸

#### 5️⃣ **캐싱 전략 적용**
- ✅ **Spring Cache 통합**
  - `userPermissions` - 사용자별 권한 캐시
  - `rolePermissions` - 역할별 권한 캐시
  - `accessibleDepts` - 접근 가능 부서 캐시
- ✅ **자동 캐시 무효화** (@CacheEvict)

#### 6️⃣ **감사 로그(Audit Log) 구현**
- ✅ **모든 권한 변경 추적**
  - 역할 생성/수정/삭제
  - 권한 생성/삭제
  - 역할-권한 할당/회수
  - 사용자-역할 할당/회수
- ✅ **자동 아카이빙 배치** - 6개월 이상 로그 자동 이동

---

## 📁 주요 파일 변경

### 추가된 파일
```
✅ common/security/TenantContextHolder.java               - 멀티테넌시 핵심
✅ common/security/AuthPrincipal.java                     - 인증 정보
✅ common/security/UnauthorizedException.java             - 권한 예외
✅ common/cache/CacheKeyGenerator.java                    - 캐시 키 유틸
✅ rbac/application/AuditLogService.java                  - 감사 로그 서비스
✅ rbac/infrastructure/batch/AuditLogArchivingBatch.java  - 로그 아카이빙
✅ DB_COMPREHENSIVE_GUIDE.md                              - DB 설계 가이드
✅ CHANGELOG.md                                           - 이 파일
```

### 수정된 파일
```
🔧 README.md                 - Swagger 인증 정보 업데이트
🔧 application.yml           - Flyway 검증 비활성화
🔧 .gitignore                - 테스트 출력 파일 추가
🔧 모든 Entity 파일           - 상세 주석 추가
🔧 모든 Service 파일          - 멀티테넌시 적용
```

---

## 📖 추가 문서

- **README.md** - 프로젝트 개요 및 실행 방법
- **DB_COMPREHENSIVE_GUIDE.md** - DB 구조 및 컬럼 설명

