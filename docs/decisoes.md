# Decisões do projeto

## Escopo

O sistema tenta resolver apenas um problema: registrar e acompanhar chamados de TI. Recursos como chat em tempo real, notificações, gráficos elaborados e gamificação ficaram fora do escopo.

## Interface

Foi utilizado HTML, CSS e JavaScript puro para praticar DOM, eventos e formulários antes de avançar para um framework. O visual foi pensado como um painel administrativo simples: fundo claro, menu lateral, tabelas e poucas cores.

A interface foi dividida em `data.js`, responsável pelas operações de dados, e `app.js`, responsável pelas telas. Isso mantém os pontos de manutenção mais claros do que um único arquivo muito grande.

## Dados

O frontend detecta uma API em `/api/auth/csrf`. Quando o servidor está disponível, utiliza Spring Boot e PostgreSQL. Sem a API, permite um modo de demonstração local com dados no navegador.

Os perfis locais servem apenas para ilustrar o fluxo. A segurança real depende do backend, onde as permissões são verificadas de acordo com o usuário autenticado.

## O que eu melhoraria

- Paginação de chamados no servidor.
- Testes ponta a ponta para o login e para o fluxo completo.
- Mensagens de validação junto aos campos.
- Administração de categorias e abertura de chamados com anexos.
- Deploy com HTTPS, segredo externo e backup do banco.
