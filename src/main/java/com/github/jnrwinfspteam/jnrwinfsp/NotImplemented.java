package com.github.jnrwinfspteam.jnrwinfsp;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * If a method is marked with this annotation then it won't be registered in FSP_FILE_SYSTEM_INTERFACE.
 * All methods in {@link com.github.jnrwinfspteam.jnrwinfsp.WinFspStubFS} are marked with this annotation.
 * <p>
 * This annotation is not inheritable, so when extending the WinFspStubFS class, all overridden methods will be
 * registered in FSP_FILE_SYSTEM_INTERFACE.
 * <p>
 * The goal of this annotation is twofold:
 * <ol>
 *     <li>Certain calls behave differently if other calls are defined or not (e.g. if both CanDelete and SetDelete are
 *     defined, SetDelete takes precedence)</li>
 *     <li>For performance reasons; if a method is not registered in FSP_FILE_SYSTEM_INTERFACE then native -> java call
 *     won't be performed.</li>
 * </ol>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = METHOD)
public @interface NotImplemented {
}
