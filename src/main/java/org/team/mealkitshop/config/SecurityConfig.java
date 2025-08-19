package org.team.mealkitshop.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;


import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    // 정적 리소스는 필터 자체를 타지 않게 무시
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationSuccessHandler authSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/thymeleaf/main",
                                "/login", "/signup", "/members/join",
                                "/css/**", "/js/**", "/img/**", "/images/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")      // ★ 폼 name과 일치
                        .passwordParameter("password")   // ★ 폼 name과 일치
                        .successHandler(authSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
// 선택: 로그인 폼에 name="remember-me" 있으므로 활성화하려면 아래 추가
                .rememberMe(rm -> rm
                                .rememberMeParameter("remember-me")
                                .tokenValiditySeconds(60 * 60 * 24 * 14) // 14일
                        // .userDetailsService(memberService)    // 필요 시 주입

                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(authSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/thymeleaf/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /*
     * 로그인 성공 시 우선순위:
     * 1) SavedRequest(보호페이지 접근 중) → 2) 세션 prevPage(Referer) → 3) 기본 /thymeleaf/main
     */
    @Bean
    public AuthenticationSuccessHandler authSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication)
                    throws ServletException, IOException {

                // 1) 보호 페이지 접근 중이었으면 그 URL로
                SavedRequest saved = new HttpSessionRequestCache().getRequest(request, response);
                if (saved != null) {
                    getRedirectStrategy().sendRedirect(request, response, saved.getRedirectUrl());
                    return;
                }

                // 2) 우리가 저장한 prevPage 사용 (예외 경로는 메인으로 대체)
                HttpSession session = request.getSession(false);
                String prev = (session != null) ? (String) session.getAttribute("prevPage") : null;

                if (prev == null
                        || prev.contains("/login")
                        || prev.contains("/logout")
                        || prev.contains("/signup")
                        || prev.contains("/members/join")
                        || prev.contains("/signup-success")
                        || prev.contains("/oauth2/authorization")) {
                    prev = "/thymeleaf/main"; // 프로젝트 메인 경로
                }
                getRedirectStrategy().sendRedirect(request, response, prev);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
