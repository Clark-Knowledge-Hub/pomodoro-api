# 🍅 Pomodoro API — Documentação

API REST desenvolvida em **Java Spring Boot** com **MongoDB Atlas** para armazenar e consultar sessões de estudo do Pomodoro Timer (Raspberry Pi Pico W).

---

## 📌 Base URL

```
Local:    http://localhost:8080
Produção: https://<seu-app>.onrender.com
```

---

## 🔐 Segurança — API Key

Todos os endpoints são **protegidos**. A autenticação é feita via header `X-API-KEY` em **todas** as requisições.

### Header obrigatório

```
X-API-KEY: <sua-api-key>
```

### Tipos de chave e permissões

| Chave               | Role           | Permissões                                      |
|---------------------|----------------|-------------------------------------------------|
| `DEVICE_API_KEY`    | `ROLE_DEVICE`  | `POST /sessions` + todos os `GET`               |
| `FRONTEND_API_KEY`  | `ROLE_FRONTEND`| Todos os `GET` (`/sessions`, `/dashboard`)      |

### Variáveis de ambiente

| Variável              | Default (dev)                        |
|-----------------------|--------------------------------------|
| `DEVICE_API_KEY`      | `pomodoro-pico-w-secret-key-2025`    |
| `FRONTEND_API_KEY`    | `pomodoro-frontend-secret-key-2025`  |

### Respostas de erro de autenticação

**401 — Header ausente:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Header X-API-KEY is required"
}
```

**401 — Chave inválida:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "The provided API Key is not valid"
}
```

**403 — Sem permissão (ex: FRONTEND tentando fazer POST):**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

---

## 📦 Enums

### `Period`
| Valor       | Descrição        |
|-------------|------------------|
| `MORNING`   | Manhã            |
| `AFTERNOON` | Tarde            |
| `NIGHT`     | Noite            |

### `Category`
| Valor        | Descrição    |
|--------------|--------------|
| `TECHNOLOGY` | Tecnologia   |
| `MATH`       | Matemática   |
| `PORTUGUESE` | Português    |
| `ENGLISH`    | Inglês       |
| `OTHER`      | Outros       |

### `DayOfWeek`
`MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

---

## 📋 Sessions

### `POST /sessions`
Registra uma nova sessão de estudo.

- **Permissão:** `ROLE_DEVICE`
- **Content-Type:** `application/json`

#### Headers
```
X-API-KEY: <DEVICE_API_KEY>
Content-Type: application/json
```

#### Request Body

| Campo               | Tipo        | Obrigatório | Validação                    | Descrição                             |
|---------------------|-------------|-------------|------------------------------|---------------------------------------|
| `date`              | `string`    | ✅          | formato `YYYY-MM-DD`         | Data da sessão                        |
| `startTime`         | `string`    | ✅          | formato `HH:MM:SS`           | Hora de início                        |
| `targetCycles`      | `integer`   | ✅          | mínimo `1`                   | Meta de ciclos pomodoro               |
| `completedCycles`   | `integer`   | ✅          | mínimo `0`                   | Ciclos concluídos                     |
| `totalFocusMinutes` | `integer`   | ✅          | mínimo `0`                   | Total de minutos em foco              |
| `totalBreakMinutes` | `integer`   | ✅          | mínimo `0`                   | Total de minutos em pausa/distração   |
| `category`          | `string`    | ✅          | enum `Category`              | Categoria estudada                    |

#### Campos derivados automaticamente

Os seguintes campos **não devem ser enviados** no body — são calculados pela API:

| Campo       | Lógica                                                                 |
|-------------|------------------------------------------------------------------------|
| `dayOfWeek` | Extraído de `date` (`date.getDayOfWeek()`)                             |
| `success`   | `true` se `completedCycles >= targetCycles`, `false` caso contrário    |
| `period`    | Derivado de `startTime`: `MORNING` (06:00–11:59), `AFTERNOON` (12:00–17:59), `NIGHT` (18:00–05:59) |

#### Exemplo de Request
```json
{
  "date": "2026-02-27",
  "startTime": "14:30:00",
  "targetCycles": 4,
  "completedCycles": 4,
  "totalFocusMinutes": 100,
  "totalBreakMinutes": 10,
  "category": "TECHNOLOGY"
}
```

#### Resposta — `201 Created`

> Os campos `dayOfWeek`, `success` e `period` são calculados automaticamente e retornados na resposta.

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

#### Erros de validação — `400 Bad Request`
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields have invalid values",
  "path": "/sessions",
  "timestamp": "2026-02-27T14:30:00",
  "fieldErrors": [
    {
      "field": "targetCycles",
      "rejectedValue": 0,
      "message": "targetCycles must be at least 1"
    }
  ]
}
```

