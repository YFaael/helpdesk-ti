package com.helpdesk;

import java.util.LinkedHashMap;
import java.util.Map;

final class Views {
    private Views() {}
    static Map<String,Object> user(UserAccount u) {
        var m = new LinkedHashMap<String,Object>();
        m.put("id",u.id);m.put("name",u.name);m.put("email",u.email);
        m.put("role",u.role);m.put("active",u.active);return m;
    }
    static Map<String,Object> category(Category c) {return Map.of("id",c.id,"name",c.name);}
    static Map<String,Object> ticket(Ticket t) {
        var m = new LinkedHashMap<String,Object>();
        m.put("id",t.id);m.put("title",t.title);m.put("description",t.description);
        m.put("priority",t.priority);m.put("status",t.status);
        m.put("requester",user(t.requester));m.put("assignee",t.assignee==null?null:user(t.assignee));
        m.put("category",category(t.category));m.put("createdAt",t.createdAt);
        m.put("updatedAt",t.updatedAt);m.put("resolvedAt",t.resolvedAt);m.put("closedAt",t.closedAt);
        return m;
    }
    static Map<String,Object> comment(TicketComment c) {
        return Map.of("id",c.id,"ticketId",c.ticket.id,"author",user(c.author),"message",c.message,"internal",c.internal,"createdAt",c.createdAt);
    }
    static Map<String,Object> history(TicketHistory h) {
        return Map.of("id",h.id,"ticketId",h.ticket.id,"actor",user(h.actor),"type",h.type,"detail",h.detail,"createdAt",h.createdAt);
    }
}
