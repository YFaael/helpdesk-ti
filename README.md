# HelpDesk TI

Sistema web para registrar e acompanhar chamados de suporte técnico. Este é um projeto de estudo que estou usando para praticar desenvolvimento web, organização de código e integração com banco de dados.

A ideia é simples: um funcionário informa um problema, e a equipe de TI acompanha o atendimento até a resolução.

## Funcionalidades

- Abertura de chamados com título, descrição, categoria e prioridade.
- Lista de chamados com busca e filtros por status e prioridade.
- Visualização de detalhes, comentários e histórico do atendimento.
- Atualização de status e atribuição de técnico responsável.
- Visão geral da quantidade de chamados por situação.
- Interface adaptada para computador e celular.

## Tecnologias

- **Frontend:** HTML, CSS e JavaScript.
- **Backend:** Java 21 e Spring Boot.
- **Banco de dados:** PostgreSQL.
- **Outras ferramentas:** Docker Compose, Flyway e Git.

## Como testar a interface

A forma mais simples é abrir `frontend/index.html` no navegador. No Windows, você também pode executar `ABRIR_DEMO.bat`.

Nessa versão, é possível alternar entre os perfis de funcionário, técnico e administrador para testar o fluxo dos chamados. Os dados ficam salvos no próprio navegador (`localStorage`). **Esse modo não usa login real nem compartilha informações entre computadores.**

## Como executar com o backend

É necessário ter o Docker Desktop instalado e em execução.

1. Faça uma cópia do arquivo `.env.example` com o nome `.env`.
2. Defina uma senha local em `POSTGRES_PASSWORD` no arquivo `.env`.
3. Abra um terminal na pasta principal do projeto.
4. Execute:

```bash
docker compose up --build -d
```

Depois, acesse `http://localhost:8088`.

Para encerrar:

```bash
docker compose down
```

O backend inclui autenticação e armazenamento no PostgreSQL, mas **a execução integrada com Docker ainda precisa ser validada neste projeto**. O ambiente foi pensado para testes locais, não para uso público em produção.

## Testes

Para executar os testes do modo de demonstração, com Node.js instalado:

```bash
node --test tests/demo.test.js
```

Para executar os testes do backend, com Java 21 e Maven instalados:

```bash
cd backend
mvn test
```

## Organização do projeto

```text
frontend/       Interface web
backend/        API em Java
tests/          Testes do modo local
docs/           Anotações e melhorias planejadas
compose.yaml    Serviços do ambiente local
```

## Próximas melhorias

- [ ] Adicionar filtro por categoria.
- [ ] Melhorar as mensagens de validação dos formulários.
- [ ] Criar testes de integração para a API.
- [ ] Validar o projeto completo com Docker.
- [ ] Adicionar capturas de tela da aplicação.

O projeto continua em desenvolvimento. Estou priorizando entender as funcionalidades, revisar o código e registrar as melhorias ao longo do tempo.
