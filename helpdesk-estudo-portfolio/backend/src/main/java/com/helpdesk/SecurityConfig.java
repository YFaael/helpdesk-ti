package com.helpdesk;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean UserDetailsService userDetailsService(Users users){return username->{
        var u=users.findByEmailIgnoreCase(username).orElseThrow(()->new org.springframework.security.core.userdetails.UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(u.email).password(u.passwordHash).roles(u.role.name()).disabled(!u.active).build();
    };}
    @Bean SecurityContextRepository securityContextRepository(){return new HttpSessionSecurityContextRepository();}
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception{return cfg.getAuthenticationManager();}
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,SecurityContextRepository repo) throws Exception{
        http.securityContext(c->c.securityContextRepository(repo))
            .csrf(c->{})
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(a->a
                .requestMatchers("/actuator/health","/api/auth/csrf","/api/auth/login").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e->e
                .authenticationEntryPoint((req,res,ex)->writeError(res,401,"Autenticação necessária."))
                .accessDeniedHandler((req,res,ex)->writeError(res,403,"Acesso negado.")))
            .logout(l->l.logoutUrl("/api/auth/logout").logoutSuccessHandler((req,res,auth)->res.setStatus(204)));
        return http.build();
    }
    private static void writeError(HttpServletResponse res,int status,String message) throws java.io.IOException {
        res.setStatus(status);res.setContentType(MediaType.APPLICATION_JSON_VALUE);res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"message\":\""+message+"\"}");
    }
}
