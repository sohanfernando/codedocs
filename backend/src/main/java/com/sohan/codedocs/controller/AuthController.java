package com.sohan.codedocs.controller;

import com.sohan.codedocs.dto.request.LoginRequest;
import com.sohan.codedocs.dto.request.RegisterRequest;
import com.sohan.codedocs.dto.response.UserResponse;
import com.sohan.codedocs.entity.User;
import com.sohan.codedocs.security.AppUserDetails;
import com.sohan.codedocs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = userService.register(request.email(), request.password());
        authenticate(request.email(), request.password(), httpRequest, httpResponse);
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication result = authenticate(request.email(), request.password(), httpRequest, httpResponse);
        return UserResponse.from(userService.findByIdOrThrow(((AppUserDetails) result.getPrincipal()).id()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return UserResponse.from(userService.findByIdOrThrow(principal.id()));
    }

    /**
     * Authenticates against the DaoAuthenticationProvider Spring Security
     * builds from the userDetailsService + passwordEncoder beans, then
     * writes the resulting SecurityContext into the HTTP session by hand —
     * there's no servlet-filter form login here to do it automatically,
     * since these are plain JSON endpoints.
     */
    private Authentication authenticate(String email, String password,
                                        HttpServletRequest request, HttpServletResponse response) {
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return result;
    }
}
