package com.ecommerce.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation dùng để đánh dấu các phương thức cần ghi nhật ký (Audit Log).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
    /**
     * Tên hành động (e.g., "APPROVE_SHOP", "UPDATE_PRODUCT")
     */
    String action() default "";
}
