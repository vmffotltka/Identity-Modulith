package com.identitymodulith.rbac.presentation.dto.response;

import java.util.List;

public record BatchAssignmentResponse(
        int successCount,
        int failedCount,
        int skippedCount,
        List<String> errors
) {}

