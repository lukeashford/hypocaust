package com.example.web.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Web configuration for serving the React frontend. Handles SPA routing by serving index.html for
 * all non-API routes.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(new PathResourceResolver() {
          @Override
          protected Resource getResource(String resourcePath, Resource location)
              throws IOException {
            Resource requestedResource = location.createRelative(resourcePath);

            // If the requested resource exists, serve it
            if (requestedResource.exists() && requestedResource.isReadable()) {
              return requestedResource;
            }

            // If the path starts with /api, don't serve index.html
            if (resourcePath.startsWith("api/")) {
              return null;
            }

            // For all other paths (SPA routes), serve index.html
            return new ClassPathResource("/static/index.html");
          }
        });
  }
}