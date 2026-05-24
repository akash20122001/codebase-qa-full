package com.codebaseqa.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        try {
            // Try multiple locations for .env file
            Dotenv dotenv = null;
            String[] locations = {"./", "./backend/", "../"};
            
            for (String location : locations) {
                try {
                    dotenv = Dotenv.configure()
                        .directory(location)
                        .ignoreIfMissing()
                        .load();
                    
                    if (dotenv.entries().iterator().hasNext()) {
                        log.info("✅ Found .env file in: {}", location);
                        break;
                    }
                } catch (Exception e) {
                    // Try next location
                }
            }
            
            if (dotenv == null || !dotenv.entries().iterator().hasNext()) {
                log.warn("⚠️  No .env file found in any location. Using system environment variables.");
                return;
            }
            
            Map<String, Object> dotenvMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvMap.put(entry.getKey(), entry.getValue());
            });
            
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", dotenvMap));
            log.info("✅ Loaded {} environment variables from .env file", dotenvMap.size());
            
            // Debug: Check if AWS credentials are loaded
            if (dotenvMap.containsKey("AWS_ACCESS_KEY_ID")) {
                String accessKey = (String) dotenvMap.get("AWS_ACCESS_KEY_ID");
                log.info("✅ AWS_ACCESS_KEY_ID found: {}****", accessKey.substring(0, Math.min(4, accessKey.length())));
            } else {
                log.warn("⚠️  AWS_ACCESS_KEY_ID NOT found in .env");
            }
            
            if (dotenvMap.containsKey("AWS_SECRET_ACCESS_KEY")) {
                log.info("✅ AWS_SECRET_ACCESS_KEY found");
            } else {
                log.warn("⚠️  AWS_SECRET_ACCESS_KEY NOT found in .env");
            }
            
        } catch (Exception e) {
            log.error("❌ Could not load .env file: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
