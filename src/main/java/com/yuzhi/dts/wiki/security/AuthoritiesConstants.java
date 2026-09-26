package com.yuzhi.dts.wiki.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    // DTS-WIKI: customized (Sprint-6 design 03 S3): write access is a separate
    // authority mapped from the Keycloak `editor` client role.
    public static final String EDITOR = "ROLE_EDITOR";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    private AuthoritiesConstants() {}
}
