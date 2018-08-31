package com.woaiqw.bmob;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.woaiqw.bmob.thread.ThreadMode.MAIN;

/**
 * Created by haoran on 2018/8/31.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Subscribe {

    Tag[] tags() default {};

    int thread() default MAIN;

}
