package com.identitymodulith.organization.infrastructure.persistence.repository;

import com.identitymodulith.organization.domain.model.DepartmentStatus;
import com.identitymodulith.organization.domain.model.DepartmentType;

/**
 * 목록성 조회 전용 경량 프로젝션.
 * DepartmentResponse 구성에 필요한 필드만 조회한다.
 */
public interface DepartmentListProjection {

    String getDeptId();

    String getName();

    DepartmentType getType();

    String getOrgPath();

    Integer getDepth();

    String getParentId();

    DepartmentStatus getStatus();
}

