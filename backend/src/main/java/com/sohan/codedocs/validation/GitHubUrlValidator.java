package com.sohan.codedocs.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;

public class GitHubUrlValidator implements ConstraintValidator<ValidGitHubUrl, String> {

    private static final Set<String> ALLOWED_HOSTS = Set.of("github.com", "www.github.com");
    private static final Pattern PATH_PATTERN =
            Pattern.compile("^/[A-Za-z0-9_.-]{1,39}/[A-Za-z0-9_.-]{1,100}(\\.git)?/?$");
    private static final int MAX_LENGTH = 512;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context){
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());

            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            if (uri.getHost() == null) return false;
            if (!ALLOWED_HOSTS.contains(uri.getHost().toLowerCase())) return false;
            if (uri.getPort() != -1) return false;          // no https://github.com:22/
            if (uri.getUserInfo() != null) return false;    // no creds in URL
            if (uri.getQuery() != null || uri.getFragment() != null) return false;
            if (uri.getPath() == null) return false;
            if (uri.getPath().contains("..")) return false;

            return PATH_PATTERN.matcher(uri.getPath()).matches();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
