package com.nexfron.identitymodulith.organization.infrastructure.repository;

import com.nexfron.identitymodulith.organization.domain.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * DepartmentRepository
 *
 * - Department 엔티티의 영속화 담당
 * - orgPath 를 이용한 트리 탐색 메서드를 제공
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * 특정 테넌트에서, 특정 orgPath prefix 로 시작하는 모든 하위 부서 조회
     *
     * ex) orgPathPrefix = "/1/5"
     *     -> "/1/5", "/1/5/10", "/1/5/11" ...
     */
    List<Department> findByTenantIdAndOrgPathStartsWith(String tenantId, String orgPathPrefix);

    /**
     * 특정 테넌트의 전체 부서 목록
     * - 트리 조회 시 전체를 메모리로 가져온 뒤, 계층 구조를 조립할 때 사용
     */
    List<Department> findAllByTenantId(String tenantId);

    /**
     * 특정 부서를 parent 로 가지는 하위 부서가 존재하는지 여부
     * - 삭제 시 "하위 부서가 있으면 삭제 불가" 조건에 사용
     */
    boolean existsByParent(Department parent);

    /**
     * 최상위(root) 부서들만 조회하고 싶을 때 사용 가능
     * - depth = 0 조건 대신 parent IS NULL 로 표현
     */
    List<Department> findByTenantIdAndParentIsNull(String tenantId);
}
