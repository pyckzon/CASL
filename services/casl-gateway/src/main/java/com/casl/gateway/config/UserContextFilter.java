package com.casl.gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Propaga el `sub` del JWT como header `X-User-Id` hacia los servicios
 * downstream (anti-corruption de identidad).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UserContextFilter extends OncePerRequestFilter {

	public static final String USER_ID_HEADER = "X-User-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String userId = null;
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			userId = jwt.getSubject();
		}
		if (userId != null) {
			final String uid = userId;
			HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
				@Override
				public String getHeader(String name) {
					if (USER_ID_HEADER.equalsIgnoreCase(name)) {
						return uid;
					}
					return super.getHeader(name);
				}

				@Override
				public Enumeration<String> getHeaders(String name) {
					if (USER_ID_HEADER.equalsIgnoreCase(name)) {
						return Collections.enumeration(List.of(uid));
					}
					return super.getHeaders(name);
				}

				@Override
				public Enumeration<String> getHeaderNames() {
					List<String> names = Collections.list(super.getHeaderNames());
					if (!names.contains(USER_ID_HEADER)) {
						names.add(USER_ID_HEADER);
					}
					return Collections.enumeration(names);
				}
			};
			filterChain.doFilter(wrapper, response);
		} else {
			filterChain.doFilter(request, response);
		}
	}
}