---

### `GET /sessions`
Lista todas as sessões de forma paginada, com suporte a filtros e ordenação.

- **Permissão:** `ROLE_DEVICE` ou `ROLE_FRONTEND`

#### Headers
```
X-API-KEY: <DEVICE_API_KEY ou FRONTEND_API_KEY>
```

#### Query Parameters

| Parâmetro   | Tipo      | Default  | Descrição                                       |
|-------------|-----------|----------|-------------------------------------------------|
| `page`      | `integer` | `0`      | Número da página (começa em 0)                  |
| `size`      | `integer` | `10`     | Quantidade de itens por página                  |
| `sortBy`    | `string`  | `date`   | Campo para ordenação (`date`, `totalFocusMinutes`, etc.) |
| `sortDir`   | `string`  | `desc`   | Direção: `asc` ou `desc`                        |
| `startDate` | `string`  | —        | Filtro de data inicial (`YYYY-MM-DD`)           |
| `endDate`   | `string`  | —        | Filtro de data final (`YYYY-MM-DD`)             |
| `period`    | `string`  | —        | Filtro por período (`MORNING`, `AFTERNOON`, `NIGHT`) |
| `category`  | `string`  | —        | Filtro por categoria (`TECHNOLOGY`, `MATH`, ...) |
| `success`   | `boolean` | —        | Filtro por sucesso (`true` ou `false`)          |

#### Exemplo de Request
```
GET /sessions?page=0&size=5&sortBy=date&sortDir=desc&category=TECHNOLOGY&success=true
```

#### Resposta — `200 OK`
```json
{
  "content": [
    {
      "id": "67c0a1b2e4f5a6b7c8d9e0f1",
      "date": "2026-02-27",
      "period": "AFTERNOON",
      "category": "TECHNOLOGY",
      "completedCycles": 4,
      "targetCycles": 4,
      "success": true,
      "totalFocusMinutes": 100
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 5,
    "sort": { "sorted": true }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

---

### `GET /sessions/{id}`
Busca uma sessão pelo ID.

- **Permissão:** `ROLE_DEVICE` ou `ROLE_FRONTEND`

#### Headers
```
X-API-KEY: <DEVICE_API_KEY ou FRONTEND_API_KEY>
```

#### Path Parameter

| Parâmetro | Tipo     | Descrição              |
|-----------|----------|------------------------|
| `id`      | `string` | ID MongoDB da sessão   |

#### Exemplo de Request
```
GET /sessions/67c0a1b2e4f5a6b7c8d9e0f1
```

#### Resposta — `200 OK`
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

#### Erro — `404 Not Found`
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Session not found with id: 67c0a1b2e4f5a6b7c8d9e0f1",
  "path": "/sessions/67c0a1b2e4f5a6b7c8d9e0f1",
  "timestamp": "2026-02-27T14:30:00"
}
```

---

## 📊 Dashboard

Todos os endpoints de dashboard requerem `ROLE_DEVICE` ou `ROLE_FRONTEND`.

#### Headers (todos os endpoints abaixo)
```
X-API-KEY: <DEVICE_API_KEY ou FRONTEND_API_KEY>
```

---

### `GET /dashboard/summary`
Resumo geral de todas as sessões: totais, médias e streak atual.

