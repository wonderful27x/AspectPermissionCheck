package com.example.wypermission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限被拒绝并勾选了不在提醒的接口，当权被拒绝后需要通知，会反射调用被它注解的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionCancel {
    int requestCode() default -1;
}
