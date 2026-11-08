package com.helpdesk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@Transactional
class UserController {
    private final Users users;
    private final AuthController authController;
    private final PasswordEncoder encoder;
    UserController(Users users,AuthController authController,PasswordEncoder encoder){this.users=users;this.authController=authController;this.encoder=encoder;}
    record CreateUser(@NotBlank @Size(min=3,max=120) String name,@NotBlank @Email String email,@NotBlank @Size(min=8,max=100) String password,@NotNull Role role) {}
    private UserAccount admin(Authentication auth){var a=authController.current(auth);if(a.role!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Apenas administradores.");return a;}
    @GetMapping("/users/staff") List<Map<String,Object>> staff(Authentication auth){
        var a=authController.current(auth);if(a.role==Role.USER)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso restrito.");
        return users.findByRoleInAndActiveTrueOrderByNameAsc(List.of(Role.TECH,Role.ADMIN)).stream().map(Views::user).toList();
    }
    @GetMapping("/admin/users") List<Map<String,Object>> list(Authentication auth){admin(auth);return users.findAllByOrderByNameAsc().stream().map(Views::user).toList();}
    @PostMapping("/admin/users") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> create(@Valid @RequestBody CreateUser request,Authentication auth){
        admin(auth);String mail=request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if(users.findByEmailIgnoreCase(mail).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"E-mail já cadastrado.");
        var a=users.save(new UserAccount(request.name().trim(),mail,encoder.encode(request.password()),request.role()));return Views.user(a);
    }
    @PatchMapping("/admin/users/{id}/toggle") Map<String,Object> toggle(@PathVariable Long id,Authentication auth){
        var actor=admin(auth);if(actor.id.equals(id))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Você não pode desativar sua própria conta.");
        var u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado."));
        u.active=!u.active;return Views.user(u);
    }
}
