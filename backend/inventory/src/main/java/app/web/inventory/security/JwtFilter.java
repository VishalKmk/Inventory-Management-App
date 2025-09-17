package app.web.inventory.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@SuppressWarnings("null") HttpServletRequest request,
            @SuppressWarnings("null") HttpServletResponse response, @SuppressWarnings("null") FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            chain.doFilter(request, response); // skip JWT for auth routes
            return;
        }

        // JWT validation
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Jws<Claims> claims = jwtUtil.parseToken(token);
                String userId = claims.getBody().getSubject();
                String email = (String) claims.getBody().get("email");

                Map<String, Object> principal = Map.of(
                        "userId", UUID.fromString(userId),
                        "email", email);

                // Set authenticated user in context
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, // principal is now a Map containing userId and email
                        null,
                        Collections.emptyList() // no authorities for now
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception ex) {
                // invalid token → return unauthorized
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}