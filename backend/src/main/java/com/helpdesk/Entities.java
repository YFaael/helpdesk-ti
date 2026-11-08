package com.helpdesk;

import jakarta.persistence.*;
import java.time.Instant;

enum Role { USER, TECH, ADMIN }
enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
enum TicketStatus { OPEN, IN_PROGRESS, WAITING, RESOLVED, CLOSED }

@Entity
@Table(name = "users")
class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=120) String name;
    @Column(nullable=false,length=255,unique=true) String email;
    @Column(name="password_hash",nullable=false,length=255) String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) Role role;
    @Column(nullable=false) boolean active = true;
    @Column(name="created_at",nullable=false) Instant createdAt = Instant.now();
    protected UserAccount() {}
    UserAccount(String name,String email,String passwordHash,Role role){this.name=name;this.email=email;this.passwordHash=passwordHash;this.role=role;}
}

@Entity
@Table(name = "categories")
class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable=false,unique=true,length=100) String name;
    protected Category() {}
    Category(String name){this.name=name;}
}

@Entity
@Table(name = "tickets")
class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=180) String title;
    @Column(nullable=false,columnDefinition="text") String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) Priority priority;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) TicketStatus status = TicketStatus.OPEN;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="requester_id") UserAccount requester;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="assignee_id") UserAccount assignee;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="category_id") Category category;
    @Column(name="created_at",nullable=false) Instant createdAt = Instant.now();
    @Column(name="updated_at",nullable=false) Instant updatedAt = Instant.now();
    @Column(name="resolved_at") Instant resolvedAt;
    @Column(name="closed_at") Instant closedAt;
    protected Ticket() {}
}

@Entity
@Table(name = "ticket_comments")
class TicketComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="ticket_id") Ticket ticket;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="author_id") UserAccount author;
    @Column(nullable=false,columnDefinition="text") String message;
    @Column(nullable=false) boolean internal;
    @Column(name="created_at",nullable=false) Instant createdAt = Instant.now();
    protected TicketComment() {}
}

@Entity
@Table(name = "ticket_history")
class TicketHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="ticket_id") Ticket ticket;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="actor_id") UserAccount actor;
    @Column(nullable=false,length=40) String type;
    @Column(nullable=false,columnDefinition="text") String detail;
    @Column(name="created_at",nullable=false) Instant createdAt = Instant.now();
    protected TicketHistory() {}
}
