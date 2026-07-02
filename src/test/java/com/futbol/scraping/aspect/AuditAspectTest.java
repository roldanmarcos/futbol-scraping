package com.futbol.scraping.aspect;

import com.futbol.scraping.annotation.FutbolUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@FutbolUnit
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private final AuditAspect aspect = new AuditAspect();

    @Test
    void audit_ShouldProceedAndReturnResult() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestService.method()");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", 42});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
    }

    @Test
    void audit_WithNullAuthentication_ShouldResolveAnonymous() throws Throwable {
        SecurityContextHolder.clearContext();
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestService.method()");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void audit_WithAnonymousUser_ShouldResolveAnonymous() throws Throwable {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("anonymousUser");
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestService.method()");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("result");
        SecurityContextHolder.clearContext();
    }

    @Test
    void audit_WithAuthenticatedUser() throws Throwable {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestService.method()");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("result");
        SecurityContextHolder.clearContext();
    }

    @Test
    void audit_WithEmptyArgs() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestService.method()");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("result");
    }
}
