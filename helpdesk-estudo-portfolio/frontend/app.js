/* Interface do HelpDesk. Código em JavaScript puro para facilitar manutenção. */
'use strict';

const dataSource = window.HelpdeskData;
const $ = (selector) => document.querySelector(selector);
const statusNames = {
  OPEN: 'Aberto',
  IN_PROGRESS: 'Em atendimento',
  WAITING: 'Aguardando',
  RESOLVED: 'Resolvido',
  CLOSED: 'Fechado'
};
const priorityNames = { LOW: 'Baixa', MEDIUM: 'Média', HIGH: 'Alta', CRITICAL: 'Crítica' };
const roleNames = { USER: 'Funcionário', TECH: 'Técnico', ADMIN: 'Administrador' };

const state = {
  user: null,
  tickets: [],
  categories: [],
  staff: [],
  page: 'dashboard',
  selectedId: null
};

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[char]));
}

function shortDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString('pt-BR');
}

function fullDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function showMessage(message, isError = false) {
  const element = $('#feedback');
  element.textContent = message;
  element.classList.toggle('bad', isError);
  element.hidden = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function clearMessage() {
  $('#feedback').hidden = true;
}

function showLogin() {
  $('#login').hidden = false;
  $('#workspace').hidden = true;
  $('#login-error').hidden = true;
}

function showLoginError(error) {
  $('#login-error').textContent = error.message || String(error);
  $('#login-error').hidden = false;
}

async function loadData() {
  const client = dataSource.client;
  const [tickets, categories] = await Promise.all([client.tickets(), client.categories()]);
  state.tickets = tickets;
  state.categories = categories;
  state.staff = state.user.role === 'USER' ? [] : await client.staff();
}

async function enter(user) {
  state.user = user;
  await loadData();
  $('#profile-name').textContent = user.name;
  $('#profile-initial').textContent = user.name.charAt(0);
  $('#profile-role').textContent = roleNames[user.role];
  $('#mode-tag').textContent = dataSource.mode === 'demo' ? 'Demonstração local' : 'Conectado ao servidor';
  $('#login').hidden = true;
  $('#workspace').hidden = false;
  renderCategoryOptions();
  navigate('dashboard');
}

function renderCategoryOptions() {
  const select = $('#new-category');
  select.replaceChildren();
  for (const category of state.categories) {
    const option = document.createElement('option');
    option.value = category.id;
    option.textContent = category.name;
    select.appendChild(option);
  }
}

function navigate(page) {
  state.page = page;
  clearMessage();
  for (const section of ['dashboard', 'tickets', 'detail']) {
    $('#' + section + '-page').hidden = section !== page;
  }
  $('.sidebar').classList.remove('expanded');
  document.querySelectorAll('[data-page]').forEach((button) => {
    button.classList.toggle('active', button.dataset.page === page);
  });
  const titles = {
    dashboard: ['PAINEL', 'Visão geral', 'Resumo dos atendimentos registrados.'],
    tickets: ['SOLICITAÇÕES', 'Chamados', 'Consulte os chamados e acompanhe os atendimentos.'],
    detail: ['ATENDIMENTO', 'Detalhes do chamado', 'Histórico e informações da solicitação.']
  };
  const [label, title, description] = titles[page];
  $('#page-label').textContent = label;
  $('#page-title').textContent = title;
  $('#page-description').textContent = description;
  $('#breadcrumb-page').textContent = title;
  if (page === 'dashboard') renderDashboard();
  if (page === 'tickets') renderTickets();
  window.scrollTo(0, 0);
}

function ticketRow(ticket) {
  return `
    <button class="ticket-row" data-ticket-id="${ticket.id}" aria-label="Abrir chamado ${ticket.id}">
      <div><strong>${escapeHtml(ticket.title)}</strong><small>#${ticket.id} · ${escapeHtml(ticket.category.name)} · ${escapeHtml(ticket.requester.name)}</small></div>
      <span class="status ${ticket.status}">${statusNames[ticket.status] || '—'}</span>
      <span class="priority ${ticket.priority}">${priorityNames[ticket.priority] || '—'}</span>
      <span class="date">${shortDate(ticket.createdAt)}</span>
    </button>`;
}

function displayTicketList(selector, tickets) {
  $(selector).innerHTML = tickets.length
    ? tickets.map(ticketRow).join('')
    : '<p class="empty">Nenhum chamado encontrado.</p>';
}

function renderDashboard() {
  const tickets = state.tickets;
  $('#stat-total').textContent = tickets.length;
  $('#stat-open').textContent = tickets.filter((t) => t.status === 'OPEN').length;
  $('#stat-progress').textContent = tickets.filter((t) => ['IN_PROGRESS', 'WAITING'].includes(t.status)).length;
  $('#stat-done').textContent = tickets.filter((t) => ['RESOLVED', 'CLOSED'].includes(t.status)).length;
  displayTicketList('#recent-list', tickets.slice(0, 5));
}

function renderTickets() {
  const search = $('#search').value.toLowerCase().trim();
  const status = $('#status-filter').value;
  const priority = $('#priority-filter').value;
  const results = state.tickets.filter((ticket) => {
    const matchesText = ticket.title.toLowerCase().includes(search) || String(ticket.id).includes(search);
    return matchesText && (status === 'ALL' || ticket.status === status)
      && (priority === 'ALL' || ticket.priority === priority);
  });
  $('#ticket-counter').textContent = `${results.length} chamado(s)`;
  displayTicketList('#all-tickets', results);
}

function displayActivity(selector, entries, emptyMessage, buildEntry) {
  $(selector).innerHTML = entries.length
    ? entries.map(buildEntry).join('')
    : `<p class="subtle">${emptyMessage}</p>`;
}

async function openTicket(id) {
  const ticket = state.tickets.find((item) => item.id === Number(id));
  if (!ticket) return showMessage('Chamado não encontrado.', true);
  state.selectedId = ticket.id;
  navigate('detail');
  $('#detail-number').textContent = `CHAMADO #${ticket.id}`;
  $('#detail-title').textContent = ticket.title;
  $('#detail-status').textContent = statusNames[ticket.status];
  $('#detail-status').className = 'status ' + ticket.status;
  $('#detail-description').textContent = ticket.description;
  $('#detail-meta').innerHTML = `
    <span>Solicitante: <strong>${escapeHtml(ticket.requester.name)}</strong></span>
    <span>Categoria: <strong>${escapeHtml(ticket.category.name)}</strong></span>
    <span>Aberto em: <strong>${shortDate(ticket.createdAt)}</strong></span>
    <span>Atualizado em: <strong>${shortDate(ticket.updatedAt)}</strong></span>`;

  const isStaff = state.user.role !== 'USER';
  $('#update-form').hidden = !isStaff;
  $('#readonly-info').hidden = isStaff;
  $('#internal-label').hidden = !isStaff;
  $('#reopen-button').hidden = !(state.user.role === 'USER'
    && ticket.requester.id === state.user.id
    && ['RESOLVED', 'CLOSED'].includes(ticket.status));

  if (isStaff) {
    $('#edit-status').value = ticket.status;
    $('#edit-priority').value = ticket.priority;
    const select = $('#edit-assignee');
    select.innerHTML = '<option value="">Sem responsável</option>';
    state.staff.forEach((user) => {
      const option = document.createElement('option');
      option.value = user.id;
      option.textContent = user.name;
      select.appendChild(option);
    });
    select.value = ticket.assignee?.id || '';
  }

  try {
    const activity = await dataSource.client.activity(id);
    // Evita desenhar os detalhes de um chamado antigo se o usuário mudou de tela.
    if (state.selectedId !== id || state.page !== 'detail') return;
    displayActivity('#comments', activity.comments, 'Ainda não há comentários.', (comment) => `
      <div class="comment">
        <strong>${escapeHtml(comment.author.name)}</strong>
        <small> · ${fullDate(comment.createdAt)}${comment.internal ? ' · Nota interna' : ''}</small>
        <p>${escapeHtml(comment.message)}</p>
      </div>`);
    displayActivity('#history', activity.history, 'Sem movimentações.', (item) => `
      <div class="history-entry">
        <strong>${escapeHtml(item.actor.name)}</strong>
        <p>${escapeHtml(item.detail)}</p>
        <small>${fullDate(item.createdAt)}</small>
      </div>`);
  } catch (error) {
    showMessage(error.message, true);
  }
}

async function refresh() {
  await loadData();
  if (state.page === 'detail' && state.selectedId !== null) {
    await openTicket(state.selectedId);
  } else {
    navigate(state.page);
  }
}

function showNewTicketDialog() {
  $('#new-form').reset();
  $('#ticket-dialog').showModal();
}

function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function submitDisabled(form, disabled) {
  form.querySelector('button[type="submit"]').disabled = disabled;
}

// Eventos principais: navegação, abertura e consulta dos chamados.
document.addEventListener('click', (event) => {
  const pageButton = event.target.closest('[data-page]');
  if (pageButton) navigate(pageButton.dataset.page);
  const ticketButton = event.target.closest('[data-ticket-id]');
  if (ticketButton) openTicket(Number(ticketButton.dataset.ticketId));
  const demoButton = event.target.closest('[data-demo-role]');
  if (demoButton) {
    dataSource.useDemo();
    dataSource.client.login(demoButton.dataset.demoRole).then(enter).catch(showLoginError);
  }
});

$('#new-ticket').addEventListener('click', showNewTicketDialog);
$('#close-dialog').addEventListener('click', () => $('#ticket-dialog').close());
$('#cancel-dialog').addEventListener('click', () => $('#ticket-dialog').close());
$('#back-to-tickets').addEventListener('click', () => navigate('tickets'));
$('#mobile-nav').addEventListener('click', () => $('.sidebar').classList.toggle('expanded'));
$('#search').addEventListener('input', renderTickets);
$('#status-filter').addEventListener('change', renderTickets);
$('#priority-filter').addEventListener('change', renderTickets);

$('#login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const { email, password } = formValues(form);
  submitDisabled(form, true);
  try {
    await enter(await dataSource.client.login(null, email, password));
  } catch (error) {
    showLoginError(error);
  } finally {
    submitDisabled(form, false);
  }
});

