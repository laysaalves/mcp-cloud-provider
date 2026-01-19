
# 🚀 MCP Cloud Provider

## ✅ O que esse projeto faz?

- Aceita comandos em linguagem natural via LLM + Spring AI (MCP).
- Gera arquivos Terraform dinamicamente a partir de templates.
- Cria infraestrutura automaticamente (ex: S3 + Lambda + IAM + notificações).
- Executa terraform init / plan / apply de forma automática.
- Ideal para testes locais com LocalStack ou uso real na AWS.

---

## 🧠 Tecnologias

- [Spring AI (MCP Server)](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
- [Terraform](https://www.terraform.io/)
- OpenAI / Qualquer LLM
- Spring Shell CLI (MCP Client)
- Docker
- S3 / Lambda / IAM (AWS) ou outra Cloud

---

## 🧪 Exemplo de prompt

```bash
chat crie um bucket chamado teste-mcp-aws em dev e em us-east-1
```

---

## 📁 Estrutura do projeto

```
/mcp-client       → shell para comunicação com a llm via prompt
/mcp-server       → tools para executar tarefa
/templates/       → templates do terraform para o mcp utilizar
```

---

## 🪪 Créditos

Criado por [Láysa Alves](https://linkedin.com/in/laysaalves) inspirado no projeto open source do [Pedro Carrijo](https://github.com/pedrocarrijo95/MultiCloudInfraAI)

---

## 📢 Contribuições

Esse projeto é **open-source** para contribuir com a comunidade! Fique à vontade para dar fork ou se inspirar e deixar uma estrelinha <3

---