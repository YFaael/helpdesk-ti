package com.helpdesk;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepository;
    private final Users users;
    AuthController(AuthenticationManager am,SecurityContextRepository cr,Users users){this.authManager=am;this.contextRepository=cr;this.users=users;}
    record LoginRequest(@Email @NotBlank String email,@NotBlank String password) {}
    @GetMapping("/csrf") Map<String,String> csrf(HttpServletRequest request){
        CsrfToken token=(CsrfToken)request.getAttribute(CsrfToken.class.getName());
        return Map.of("token",token.getToken());
    }
    @GetMapping("/me") Object me(Authentication auth){return Views.user(current(auth));}
    @PostMapping("/login") Object login(@Valid @RequestBody LoginRequest request,HttpServletRequest req,HttpServletResponse res){
        Authentication auth=authManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(),request.password()));
        req.getSession(true);req.changeSessionId();
        SecurityContext context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);contextRepository.saveContext(context,req,res);
        return Views.user(current(auth));
    }
    UserAccount current(Authentication auth){if(auth==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Faça login.");
        return users.findByEmailIgnoreCase(auth.getName()).filter(u->u.active).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuário inativo."));
    }
}
