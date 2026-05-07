package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults()) // 开启全局CORS配置
                                .csrf(AbstractHttpConfigurer::disable) // 关闭 CSRF 防护，前后端分离项目通常关闭
                                .sessionManagement(session -> // 配置 Session 管理策略，设置无状态
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth // 重点：配置接口访问规则
                                                .requestMatchers(HttpMethod.POST, "/api/users").permitAll() // 放行注册接口
                                                .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll() // 放行登录接口
                                                .requestMatchers("/api/chat").permitAll() // 放行聊天接口
                                                .anyRequest().authenticated() // 其他所有请求都必须先认证
                                )
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // 添加JWT过滤器
                                .formLogin(AbstractHttpConfigurer::disable) // 关闭 Spring Security 默认表单登录
                                .httpBasic(AbstractHttpConfigurer::disable); // 关闭 httpBasic 默认认证

                return http.build();
        }
}
