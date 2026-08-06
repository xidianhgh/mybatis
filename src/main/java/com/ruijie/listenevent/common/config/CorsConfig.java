package com.ruijie.listenevent.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 匹配所有接口
                .allowedOrigins("*") // 允许来源，生产不要写*，写前端域名如"http://127.0.0.1:5173"
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许请求方法
                .allowedHeaders("*")
                .maxAge(3600); // 预检OPTIONS请求缓存时间，单位秒
    }
}
