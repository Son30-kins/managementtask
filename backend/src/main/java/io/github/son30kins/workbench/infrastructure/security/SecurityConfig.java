package io.github.son30kins.workbench.infrastructure.security;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.github.son30kins.workbench.application.config.WorkbenchProperties;
import io.github.son30kins.workbench.domain.user.UserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;

/**
 * Cấu hình Spring Security.
 *
 * <pre>
 * Trình duyệt ──► Next.js (cổng 3000) ──rewrite /api/auth/**──► Spring (cổng 8080)
 *                     └── server gọi /api/** kèm cookie WORKBENCH_SESSION + X-Requested-By
 * </pre>
 *
 * Đăng nhập: GET /api/auth/signin/google → Google → GET /api/auth/callback/google → về trang chủ.
 * Phiên lưu trong Postgres (Spring Session JDBC), cookie HttpOnly + SameSite=Lax.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	static final String SIGNIN_BASE = "/api/auth/signin";

	static final String CALLBACK_BASE = "/api/auth/callback/*";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, WorkbenchOidcUserService oidcUserService,
			OAuth2AuthorizationRequestResolver authorizationRequestResolver, UserRepository users,
			WorkbenchProperties properties) throws Exception {
		var appUrl = properties.appBaseUrl();
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/actuator/health/**", "/error").permitAll()
			// Link lịch iCal: ứng dụng lịch tải về không có phiên đăng nhập — token bí mật trong URL thay cho phiên
			.requestMatchers(HttpMethod.GET, "/api/calendar/feed/*").permitAll()
			.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
			.requestMatchers("/api/admin/**").hasRole("ADMIN")
			.requestMatchers("/api/**").authenticated()
			.anyRequest().denyAll())
			.oauth2Login(oauth -> oauth
				.authorizationEndpoint(a -> a.baseUri(SIGNIN_BASE).authorizationRequestResolver(authorizationRequestResolver))
				.redirectionEndpoint(r -> r.baseUri(CALLBACK_BASE))
				.userInfoEndpoint(u -> u.oidcUserService(oidcUserService))
				.successHandler((request, response, authentication) -> response.sendRedirect(appUrl + "/"))
				.failureHandler((request, response, exception) -> {
					var code = exception instanceof OAuth2AuthenticationException oauthError
							? oauthError.getError().getErrorCode() : "OAUTH_FAILED";
					response.sendRedirect(appUrl + "/login?error=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
				}))
			.logout(logout -> logout.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			// API trả 401 thay vì chuyển hướng sang trang đăng nhập Google
			.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			// CSRF: dùng header X-Requested-By (RequestedByFilter) + cookie SameSite=Lax thay cho token
			.csrf(csrf -> csrf.disable())
			.addFilterBefore(new RequestedByFilter(), LogoutFilter.class)
			.addFilterBefore(new CurrentUserFilter(users), AuthorizationFilter.class);
		return http.build();
	}

	/** Luôn cho chọn tài khoản Google (không tự đăng nhập bằng tài khoản đang mở). */
	@Bean
	OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository registrations) {
		var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, SIGNIN_BASE);
		resolver.setAuthorizationRequestCustomizer(c -> c.additionalParameters(p -> p.put("prompt", "select_account")));
		return resolver;
	}

}
