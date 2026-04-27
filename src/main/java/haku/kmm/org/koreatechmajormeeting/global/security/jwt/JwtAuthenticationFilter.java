package haku.kmm.org.koreatechmajormeeting.global.security.jwt;

import haku.kmm.org.koreatechmajormeeting.global.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieService jwtCookieService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> headerToken = extractHeaderToken(request);
        if (headerToken.isPresent() && jwtTokenProvider.validate(headerToken.get())) {
            setAuthentication(headerToken.get());
        } else {
            Optional<String> cookieToken = extractCookieToken(request);
            if (cookieToken.isPresent() && jwtTokenProvider.validate(cookieToken.get())) {
                setAuthentication(cookieToken.get());
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractHeaderToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return Optional.of(authorization.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    private Optional<String> extractCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (jwtCookieService.getCookieName().equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private void setAuthentication(String token) {
        AuthenticatedUser authenticatedUser = jwtTokenProvider.toAuthenticatedUser(token);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name()))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
