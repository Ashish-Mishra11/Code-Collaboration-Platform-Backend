package com.codesync.project.component;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getCredentials() != null) {

            String token = (String) authentication.getCredentials();

            // Send JWT to auth-service
            template.header("Authorization", token);
            System.out.println("Feign Token: " + token);
        }
        System.out.println("not get token in feignInterceptio");
    }
}