$('#logout').addEventListener('click', async () => {
  try {
    await dataSource.client.logout();
    state.user = null;
    showLogin();
  } catch (error) {
    showMessage(error.message, true);
  }
});

$('#new-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const values = formValues(form);
  submitDisabled(form, true);
  try {
    const ticket = await dataSource.client.create({
      title: values.title,
      description: values.description,
      categoryId: Number(values.categoryId),
      priority: values.priority
    });
    $('#ticket-dialog').close();
    await loadData();
    await openTicket(ticket.id);
    showMessage('Chamado registrado com sucesso.');
  } catch (error) {
    // O formulário continua aberto para o usuário corrigir os campos.
    alert(error.message);
  } finally {
    submitDisabled(form, false);
  }
});

$('#update-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const values = formValues(form);
  submitDisabled(form, true);
  try {
    await dataSource.client.update(state.selectedId, {
      status: values.status,
      priority: values.priority,
      assigneeId: values.assigneeId ? Number(values.assigneeId) : null
    });
    await refresh();
    showMessage('Atendimento atualizado.');
  } catch (error) {
    showMessage(error.message, true);
  } finally {
    submitDisabled(form, false);
  }
});

$('#comment-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const values = formValues(form);
  submitDisabled(form, true);
  try {
    await dataSource.client.comment(state.selectedId, {
      message: values.message,
      internal: values.internal === 'on'
    });
    form.reset();
    await refresh();
    showMessage('Comentário adicionado.');
  } catch (error) {
    showMessage(error.message, true);
  } finally {
    submitDisabled(form, false);
  }
});

$('#reopen-button').addEventListener('click', async () => {
  try {
    await dataSource.client.reopen(state.selectedId);
    await refresh();
    showMessage('Chamado reaberto.');
  } catch (error) {
    showMessage(error.message, true);
  }
});

async function initialize() {
  const mode = await dataSource.detectMode();
  if (mode === 'server') {
    $('#login-form').hidden = false;
    $('#demo-choice').hidden = true;
    try {
      const user = await dataSource.client.me();
      if (user) await enter(user);
    } catch (_) {
      showLogin();
    }
  } else {
    $('#login-form').hidden = true;
    $('#demo-choice').hidden = false;
  }
}

initialize();
