# Publicando o código no GitHub

1. Crie um repositório vazio no GitHub, por exemplo `helpdesk-ti`.
2. Extraia o ZIP e abra a pasta no VS Code.
3. Abra um terminal CMD na pasta raiz.
4. Revise o `.gitignore` e garanta que nenhum `.env` ou senha real será enviado.
5. Execute os comandos abaixo, substituindo o endereço pelo seu repositório.

```bash
git init -b main
git add .
git commit -m "versão inicial do sistema de chamados"
git remote add origin https://github.com/SEU_USUARIO/helpdesk-ti.git
git push -u origin main
```

Se `origin` já existir, use `git remote -v` e depois `git remote set-url origin URL_CORRETA`.

## Para uma boa apresentação

- Coloque 2 ou 3 capturas reais da interface no README, depois de executá-la no seu computador.
- Adicione uma seção explicando uma melhoria que você mesmo implementou e o problema que resolveu.
- Deixe claros os limites do modo local versus a API Java com PostgreSQL.
- Não inclua contas, dados ou senhas de empresas reais.
- Não declare funcionalidades ou tecnologias que não tenha testado e compreendido.

A qualidade de um portfólio não depende do número de telas: importa mais conseguir demonstrar e explicar o funcionamento do código.