#### Resposta — `200 OK`
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

| Campo                   | Descrição                                                          |
|-------------------------|--------------------------------------------------------------------|
| `totalSessions`         | Total de sessões registradas                                       |
| `successfulSessions`    | Sessões em que a meta de ciclos foi atingida                       |
| `successRate`           | Percentual de sucesso (%)                                          |
| `totalFocusMinutes`     | Total acumulado de minutos em foco                                 |
| `averageCompletedCycles`| Média de ciclos completados por sessão                             |
| `averageFocusMinutes`   | Média de minutos de foco por sessão                                |
| `currentStreak`         | Dias consecutivos com pelo menos uma sessão (estilo GitHub streak) |

---

### `GET /dashboard/overview`
Dados agrupados por período para gráfico de linha/barra principal.

#### Query Parameters

| Parâmetro   | Tipo     | Default | Descrição                                              |
|-------------|----------|---------|--------------------------------------------------------|
| `period`    | `string` | `week`  | `week` (últimos 7 dias), `month` (mês atual), `year` (ano atual), `all` (todos os tempos) |
| `startDate` | `string` | —       | Data início customizada (`YYYY-MM-DD`). Sobrescreve `period` |
| `endDate`   | `string` | —       | Data fim customizada (`YYYY-MM-DD`). Sobrescreve `period`   |

- `week` e `month` → dados agrupados **por dia**
- `year` e `all` → dados agrupados **por semana ISO** (se > 60 dias) ou **por dia**
- Se `startDate` e `endDate` forem informados, o `period` é ignorado

#### Exemplos de Request
```
GET /dashboard/overview?period=week
GET /dashboard/overview?period=all
GET /dashboard/overview?startDate=2025-07-01&endDate=2025-07-31
```

#### Resposta — `200 OK`
```json
{
  "period": "week",
  "totalFocusMinutes": 350,
  "totalSessions": 7,
  "successRate": 85.71,
  "data": [
    { "label": "21/02", "totalFocusMinutes": 50, "sessions": 1, "successRate": 100.0 },
    { "label": "22/02", "totalFocusMinutes": 0,  "sessions": 0, "successRate": 0.0  },
    { "label": "23/02", "totalFocusMinutes": 75, "sessions": 2, "successRate": 50.0 },
    { "label": "24/02", "totalFocusMinutes": 50, "sessions": 1, "successRate": 100.0 },
    { "label": "25/02", "totalFocusMinutes": 50, "sessions": 1, "successRate": 100.0 },
    { "label": "26/02", "totalFocusMinutes": 75, "sessions": 1, "successRate": 100.0 },
    { "label": "27/02", "totalFocusMinutes": 50, "sessions": 1, "successRate": 100.0 }
  ]
}
```

