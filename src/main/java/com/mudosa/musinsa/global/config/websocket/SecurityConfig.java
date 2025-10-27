//package com.mudosa.musinsa.global.config.websocket;// SecurityConfig.java
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration("webSocketSecurityConfig") // 👈 명시적 이름
//@EnableWebSecurity
//public class SecurityConfig {
//
//  @Bean
//  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    http
//        .csrf(csrf -> csrf.disable())
//        .authorizeHttpRequests(auth -> auth
//            .anyRequest().permitAll()
//        );
//
//    return http.build();
//  }
//}
