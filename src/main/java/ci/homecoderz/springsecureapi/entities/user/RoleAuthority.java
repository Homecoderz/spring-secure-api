package ci.homecoderz.springsecureapi.entities.user;

public final class RoleAuthority {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private RoleAuthority() {
    }

    public static String toSpringAuthority(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_USER;
        }
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
