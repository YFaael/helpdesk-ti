package com.helpdesk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@Transactional
class TicketController {
    private final Tickets tickets;
    private final Comments comments;
    private final Histories histories;
    private final Categories categories;
    private final Users users;
    private final AuthController authController;
    TicketController(Tickets tickets, Comments comments, Histories histories,Categories categories,Users users,AuthController authController){
        this.tickets=tickets;this.comments=comments;this.histories=histories;this.categories=categories;this.users=users;this.authController=authController;
    }
    record CreateRequest(@NotBlank @Size(min=5,max=180) String title,@NotBlank @Size(min=10,max=5000) String description,@NotNull Long categoryId,@NotNull Priority priority) {}
    record UpdateRequest(@NotNull TicketStatus status,@NotNull Priority priority,Long assigneeId) {}
    record CommentRequest(@NotBlank @Size(min=2,max=3000) String message,boolean internal) {}

    @GetMapping("/categories") List<Map<String,Object>> getCategories() {
        return categories.findAllByOrderByNameAsc().stream().map(Views::category).toList();
    }
    @GetMapping("/tickets") List<Map<String,Object>> list(Authentication auth) {
        var actor=authController.current(auth);
        var ts=actor.role==Role.USER?tickets.findByRequesterIdOrderByCreatedAtDesc(actor.id):tickets.findAllByOrderByCreatedAtDesc();
        return ts.stream().map(Views::ticket).toList();
    }
    @GetMapping("/tickets/{id}") Map<String,Object> one(@PathVariable Long id,Authentication auth) {return Views.ticket(allowedTicket(id,auth));}
    @PostMapping("/tickets") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> create(@Valid @RequestBody CreateRequest input,Authentication auth) {
        var actor=authController.current(auth);
        var category=categories.findById(input.categoryId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Categoria inválida."));
        var t=new Ticket();t.title=input.title().trim();t.description=input.description().trim();t.category=category;t.requester=actor;t.priority=input.priority();t.status=TicketStatus.OPEN;
        if(t.title.length()<5||t.description.length()<10)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Título ou descrição muito curtos.");
        tickets.save(t);addHistory(t,actor,"CREATED","Chamado aberto");return Views.ticket(t);
    }
    @PatchMapping("/tickets/{id}") Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody UpdateRequest input,Authentication auth) {
        var actor=authController.current(auth);
        if(!TicketPolicy.canManage(actor.role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente a equipe de suporte pode atualizar um atendimento.");
        var t=allowedTicket(id,auth);
        var newAssignee=input.assigneeId()==null?null:users.findById(input.assigneeId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Responsável inválido."));
        if(newAssignee!=null&&(!newAssignee.active||!TicketPolicy.canManage(newAssignee.role)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Escolha um técnico ou administrador ativo.");
        if(input.status()!=t.status){addHistory(t,actor,"STATUS_CHANGED","Status: "+t.status+" → "+input.status());t.status=input.status();}
        if(input.priority()!=t.priority){addHistory(t,actor,"PRIORITY_CHANGED","Prioridade: "+t.priority+" → "+input.priority());t.priority=input.priority();}
        if(!java.util.Objects.equals(t.assignee==null?null:t.assignee.id,newAssignee==null?null:newAssignee.id)) {
            addHistory(t,actor,"ASSIGNED","Responsável: "+(newAssignee==null?"Não atribuído":newAssignee.name));t.assignee=newAssignee;
        }
        Instant now=Instant.now();
        if(t.status==TicketStatus.RESOLVED&&t.resolvedAt==null)t.resolvedAt=now;
        if(t.status==TicketStatus.CLOSED&&t.closedAt==null)t.closedAt=now;
        if(t.status==TicketStatus.OPEN||t.status==TicketStatus.IN_PROGRESS||t.status==TicketStatus.WAITING){t.resolvedAt=null;t.closedAt=null;}
        t.updatedAt=now;return Views.ticket(t);
    }
    @PostMapping("/tickets/{id}/reopen") Map<String,Object> reopen(@PathVariable Long id,Authentication auth) {
        var actor=authController.current(auth);var t=allowedTicket(id,auth);
        if(!TicketPolicy.canReopen(actor.role,actor.id,t.requester.id,t.status))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Este chamado não pode ser reaberto.");
        t.status=TicketStatus.OPEN;t.updatedAt=Instant.now();t.resolvedAt=null;t.closedAt=null;
        addHistory(t,actor,"REOPENED","Chamado reaberto pelo solicitante");return Views.ticket(t);
    }
    @GetMapping("/tickets/{id}/activity") Map<String,Object> activity(@PathVariable Long id,Authentication auth) {
        var actor=authController.current(auth);allowedTicket(id,auth);
        return Map.of("comments",comments.findByTicketIdOrderByCreatedAtAsc(id).stream().filter(c->!c.internal||TicketPolicy.canManage(actor.role)).map(Views::comment).toList(),
                      "history",histories.findByTicketIdOrderByCreatedAtAsc(id).stream().map(Views::history).toList());
    }
    @PostMapping("/tickets/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> comment(@PathVariable Long id,@Valid @RequestBody CommentRequest input,Authentication auth) {
        var actor=authController.current(auth);var t=allowedTicket(id,auth);
        if(input.internal()&&!TicketPolicy.canManage(actor.role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Nota interna exige perfil técnico.");
        if(input.message().trim().length()<2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Comentário muito curto.");
        var c=new TicketComment();c.ticket=t;c.author=actor;c.message=input.message().trim();c.internal=input.internal();
        comments.save(c);t.updatedAt=Instant.now();return Views.comment(c);
    }
    private Ticket allowedTicket(Long id,Authentication auth){
        var actor=authController.current(auth);
        var t=tickets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Chamado não encontrado."));
        if(!TicketPolicy.canSee(actor.role,actor.id,t.requester.id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Chamado não encontrado.");
        return t;
    }
    private void addHistory(Ticket t,UserAccount actor,String type,String detail){
        var h=new TicketHistory();h.ticket=t;h.actor=actor;h.type=type;h.detail=detail;histories.save(h);
    }
}
