package com.helpdesk;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class SeedData {
    @Bean CommandLineRunner seed(Users users,Categories categories,Tickets tickets,Histories histories,PasswordEncoder encoder,@Value("${app.seed-demo:false}") boolean enabled){
        return args->{
            if(!enabled||users.count()>0)return;
            var admin=users.save(new UserAccount("Marina Costa","admin@helpdesk.local",encoder.encode("Demo@1234"),Role.ADMIN));
            var tech=users.save(new UserAccount("Lucas Ferreira","tecnico@helpdesk.local",encoder.encode("Demo@1234"),Role.TECH));
            var user=users.save(new UserAccount("Ana Oliveira","usuario@helpdesk.local",encoder.encode("Demo@1234"),Role.USER));
            var other=users.save(new UserAccount("Pedro Martins","pedro@helpdesk.local",encoder.encode("Demo@1234"),Role.USER));
            var cat=List.of(categories.save(new Category("Hardware")),categories.save(new Category("Software")),categories.save(new Category("Rede e Internet")),categories.save(new Category("Acesso e Senhas")),categories.save(new Category("Outros")));
            make(tickets,histories,user,tech,cat.get(0),"Notebook não inicia após atualização","A tela permanece preta após a atualização do sistema.",Priority.HIGH,TicketStatus.IN_PROGRESS,2);
            make(tickets,histories,other,null,cat.get(3),"Problema com acesso ao e-mail","Não consigo acessar o e-mail depois da redefinição.",Priority.MEDIUM,TicketStatus.OPEN,1);
            make(tickets,histories,other,tech,cat.get(2),"Internet lenta no setor","A internet oscila durante todo o expediente.",Priority.CRITICAL,TicketStatus.OPEN,0);
            make(tickets,histories,user,tech,cat.get(1),"Instalação de aplicativo","Preciso do aplicativo para uma reunião.",Priority.LOW,TicketStatus.WAITING,5);
            make(tickets,histories,other,tech,cat.get(0),"Impressora sem conexão","A impressora não está aparecendo na rede.",Priority.MEDIUM,TicketStatus.RESOLVED,4);
        };
    }
    private void make(Tickets tickets,Histories histories,UserAccount requester,UserAccount assignee,Category category,String title,String description,Priority priority,TicketStatus status,int days){
        var t=new Ticket();t.requester=requester;t.assignee=assignee;t.category=category;t.title=title;t.description=description;t.priority=priority;t.status=status;
        t.createdAt=Instant.now().minus(days,ChronoUnit.DAYS);t.updatedAt=t.createdAt;
        if(status==TicketStatus.RESOLVED)t.resolvedAt=t.createdAt.plus(4,ChronoUnit.HOURS);
        tickets.save(t);
        var h=new TicketHistory();h.ticket=t;h.actor=requester;h.type="CREATED";h.detail="Chamado aberto";h.createdAt=t.createdAt;histories.save(h);
    }
}
