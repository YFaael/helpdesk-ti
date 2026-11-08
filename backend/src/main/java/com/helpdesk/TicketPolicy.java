package com.helpdesk;

/** Centraliza as regras de autorização que não dependem de infraestrutura. */
final class TicketPolicy {
    private TicketPolicy() {}
    static boolean canSee(Role role,long actorId,long requesterId) {
        return role==Role.ADMIN || role==Role.TECH || actorId==requesterId;
    }
    static boolean canManage(Role role) {
        return role==Role.ADMIN || role==Role.TECH;
    }
    static boolean canReopen(Role role,long actorId,long requesterId,TicketStatus status) {
        return role==Role.USER && actorId==requesterId && (status==TicketStatus.RESOLVED||status==TicketStatus.CLOSED);
    }
}
