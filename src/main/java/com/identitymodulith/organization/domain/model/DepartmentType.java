package com.identitymodulith.organization.domain.model;

/**
 * 부서 타입 Enum
 *
 * <h3>개요:</h3>
 * 조직의 계층적 구조에서 부서의 역할과 위치를 나타냅니다.
 * EVENT_STORMING.md 명세에 따라 5가지 표준 타입을 정의합니다.
 *
 * <h3>타입 설명:</h3>
 * <ul>
 *   <li>COMPANY: 회사 (최상위 루트 전용)</li>
 *   <li>DIVISION: 본부/사업부 (대규모 조직 단위)</li>
 *   <li>TEAM: 팀 (실무 조직 단위)</li>
 *   <li>GROUP: 그룹/파트 (소규모 조직 단위)</li>
 *   <li>CUSTOM: 커스텀 (사용자 정의 타입, customTypeName 필수)</li>
 * </ul>
 *
 * <h3>예시 구조:</h3>
 * <pre>
 * ABC금융 (COMPANY) - 루트
 *   ├── 기술본부 (DIVISION)
 *   │   └── 개발팀 (TEAM)
 *   │       └── 백엔드파트 (GROUP)
 *   ├── 서울센터 (CUSTOM: "센터")
 *   │   └── 인바운드팀 (TEAM)
 *   └── 부산센터 (CUSTOM: "센터")
 *       └── 아웃바운드팀 (TEAM)
 * </pre>
 *
 * <h3>CUSTOM 타입 사용 규칙:</h3>
 * - customTypeName 필수 입력
 * - 표준 타입으로 분류되지 않는 조직 단위에 사용
 * - 예: "센터", "지점", "사무소" 등
 *
 * <h3>타입별 권장 깊이:</h3>
 * - COMPANY: depth=0 (루트만)
 * - DIVISION: depth=1~2
 * - TEAM: depth=2~3
 * - GROUP: depth=3~4
 * - CUSTOM: 제한 없음
 *
 * @see DepartmentEntity
 * @see DepartmentStatus
 * @author Identity System Team
 * @version 1.0
 * @since EVENT_STORMING.md 명세 기준
 */
public enum DepartmentType {

    /**
     * 회사 (Company)
     * - 조직의 최상위 레벨
     * - 루트 부서에만 사용 권장
     * - 예: "ABC금융", "XYZ기술"
     */
    COMPANY("회사"),

    /**
     * 본부/사업부 (Division)
     * - 회사의 주요 사업 단위
     * - 대규모 조직 구분
     * - 예: "기술본부", "영업본부", "마케팅본부"
     */
    DIVISION("본부"),

    /**
     * 팀 (Team)
     * - 실무를 수행하는 조직 단위
     * - 가장 일반적인 부서 타입
     * - 예: "개발팀", "인사팀", "고객지원팀"
     */
    TEAM("팀"),

    /**
     * 그룹/파트 (Group)
     * - 팀 내 소규모 조직 단위
     * - 세분화된 업무 분담
     * - 예: "백엔드파트", "프론트엔드파트", "채용그룹"
     */
    GROUP("그룹"),

    /**
     * 커스텀 (Custom)
     * - 사용자 정의 타입
     * - customTypeName 필수 입력
     * - 표준 타입으로 분류되지 않는 조직 단위
     * - 예: "센터", "지점", "사무소", "지사"
     */
    CUSTOM("커스텀");

    private final String displayName;

    DepartmentType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 표시용 이름 반환
     *
     * @return 한글 표시명
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * CUSTOM 타입 여부 확인
     *
     * @return CUSTOM 타입이면 true
     */
    public boolean isCustomType() {
        return this == CUSTOM;
    }

    /**
     * 루트 부서로 사용 가능한 타입인지 확인
     *
     * @return COMPANY 타입이면 true (루트 권장)
     */
    public boolean isRootType() {
        return this == COMPANY;
    }
}
