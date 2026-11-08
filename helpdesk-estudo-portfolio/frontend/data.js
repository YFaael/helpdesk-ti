/*
 * A interface pode usar uma API Spring Boot ou uma demonstração local.
 * Os dados locais são apenas para apresentação e não têm autenticação real.
 */
(function () {
  'use strict';

  const STORAGE_KEY = 'helpdesk-estudo-v1';
  const users = [
    { id: 1, name: 'Marina Costa', email: 'admin@helpdesk.local', role: 'ADMIN', active: true },
    { id: 2, name: 'Lucas Ferreira', email: 'tecnico@helpdesk.local', role: 'TECH', active: true },
    { id: 3, name: 'Ana Oliveira', email: 'usuario@helpdesk.local', role: 'USER', active: true },
    { id: 4, name: 'Pedro Martins', email: 'pedro@helpdesk.local', role: 'USER', active: true }
  ];
  const categories = [
    { id: 1, name: 'Hardware' },
    { id: 2, name: 'Software' },
    { id: 3, name: 'Rede e Internet' },
    { id: 4, name: 'Acesso e Senhas' },
    { id: 5, name: 'Outros' }
  ];

  function daysAgo(days) {
    return new Date(Date.now() - days * 86400000).toISOString();
  }

  function seed() {
    const examples = [
      { id: 1001, title: 'Notebook não inicia', description: 'Depois da atualização do Windows, o notebook fica com a tela preta.', categoryId: 1, priority: 'HIGH', status: 'IN_PROGRESS', requesterId: 3, assigneeId: 2, days: 1 },
      { id: 1002, title: 'Acesso ao e-mail corporativo', description: 'Não consigo entrar no e-mail com a senha atual.', categoryId: 4, priority: 'MEDIUM', status: 'OPEN', requesterId: 4, assigneeId: null, days: 2 },
      { id: 1003, title: 'Instalação de editor de PDF', description: 'Preciso de um editor de PDF para trabalhar com os documentos do setor.', categoryId: 2, priority: 'LOW', status: 'WAITING', requesterId: 3, assigneeId: 2, days: 3 },
      { id: 1004, title: 'Impressora sem conexão', description: 'A impressora de rede não aparece na lista de dispositivos.', categoryId: 3, priority: 'MEDIUM', status: 'RESOLVED', requesterId: 4, assigneeId: 2, days: 5 }
    ];

    const tickets = examples.map((item) => ({
      id: item.id,
      title: item.title,
      description: item.description,
      category: categories.find((category) => category.id === item.categoryId),
      requester: users.find((user) => user.id === item.requesterId),
      assignee: users.find((user) => user.id === item.assigneeId) || null,
      priority: item.priority,
      status: item.status,
      createdAt: daysAgo(item.days),
      updatedAt: daysAgo(item.days),
      resolvedAt: item.status === 'RESOLVED' ? daysAgo(item.days) : null,
      closedAt: null
    }));

    const history = tickets.map((ticket) => ({
      id: ticket.id,
      ticketId: ticket.id,
      actor: ticket.requester,
      type: 'CREATED',
      detail: 'Chamado aberto',
      createdAt: ticket.createdAt
    }));

    return { tickets, comments: [], history, nextId: 1005 };
  }

  function readData() {
    try {
      const content = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (content && Array.isArray(content.tickets) && Array.isArray(content.comments)) {
        return content;
      }
    } catch (error) {
      console.warn('Não foi possível carregar os dados locais.', error);
    }
    return seed();
  }

  let demo = readData();
  let demoUser = null;
  let mode = 'demo';

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(demo));
    } catch (error) {
      console.warn('Não foi possível salvar os dados locais.', error);
    }
  }

  function currentUser() {
    return demoUser;
  }

  function allowed(ticket) {
    return demoUser.role !== 'USER' || ticket.requester.id === demoUser.id;
  }

  function findTicket(id) {
    const ticket = demo.tickets.find((item) => item.id === Number(id));
    if (!ticket || !allowed(ticket)) throw new Error('Chamado não encontrado.');
    return ticket;
  }

  function registerHistory(ticket, type, detail) {
    demo.history.push({
      id: Date.now() + Math.random(),
      ticketId: ticket.id,
      actor: demoUser,
      type,
      detail,
      createdAt: new Date().toISOString()
    });
  }

  const local = {
    async login(role) {
      demoUser = users.find((user) => user.role === role);
      return demoUser;
    },
    async logout() { demoUser = null; },
    async me() { return currentUser(); },
    async categories() { return categories; },
    async staff() { return users.filter((user) => ['ADMIN', 'TECH'].includes(user.role)); },
    async tickets() {
      return demo.tickets.filter(allowed).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    },
    async activity(id) {
      findTicket(id);
      return {
        comments: demo.comments.filter((item) => item.ticketId === Number(id) && (!item.internal || demoUser.role !== 'USER')),
        history: demo.history.filter((item) => item.ticketId === Number(id))
      };
    },
    async create(payload) {
      if (payload.title.trim().length < 5 || payload.description.trim().length < 10) {
        throw new Error('Preencha um título e uma descrição válidos.');
      }
      const category = categories.find((item) => item.id === Number(payload.categoryId));
      if (!category) throw new Error('Selecione uma categoria.');
      const date = new Date().toISOString();
      const ticket = {
        id: demo.nextId++,
        title: payload.title.trim(),
        description: payload.description.trim(),
        category,
        requester: demoUser,
        assignee: null,
        status: 'OPEN',
        priority: payload.priority,
        createdAt: date,
        updatedAt: date,
        resolvedAt: null,
        closedAt: null
      };
      demo.tickets.push(ticket);
      registerHistory(ticket, 'CREATED', 'Chamado aberto');
      persist();
      return ticket;
    },
    async update(id, payload) {
      if (demoUser.role === 'USER') throw new Error('Apenas a equipe técnica pode atualizar chamados.');
      const ticket = findTicket(id);
      const assignee = payload.assigneeId ? users.find((user) => user.id === Number(payload.assigneeId)) : null;
      if (payload.assigneeId && (!assignee || assignee.role === 'USER')) throw new Error('Responsável inválido.');
      if (ticket.status !== payload.status) registerHistory(ticket, 'STATUS_CHANGED', `Status alterado de ${ticket.status} para ${payload.status}`);
      if (ticket.priority !== payload.priority) registerHistory(ticket, 'PRIORITY_CHANGED', `Prioridade alterada de ${ticket.priority} para ${payload.priority}`);
      if ((ticket.assignee?.id || null) !== (assignee?.id || null)) registerHistory(ticket, 'ASSIGNED', `Responsável: ${assignee?.name || 'Sem responsável'}`);
      ticket.status = payload.status;
      ticket.priority = payload.priority;
      ticket.assignee = assignee;
      ticket.updatedAt = new Date().toISOString();
      ticket.resolvedAt = ticket.status === 'RESOLVED' ? ticket.updatedAt : null;
      ticket.closedAt = ticket.status === 'CLOSED' ? ticket.updatedAt : null;
      persist();
      return ticket;
    },
    async comment(id, payload) {
      const ticket = findTicket(id);
      if (!payload.message || payload.message.trim().length < 2) throw new Error('Escreva um comentário válido.');
      if (payload.internal && demoUser.role === 'USER') throw new Error('Notas internas são exclusivas da equipe.');
      demo.comments.push({
        id: Date.now(),
        ticketId: ticket.id,
        author: demoUser,
        message: payload.message.trim(),
        internal: Boolean(payload.internal),
        createdAt: new Date().toISOString()
      });
      ticket.updatedAt = new Date().toISOString();
      persist();
    },
    async reopen(id) {
      const ticket = findTicket(id);
      if (demoUser.role !== 'USER' || ticket.requester.id !== demoUser.id || !['RESOLVED', 'CLOSED'].includes(ticket.status)) {
        throw new Error('Este chamado não pode ser reaberto.');
      }
      ticket.status = 'OPEN';
      ticket.updatedAt = new Date().toISOString();
      ticket.resolvedAt = null;
      ticket.closedAt = null;
      registerHistory(ticket, 'REOPENED', 'Chamado reaberto');
      persist();
    }
  };

  async function request(path, method = 'GET', body) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET') {
      const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
      if (!response.ok) throw new Error('Não foi possível verificar a sessão.');
      const token = await response.json();
      headers['X-CSRF-TOKEN'] = token.token;
    }
    const response = await fetch('/api' + path, {
      method,
      headers,
      credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body)
    });
    if (!response.ok) {
      let data = {};
      try { data = await response.json(); } catch (_) { /* resposta sem JSON */ }
      throw new Error(data.message || `Erro na solicitação (${response.status}).`);
    }
    if (response.status === 204) return null;
    return response.json();
  }

  const server = {
    async login(_, email, password) { return request('/auth/login', 'POST', { email, password }); },
    async logout() { return request('/auth/logout', 'POST'); },
    async me() { return request('/auth/me'); },
    async categories() { return request('/categories'); },
    async staff() { return request('/users/staff'); },
    async tickets() { return request('/tickets'); },
    async activity(id) { return request(`/tickets/${id}/activity`); },
    async create(data) { return request('/tickets', 'POST', data); },
    async update(id, data) { return request(`/tickets/${id}`, 'PATCH', data); },
    async comment(id, data) { return request(`/tickets/${id}/comments`, 'POST', data); },
    async reopen(id) { return request(`/tickets/${id}/reopen`, 'POST'); }
  };

  window.HelpdeskData = {
    async detectMode() {
      try {
        const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
        const data = await response.json();
        mode = response.ok && data.token ? 'server' : 'demo';
      } catch (_) { mode = 'demo'; }
      return mode;
    },
    useDemo() { mode = 'demo'; },
    get mode() { return mode; },
    get client() { return mode === 'server' ? server : local; }
  };
})();
