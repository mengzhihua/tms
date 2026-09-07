package com.tms.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.basic.entity.Customer;
import com.tms.basic.mapper.CustomerMapper;
import com.tms.common.R;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {
    private final CustomerMapper customerMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = request.getHeader("X-Api-Key");
        Customer customer =
                customerMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Customer>()
                                .eq("api_key", key)
                                .isNotNull("api_key"));
        if (customer == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream()
                    .write(objectMapper.writeValueAsString(R.fail(401, "未授权")).getBytes(StandardCharsets.UTF_8));
            return false;
        }
        request.setAttribute("openCustomer", customer);
        return true;
    }
}
