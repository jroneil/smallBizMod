package com.acmeco.reference.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces the mandatory password-change state at the security boundary. A user flagged for a
 * required change may only reach the identity, own-profile, and password-change endpoints until the
 * flag is cleared; all other protected APIs return 403.
 */
public class AcmePasswordChangeRequiredFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.getPrincipal() instanceof AcmeUserDetails userDetails
				&& userDetails.isPasswordChangeRequired()
				&& !isAllowed(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isAllowed(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return path.equals("/api/auth/session")
				|| path.equals("/api/me")
				|| path.equals("/api/me/password")
				|| path.equals("/error");
	}
}
