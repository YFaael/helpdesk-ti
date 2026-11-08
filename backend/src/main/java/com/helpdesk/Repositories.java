package com.helpdesk;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface Users extends JpaRepository<UserAccount,Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    List<UserAccount> findByRoleInAndActiveTrueOrderByNameAsc(Collection<Role> roles);
    List<UserAccount> findAllByOrderByNameAsc();
}
interface Categories extends JpaRepository<Category,Long> {
    boolean existsByName(String name);
    List<Category> findAllByOrderByNameAsc();
}
interface Tickets extends JpaRepository<Ticket,Long> {
    List<Ticket> findAllByOrderByCreatedAtDesc();
    List<Ticket> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}
interface Comments extends JpaRepository<TicketComment,Long> {
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
interface Histories extends JpaRepository<TicketHistory,Long> {
    List<TicketHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
