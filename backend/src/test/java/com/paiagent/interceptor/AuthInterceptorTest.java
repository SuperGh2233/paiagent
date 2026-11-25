package com.paiagent.interceptor;

import com.paiagent.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthInterceptorTest {

    @Test
    void rejectsTokensSuppliedInQueryParameters() throws Exception {
        AuthService authService = mock(AuthService.class);
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "authService", authService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setParameter("token", "query-string-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        verifyNoInteractions(authService);
    }
}
