package com.nexfron.identitymodulith.rbac.application.dto;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 감사 로그 DTO
 *
 * 권한 변경 이력을 클라이언트에 반환할 때 사용됩니다.
 */
public record AuditLogDto(
        String auditId,
        String tenantId,
        String action,
        String resourceType,
        String resourceId,
        String operatorId,
        String changes,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp,
        String remarks,
        String ipAddress
) {
    /**
     * AuditLogJpaEntity를 AuditLogDto로 변환
     *
     * @param entity JPA 엔티티
     * @return DTO
     */
    public static AuditLogDto from(AuditLogJpaEntity entity) {
        return new AuditLogDto(
                entity.getAuditId(),
                entity.getTenantId(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getOperatorId(),
                entity.getChanges(),
                entity.getTimestamp(),
                entity.getRemarks(),
                entity.getIpAddress()
        );
    }

    /**
     * 작업 유형을 한국어로 변환
     *
     * @return 한국어 작업 유형
     */
    public String getActionDescription() {
        return switch (action) {
            case "CREATE" -> "생성";
            case "UPDATE" -> "수정";
            case "DELETE" -> "삭제";
            case "ASSIGN" -> "할당";
            case "REVOKE" -> "회수";
            default -> action;
        };
    }

    /**
     * 리소스 타입을 한국어로 변환
     *
     * @return 한국어 리소스 타입
     */
    public String getResourceTypeDescription() {
        return switch (resourceType) {
            case "ROLE" -> "역할";
            case "PERMISSION" -> "권한";
            case "ROLE_PERMISSION" -> "역할-권한";
            case "AGENT_ROLE" -> "사용자-역할";
            case "ROLE_PERMISSION_GROUP" -> "역할-권한그룹";
            case "PERMISSION_GROUP" -> "권한그룹";
            default -> resourceType;
        };
    }
}
