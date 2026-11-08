package com.helpdesk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicketPolicyTest {
    @Test void funcionarioSoVeSeusChamados(){
        assertTrue(TicketPolicy.canSee(Role.USER,3L,3L));
        assertFalse(TicketPolicy.canSee(Role.USER,3L,4L));
        assertTrue(TicketPolicy.canSee(Role.TECH,2L,4L));
        assertTrue(TicketPolicy.canSee(Role.ADMIN,1L,4L));
    }
    @Test void apenasEquipeGerenciaAtendimentos(){
        assertFalse(TicketPolicy.canManage(Role.USER));
        assertTrue(TicketPolicy.canManage(Role.TECH));
        assertTrue(TicketPolicy.canManage(Role.ADMIN));
    }
    @Test void reaberturaDependeDoDonoEStatus(){
        assertTrue(TicketPolicy.canReopen(Role.USER,3L,3L,TicketStatus.RESOLVED));
        assertFalse(TicketPolicy.canReopen(Role.USER,3L,4L,TicketStatus.RESOLVED));
        assertFalse(TicketPolicy.canReopen(Role.USER,3L,3L,TicketStatus.OPEN));
    }
}
