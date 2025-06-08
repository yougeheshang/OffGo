package org.tinkerhub.offgo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源映射
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
                
        // 配置上传文件映射
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
} 