#### Erro — `400 Bad Request` (period inválido)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid period: daily. Use: week, month, year"
}
```

---

### `GET /dashboard/by-category`
Distribuição de minutos e sessões por categoria para gráfico de pizza/donut.

#### Query Parameters

| Parâmetro   | Tipo     | Default  | Descrição                                              |
|-------------|----------|----------|--------------------------------------------------------|
| `period`    | `string` | `month`  | `week`, `month`, `year` ou `all`                       |
| `startDate` | `string` | —        | Data início customizada (`YYYY-MM-DD`). Sobrescreve `period` |
| `endDate`   | `string` | —        | Data fim customizada (`YYYY-MM-DD`). Sobrescreve `period`   |

#### Exemplos de Request
```
GET /dashboard/by-category?period=month
GET /dashboard/by-category?period=all
GET /dashboard/by-category?startDate=2025-01-01&endDate=2025-12-31
```

#### Resposta — `200 OK`
```json
[
  {
    "category": "TECHNOLOGY",
    "totalFocusMinutes": 500,
    "sessions": 10,
    "successRate": 80.0,
    "percentage": 55.56
  },
  {
    "category": "MATH",
    "totalFocusMinutes": 250,
    "sessions": 5,
    "successRate": 60.0,
    "percentage": 27.78
  },
  {
    "category": "ENGLISH",
    "totalFocusMinutes": 150,
    "sessions": 3,
    "successRate": 100.0,
    "percentage": 16.67
  }
]
```

> Ordenado por `totalFocusMinutes` decrescente. `percentage` é a fatia do total de minutos do período.

---

### `GET /dashboard/heatmap`
Atividade diária do ano inteiro, estilo GitHub contribution graph.

#### Query Parameters

| Parâmetro | Tipo      | Default | Descrição                   |
|-----------|-----------|---------|-----------------------------|
| `year`    | `integer` | ano atual | Ano desejado (ex: `2025`)   |

#### Exemplo de Request
```
GET /dashboard/heatmap?year=2026
```

#### Resposta — `200 OK`
```json
[
  { "date": "2026-01-01", "totalMinutes": 0,  "sessions": 0 },
  { "date": "2026-01-02", "totalMinutes": 50, "sessions": 2 },
  { "date": "2026-01-03", "totalMinutes": 25, "sessions": 1 },
  "..."
]
```

> Retorna uma entrada para cada dia de `01/01/{year}` até hoje (ou `31/12/{year}` se o ano for passado). Dias sem sessão retornam `totalMinutes: 0, sessions: 0`.

---

### `GET /dashboard/by-period`
Estatísticas agrupadas por período do dia (manhã, tarde, noite) para todos os tempos.

#### Resposta — `200 OK`
```json
[
  {
    "period": "MORNING",
    "totalFocusMinutes": 800,
    "sessions": 16,
    "successRate": 75.0,
    "averageFocusMinutes": 50.0
  },
  {
    "period": "AFTERNOON",
    "totalFocusMinutes": 600,
    "sessions": 12,
    "successRate": 83.33,
    "averageFocusMinutes": 50.0
  },
  {
    "period": "NIGHT",
    "totalFocusMinutes": 200,
    "sessions": 4,
    "successRate": 50.0,
    "averageFocusMinutes": 50.0
  }
]
```

> Sempre retorna os 3 períodos, mesmo sem sessões (zerado).

---

### `GET /dashboard/by-weekday`
Estatísticas agrupadas por dia da semana para todos os tempos.

#### Resposta — `200 OK`
```json
[
  { "dayOfWeek": "MONDAY",    "totalFocusMinutes": 200, "sessions": 4, "successRate": 75.0,  "averageFocusMinutes": 50.0 },
  { "dayOfWeek": "TUESDAY",   "totalFocusMinutes": 150, "sessions": 3, "successRate": 66.67, "averageFocusMinutes": 50.0 },
  { "dayOfWeek": "WEDNESDAY", "totalFocusMinutes": 250, "sessions": 5, "successRate": 80.0,  "averageFocusMinutes": 50.0 },
  { "dayOfWeek": "THURSDAY",  "totalFocusMinutes": 100, "sessions": 2, "successRate": 100.0, "averageFocusMinutes": 50.0 },
  { "dayOfWeek": "FRIDAY",    "totalFocusMinutes": 300, "sessions": 6, "successRate": 83.33, "averageFocusMinutes": 50.0 },
  { "dayOfWeek": "SATURDAY",  "totalFocusMinutes": 0,   "sessions": 0, "successRate": 0.0,   "averageFocusMinutes": 0.0  },
  { "dayOfWeek": "SUNDAY",    "totalFocusMinutes": 0,   "sessions": 0, "successRate": 0.0,   "averageFocusMinutes": 0.0  }
]
```

> Sempre retorna os 7 dias na ordem `MONDAY → SUNDAY`, mesmo sem sessões.

---

### `GET /dashboard/goals`
Métricas de progresso no período: dias ativos, médias e taxa de sucesso.

#### Query Parameters

| Parâmetro   | Tipo     | Default | Descrição                                              |
|-------------|----------|---------|--------------------------------------------------------|
| `period`    | `string` | `week`  | `week`, `month`, `year` ou `all`                       |
| `startDate` | `string` | —       | Data início customizada (`YYYY-MM-DD`). Sobrescreve `period` |
| `endDate`   | `string` | —       | Data fim customizada (`YYYY-MM-DD`). Sobrescreve `period`   |

#### Exemplos de Request
```
GET /dashboard/goals?period=month
GET /dashboard/goals?period=all
GET /dashboard/goals?startDate=2025-07-01&endDate=2025-07-31
```

#### Resposta — `200 OK`
```json
{
  "period": "month",
  "totalFocusMinutes": 1200,
  "totalSessions": 24,
  "successfulSessions": 18,
  "successRate": 75.0,
  "averageFocusMinutesPerDay": 38.71,
  "activeDays": 20,
  "totalDaysInPeriod": 31
}
```

| Campo                     | Descrição                                              |
|---------------------------|--------------------------------------------------------|
| `totalFocusMinutes`       | Total de minutos focados no período                    |
| `totalSessions`           | Total de sessões no período                            |
| `successfulSessions`      | Sessões com sucesso no período                         |
| `successRate`             | Percentual de sucesso (%)                              |
| `averageFocusMinutesPerDay` | Média de minutos de foco por dia do período          |
| `activeDays`              | Dias com pelo menos uma sessão no período              |
| `totalDaysInPeriod`       | Total de dias do período (7 / dias do mês / dias do ano até hoje) |

---

## ⚡ Resumo de Endpoints

| Método | Endpoint                  | Role necessária               | Descrição                        |
|--------|---------------------------|-------------------------------|----------------------------------|
| `POST` | `/sessions`               | `DEVICE`                      | Registra nova sessão             |
| `GET`  | `/sessions`               | `DEVICE` ou `FRONTEND`        | Lista sessões paginadas          |
| `GET`  | `/sessions/{id}`          | `DEVICE` ou `FRONTEND`        | Busca sessão por ID              |
| `GET`  | `/dashboard/summary`      | `DEVICE` ou `FRONTEND`        | Resumo geral + streak            |
| `GET`  | `/dashboard/overview`     | `DEVICE` ou `FRONTEND`        | Dados por período (gráfico)      |
| `GET`  | `/dashboard/by-category`  | `DEVICE` ou `FRONTEND`        | Distribuição por categoria       |
| `GET`  | `/dashboard/heatmap`      | `DEVICE` ou `FRONTEND`        | Heatmap de atividade anual       |
| `GET`  | `/dashboard/by-period`    | `DEVICE` ou `FRONTEND`        | Stats por manhã/tarde/noite      |
| `GET`  | `/dashboard/by-weekday`   | `DEVICE` ou `FRONTEND`        | Stats por dia da semana          |
| `GET`  | `/dashboard/goals`        | `DEVICE` ou `FRONTEND`        | Metas e progresso no período     |

---

## 🌐 CORS

A API aceita requisições dos seguintes origins:

| Ambiente   | Origin                          |
|------------|---------------------------------|
| Dev        | `http://localhost:3000`         |
| Produção   | configurado via `CORS_ALLOWED_ORIGINS` |

Para adicionar o frontend deployado, configure a variável de ambiente:
```
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://seu-frontend.vercel.app
```

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
  "path": "/sessions",
  "timestamp": "2026-02-27T14:30:00",
  "fieldErrors": [
    {
      "field": "category",
      "rejectedValue": "SCIENCE",
      "message": "Invalid value 'SCIENCE' for field 'category'. Accepted values: [TECHNOLOGY, MATH, PORTUGUESE, ENGLISH, OTHER]"
    }
  ]
}
```

| Status | Situação                                          |
|--------|---------------------------------------------------|
| `201`  | Sessão criada com sucesso                         |
| `200`  | Requisição GET bem-sucedida                       |
| `400`  | Dados inválidos / parâmetros incorretos           |
| `401`  | API Key ausente ou inválida                       |
| `403`  | Chave sem permissão para o endpoint               |
| `404`  | Sessão não encontrada                             |
| `500`  | Erro interno inesperado                           |

