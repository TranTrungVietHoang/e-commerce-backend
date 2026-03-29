package com.ecommerce.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(String.format("%s không tìm thấy với id: '%s'", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }
}
