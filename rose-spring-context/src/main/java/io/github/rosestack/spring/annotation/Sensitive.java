package io.github.rosestack.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.rosestack.core.util.SensitiveType;
import io.github.rosestack.core.util.SensitiveUtils;
import io.github.rosestack.spring.desensitization.FieldSensitiveSerializer;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since 0.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@JsonSerialize(using = FieldSensitiveSerializer.class)
public @interface Sensitive {

    SensitiveType type() default SensitiveType.CUSTOM;

    /**
     * @return 前置不需要打码的长度
     */
    int prefixKeep() default 0;

    /**
     * @return 后置不需要打码的长度
     */
    int suffixKeep() default 0;

    /**
     * 用什么打码
     *
     * @return String
     */
    char mask() default SensitiveUtils.MASK;

    String expression() default "";
}
