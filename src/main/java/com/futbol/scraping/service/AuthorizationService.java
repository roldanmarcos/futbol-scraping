package com.futbol.scraping.service;

import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepository userRepository;

    public void assertUserMatchesOrSuperuser(Long targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException("User id is required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        String username = authentication.getName();
        var authenticatedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        boolean isSuperuser = Boolean.TRUE.equals(authenticatedUser.getIsSuperuser());
        boolean sameUser = targetUserId.equals(authenticatedUser.getId());

        if (!isSuperuser && !sameUser) {
            throw new AccessDeniedException("You can only access your own resources");
        }
    }
}
