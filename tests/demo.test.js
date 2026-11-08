// Testes simples do modo local. Execute: node --test tests/demo.test.js
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const script = fs.readFileSync(path.join(__dirname, '../frontend/data.js'), 'utf8');

function makeClient() {
  const saved = new Map();
  const localStorage = {
    getItem: (key) => saved.get(key) || null,
    setItem: (key, value) => saved.set(key, value)
  };
  const context = { window: {}, localStorage, console, Date, Math, fetch: async () => { throw new Error('Sem servidor'); } };
  vm.runInNewContext(script, context);
  return context.window.HelpdeskData;
}

test('funcionário vê somente seus próprios chamados', async () => {
  const data = makeClient();
  await data.detectMode();
  await data.client.login('USER');
  const tickets = await data.client.tickets();
  assert.equal(tickets.length, 2);
  assert.ok(tickets.every((ticket) => ticket.requester.id === 3));
});

test('funcionário pode criar e comentar, mas não alterar o atendimento', async () => {
  const data = makeClient();
  await data.detectMode();
  await data.client.login('USER');
  const ticket = await data.client.create({
    title: 'Erro na impressora',
    description: 'A impressora apresenta erro de conexão.',
    categoryId: 1,
    priority: 'LOW'
  });
  assert.equal(ticket.status, 'OPEN');
  await data.client.comment(ticket.id, { message: 'O erro continua.', internal: false });
  const activity = await data.client.activity(ticket.id);
  assert.equal(activity.comments.length, 1);
  await assert.rejects(() => data.client.update(ticket.id, { status: 'RESOLVED', priority: 'LOW' }));
});

test('técnico vê os chamados e pode marcar como resolvido', async () => {
  const data = makeClient();
  await data.detectMode();
  await data.client.login('TECH');
  const tickets = await data.client.tickets();
  assert.equal(tickets.length, 4);
  const updated = await data.client.update(1002, { status: 'RESOLVED', priority: 'MEDIUM', assigneeId: 2 });
  assert.equal(updated.status, 'RESOLVED');
  assert.equal(updated.assignee.id, 2);
  assert.ok((await data.client.activity(1002)).history.length > 1);
});

test('notas internas não ficam visíveis para funcionários', async () => {
  const data = makeClient();
  await data.detectMode();
  await data.client.login('TECH');
  await data.client.comment(1001, { message: 'Analisar registro interno.', internal: true });
  await data.client.login('USER');
  assert.equal((await data.client.activity(1001)).comments.length, 0);
});
