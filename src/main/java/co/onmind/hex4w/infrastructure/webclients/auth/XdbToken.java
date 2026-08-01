package co.onmind.hex4w.infrastructure.webclients.auth;

/**
 * ABC (Authorization Bearer Credentials) for XDB authentication.
 * <p>
 * Use Case (UC): Provides optional bearer/Basic token injection for WebClient requests.
 * <p>
 * XDB supports: NoAuth (default), Basic (user:pass), Bearer (JWT for OIDC/Cognito).
 *
 * @param type  authorization type: {@code none}, {@code basic}, {@code bearer}
 * @param token the credential value (empty/ignored when type is {@code none})
 */
public record XdbToken(String type, String token) {

    public static final String BEARER = "bearer";
    public static final String BASIC = "basic";
    public static final String NONE = "none";

    public XdbToken {
        if (type == null || type.isBlank()) type = NONE;
        if (token == null) token = "";
    }

    /** Creates a NoAuth token (no header added). */
    public static XdbToken none() {
        return new XdbToken(NONE, "");
    }

    /** Creates a Bearer token for JWT-based auth. */
    public static XdbToken bearer(String jwt) {
        return new XdbToken(BEARER, jwt != null ? jwt : "");
    }

    /** Creates a Basic token for username:password auth. */
    public static XdbToken basic(String user, String pass) {
        String encoded = java.util.Base64.getEncoder()
            .encodeToString((user + ":" + pass).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new XdbToken(BASIC, encoded);
    }

    /** Returns the {@code Authorization} header value, or empty if type is {@code none}. */
    public String toHeaderValue() {
        return switch (type) {
            case BEARER -> "Bearer " + token;
            case BASIC -> "Basic " + token;
            default -> "";
        };
    }
}