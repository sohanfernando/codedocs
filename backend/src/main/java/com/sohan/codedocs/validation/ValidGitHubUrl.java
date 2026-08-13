package com.sohan.codedocs.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = GitHubUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGitHubUrl {
    String message() default "must be a public GitHub HTTPS repository URL";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
