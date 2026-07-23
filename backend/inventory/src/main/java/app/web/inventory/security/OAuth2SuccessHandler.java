package app.web.inventory.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import app.web.inventory.model.Users;
import app.web.inventory.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final String frontendRedirectUri;

    public OAuth2SuccessHandler(
            UserService userService,
            JwtUtil jwtUtil,
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null) {
            response.sendRedirect(frontendRedirectUri + "?error=no_email_from_google");
            return;
        }

        Users user = userService.findOrCreateGoogleUser(email, name != null ? name : email);

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail());

        response.sendRedirect(frontendRedirectUri + "#token=" + token);
    }
}