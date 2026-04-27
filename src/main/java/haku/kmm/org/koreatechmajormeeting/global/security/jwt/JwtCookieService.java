package haku.kmm.org.koreatechmajormeeting.global.security.jwt;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Getter
@Component
public class JwtCookieService {

    private final String cookieName;
    private final boolean cookieSecure;
    private final long accessTokenValiditySeconds;

    public JwtCookieService(
        @Value("${security.jwt.cookie-name:ACCESS_TOKEN}") String cookieName,
        @Value("${security.jwt.cookie-secure:false}") boolean cookieSecure,
        @Value("${security.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds
    ) {
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .sameSite("Lax")
            .maxAge(Duration.ofSeconds(accessTokenValiditySeconds))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .sameSite("Lax")
            .maxAge(Duration.ZERO)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
