package com.example.demo.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 1. 获取本次请求的 HTTP 动词和具体路径
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 2. 手写细粒度放行规则
        // 规则 A：POST /api/users → 允许用户注册（放行）
        boolean isCreateUser = "POST".equalsIgnoreCase(method) && "/api/users".equals(uri);
        // 规则 B：GET /api/users/{id} → 允许查询用户信息（放行）
        boolean isGetUser = "GET".equalsIgnoreCase(method) && uri.startsWith("/api/users/");

        // 满足公开操作规则 → 直接放行，无需 Token
        if (isCreateUser || isGetUser) {
            return true;
        }

        // 3. 敏感操作（DELETE/PUT 等）必须校验 Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            // 提示敏感操作需要凭证
            String errorJson = "{\"code\": 401, \"msg\": \"非法操作：敏感动作 [" + method + "] 需携带有效凭证\"}";
            response.getWriter().write(errorJson);
            return false;
        }

        // Token 存在 → 放行
        return true;
    }
}