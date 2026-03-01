<div align="center">

<h1>🍅 PomoCube — Backend</h1>
<h3>API RESTful do Ecossistema PomoCube</h3>

<p>Serviço responsável pelo gerenciamento de ciclos Pomodoro, persistência de dados e estatísticas de produtividade para o <strong>PomoCube IoT</strong></p>

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

<br/>

> ⏱️ API RESTful do ecossistema PomoCube, responsável pelo gerenciamento de ciclos Pomodoro, persistência de dados e estatísticas de produtividade.

</div>

---

## 📋 Sumário

- [Sobre](#-sobre)
- [Tecnologias](#-tecnologias)
- [Arquitetura e Segurança](#-arquitetura-e-segurança)
- [Configuração do Ambiente](#-configuração-do-ambiente)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Endpoints da API](#-endpoints-da-api)
  - [Sessions](#sessions)
  - [Dashboard](#dashboard)
- [Formato de Erros](#-formato-de-erros)
- [CORS](#-cors)

---

## 🎯 Sobre

O backend do **PomoCube** é uma API RESTful construída com **Java Spring Boot** e **MongoDB Atlas**, projetada para receber, armazenar e consultar sessões de estudo enviadas pelo dispositivo físico PomoCube (Raspberry Pi Pico W). Além do registro de sessões, a API oferece um conjunto de endpoints de **dashboard** com métricas e estatísticas de produtividade prontas para visualização no frontend.

---

## 🛠 Tecnologias

- [Java](https://www.oracle.com/java/) + [Spring Boot](https://spring.io/projects/spring-boot) — Framework principal da API
- [MongoDB Atlas](https://www.mongodb.com/atlas) — Banco de dados NoSQL na nuvem
- [Spring Security](https://spring.io/projects/spring-security) — Autenticação via API Key
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb) — Integração com MongoDB
- [Spring Validation](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#validation) — Validação de dados de entrada
- [Render](https://render.com/) — Plataforma de deploy em produção

---

Essa é uma abordagem muito sensata e alinhada com o princípio **YAGNI** (*You Ain't Gonna Need It*). Para um projeto pessoal e de usuário único, introduzir toda a infraestrutura de um *Identity Provider* ou a complexidade de expiração/refresh de tokens JWT muitas vezes traz mais "ruído" do que benefício real.

Aqui está uma sugestão de como estruturar essa justificativa de forma profissional e técnica para o seu README ou documentação:

---

## 🔐 Arquitetura e Segurança

A escolha do método de autenticação priorizou o equilíbrio entre **segurança** e **simplicidade operacional**.

### Justificativa Técnica

Optei por utilizar autenticação via header customizado `X-API-KEY` em vez de padrões mais complexos (como JWT ou OAuth2) devido à natureza do sistema: por ser um projeto de uso pessoal (usuário único), a implementação de fluxos de *token refresh* ou integração com provedores de identidade aumentaria a complexidade do código sem oferecer um ganho proporcional de segurança para este caso de uso específico.

Com o uso de chaves estáticas protegidas, consigo garantir:

1. **Baixo Overhead:** Sem necessidade de parsing complexo de tokens em cada requisição.
2. **Segurança Eficiente:** As chaves são tratadas como segredos de ambiente, mantendo a API protegida contra acessos não autorizados.
3. **Segregação de Funções:** Mesmo com chaves simples, o sistema implementa RBAC (Role-Based Access Control) para limitar o que cada cliente pode fazer.

### Implementação

A autenticação é obrigatória em **todas** as requisições. O sistema diferencia a origem através de duas chaves distintas:

| Chave | Role | Escopo de Permissões |
| --- | --- | --- |
| `DEVICE_API_KEY` | `ROLE_DEVICE` | Escrita (`POST /sessions`) e leitura geral (`GET`). |
| `FRONTEND_API_KEY` | `ROLE_FRONTEND` | Leitura restrita aos endpoints de visualização (`/sessions`, `/dashboard`). |

### Respostas de erro de autenticação

| Status | Situação                                      |
|--------|-----------------------------------------------|
| `401`  | Header `X-API-KEY` ausente ou chave inválida  |
| `403`  | Chave sem permissão para o endpoint acessado  |

---

## ⚙️ Configuração do Ambiente

### Pré-requisitos

- [Java 17+](https://adoptium.net/)
- [Maven](https://maven.apache.org/) ou [Gradle](https://gradle.org/)
- Conta no [MongoDB Atlas](https://www.mongodb.com/atlas) (ou instância local)

### 1. Clone o repositório

```bash
git clone https://github.com/ClarkAshida/pomodoro-api
cd pomodoro-api
```

### 2. Configure as variáveis de ambiente

Crie um arquivo `.env` ou configure as variáveis diretamente no ambiente (veja a seção abaixo).

### 3. Execute a aplicação

```bash
# Maven
./mvnw spring-boot:run

```

A API estará disponível em: **[http://localhost:8080](http://localhost:8080)**

---

## 🔧 Variáveis de Ambiente

| Variável                 | Default (dev)                       | Descrição                               |
|--------------------------|-------------------------------------|-----------------------------------------|
| `SPRING_DATA_MONGODB_URI`| —                                   | URI de conexão com o MongoDB Atlas      |
| `DEVICE_API_KEY`         | `pomodoro-pico-w-secret-key-2025`   | Chave de autenticação do dispositivo    |
| `FRONTEND_API_KEY`       | `pomodoro-frontend-secret-key-2025` | Chave de autenticação do frontend       |
| `CORS_ALLOWED_ORIGINS`   | `http://localhost:3000`             | Origins permitidas (separadas por vírgula) |

---

### Header obrigatório em todas as requisições

```
X-API-KEY: <sua-api-key>
```

---

### Sessions

#### `POST /sessions`
Registra uma nova sessão de estudo.

**Permissão:** `ROLE_DEVICE`

```bash
curl -X POST http://localhost:8080/sessions \
  -H "X-API-KEY: <DEVICE_API_KEY>" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-02-27",
    "startTime": "14:30:00",
    "targetCycles": 4,
    "completedCycles": 4,
    "totalFocusMinutes": 100,
    "totalBreakMinutes": 10,
    "category": "TECHNOLOGY"
  }'
```

**Campos do body:**

| Campo               | Tipo      | Obrigatório | Descrição                           |
|---------------------|-----------|-------------|-------------------------------------|
| `date`              | `string`  | ✅          | Data da sessão (`YYYY-MM-DD`)       |
| `startTime`         | `string`  | ✅          | Hora de início (`HH:MM:SS`)         |
| `targetCycles`      | `integer` | ✅          | Meta de ciclos (mínimo `1`)         |
| `completedCycles`   | `integer` | ✅          | Ciclos concluídos (mínimo `0`)      |
| `totalFocusMinutes` | `integer` | ✅          | Minutos em foco (mínimo `0`)        |
| `totalBreakMinutes` | `integer` | ✅          | Minutos em pausa (mínimo `0`)       |
| `category`          | `string`  | ✅          | `TECHNOLOGY`, `MATH`, `PORTUGUESE`, `ENGLISH`, `OTHER` |

> Os campos `dayOfWeek`, `success` e `period` são **calculados automaticamente** pela API e retornados na resposta.

**Resposta `201 Created`:**

```json
{
  "id": "67c0a1b2e4f5a6b7c8d9e0f1",
  "date": "2026-02-27",
  "dayOfWeek": "FRIDAY",
  "startTime": "14:30:00",
  "period": "AFTERNOON",
  "category": "TECHNOLOGY",
  "targetCycles": 4,
  "completedCycles": 4,
  "success": true,
  "totalFocusMinutes": 100,
  "totalBreakMinutes": 10
}
```

---

#### `GET /sessions`
Lista sessões de forma paginada com suporte a filtros e ordenação.

**Permissão:** `ROLE_DEVICE` ou `ROLE_FRONTEND`

**Query Parameters:**

| Parâmetro   | Default  | Descrição                                               |
|-------------|----------|---------------------------------------------------------|
| `page`      | `0`      | Número da página                                        |
| `size`      | `10`     | Itens por página                                        |
| `sortBy`    | `date`   | Campo para ordenação                                    |
| `sortDir`   | `desc`   | `asc` ou `desc`                                         |
| `startDate` | —        | Filtro de data inicial (`YYYY-MM-DD`)                   |
| `endDate`   | —        | Filtro de data final (`YYYY-MM-DD`)                     |
| `period`    | —        | `MORNING`, `AFTERNOON` ou `NIGHT`                       |
| `category`  | —        | `TECHNOLOGY`, `MATH`, `PORTUGUESE`, `ENGLISH`, `OTHER`  |
| `success`   | —        | `true` ou `false`                                       |

---

#### `GET /sessions/{id}`
Busca uma sessão pelo ID.

**Permissão:** `ROLE_DEVICE` ou `ROLE_FRONTEND`

---

### Dashboard

Todos os endpoints de dashboard requerem `ROLE_DEVICE` ou `ROLE_FRONTEND`.

#### `GET /dashboard/summary`
Resumo geral: totais, médias e streak atual.

```json
{
  "totalSessions": 42,
  "successfulSessions": 30,
  "successRate": 71.43,
  "totalFocusMinutes": 2100,
  "averageCompletedCycles": 3.75,
  "averageFocusMinutes": 50.0,
  "currentStreak": 7
}
```

---

#### `GET /dashboard/overview`
Dados agrupados por período para gráficos de linha/barra.

**Query Parameters:** `period` (`week` | `month` | `year` | `all`), `startDate`, `endDate`

---

#### `GET /dashboard/by-category`
Distribuição de minutos e sessões por categoria (gráfico de pizza/donut).

**Query Parameters:** `period`, `startDate`, `endDate`

---

#### `GET /dashboard/heatmap`
Atividade diária do ano inteiro, estilo GitHub contribution graph.

**Query Parameters:** `year` (default: ano atual)

---

#### `GET /dashboard/by-period`
Estatísticas agrupadas por período do dia (manhã, tarde, noite).

---

#### `GET /dashboard/by-weekday`
Estatísticas agrupadas por dia da semana (`MONDAY → SUNDAY`).

---

#### `GET /dashboard/goals`
Métricas de progresso no período: dias ativos, médias e taxa de sucesso.

**Query Parameters:** `period`, `startDate`, `endDate`

---

### Resumo de Endpoints

| Método | Endpoint                 | Role necessária        | Descrição                      |
|--------|--------------------------|------------------------|--------------------------------|
| `POST` | `/sessions`              | `DEVICE`               | Registra nova sessão           |
| `GET`  | `/sessions`              | `DEVICE` ou `FRONTEND` | Lista sessões paginadas        |
| `GET`  | `/sessions/{id}`         | `DEVICE` ou `FRONTEND` | Busca sessão por ID            |
| `GET`  | `/dashboard/summary`     | `DEVICE` ou `FRONTEND` | Resumo geral + streak          |
| `GET`  | `/dashboard/overview`    | `DEVICE` ou `FRONTEND` | Dados por período (gráfico)    |
| `GET`  | `/dashboard/by-category` | `DEVICE` ou `FRONTEND` | Distribuição por categoria     |
| `GET`  | `/dashboard/heatmap`     | `DEVICE` ou `FRONTEND` | Heatmap de atividade anual     |
| `GET`  | `/dashboard/by-period`   | `DEVICE` ou `FRONTEND` | Stats por manhã/tarde/noite    |
| `GET`  | `/dashboard/by-weekday`  | `DEVICE` ou `FRONTEND` | Stats por dia da semana        |
| `GET`  | `/dashboard/goals`       | `DEVICE` ou `FRONTEND` | Metas e progresso no período   |

---

## 🗂️ Formato de Erros

Todos os erros seguem o padrão:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descrição do erro",
  "path": "/endpoint",
  "timestamp": "2026-02-27T14:30:00"
}
```

Erros de validação incluem o campo adicional `fieldErrors`:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields have invalid values",
  "fieldErrors": [
    {
      "field": "targetCycles",
      "rejectedValue": 0,
      "message": "targetCycles must be at least 1"
    }
  ]
}
```

| Status | Situação                                |
|--------|-----------------------------------------|
| `201`  | Sessão criada com sucesso               |
| `200`  | Requisição GET bem-sucedida             |
| `400`  | Dados inválidos / parâmetros incorretos |
| `401`  | API Key ausente ou inválida             |
| `403`  | Chave sem permissão para o endpoint     |
| `404`  | Sessão não encontrada                   |
| `500`  | Erro interno inesperado                 |

---

## 🌐 CORS

Para adicionar o frontend em produção, configure a variável de ambiente:

```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://seu-frontend.vercel.app
```

---

<div align="center">

Parte do ecossistema **PomoCube IoT** 🍅

</div>
