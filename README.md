# foody-api

API do **Foody**, um sistema de delivery para um restaurante: o cliente monta a sacola, faz o pedido
e acompanha cada etapa; a equipe toca os pedidos num quadro, decide pedidos de cancelamento, cuida do
cardápio e acompanha um dashboard; o administrador gerencia os usuários.

Este repositório é o **backend**. A interface web fica no repositório irmão **`foody-app`**. Cada um
roda sozinho — esta API se explora pelo Swagger UI ou por `curl` —, mas a experiência completa é com
os dois no ar: suba esta API primeiro e depois siga o README do `foody-app`.

| | |
|---|---|
| Linguagem e framework | Java 21 · Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation) |
| Banco | PostgreSQL 18 · migrations Flyway |
| Autenticação | JWT RS256 de 15 minutos + refresh token rotativo em cookie `HttpOnly` |
| Testes | JUnit · Testcontainers (PostgreSQL real) · ArchUnit · Datafaker |
| Contrato | OpenAPI gerado pelos testes em [`docs/openapi.json`](./docs/openapi.json) |
| Empacotamento | Imagem Docker com JRE enxuto (jlink) sobre Alpine |

## Sumário

1. [Início rápido](#início-rápido)
2. [Contas de demonstração](#contas-de-demonstração)
3. [Roteiro sugerido (5 min)](#roteiro-sugerido-5-min)
4. [Para desenvolver](#para-desenvolver)
5. [Testes e qualidade](#testes-e-qualidade)
6. [Produção](#produção)
7. [A API em resumo](#a-api-em-resumo)
8. [Organização do código](#organização-do-código)
9. [Variáveis de ambiente](#variáveis-de-ambiente)
10. [Solução de problemas](#solução-de-problemas)

---

## Início rápido

### Pré-requisitos

- **Git**.
- **Docker em execução**: Docker Desktop no Windows e no macOS; no Linux, Docker Engine com o plugin
  Compose v2. Confira com `docker compose version`.
- **Porta 8080 livre** — é a única que a demonstração ocupa no seu computador. O PostgreSQL roda só
  dentro da rede do Docker, sem porta publicada: um PostgreSQL local na 5432 não atrapalha.

Não precisa de JDK, Maven, Node, bash nem de arquivo `.env`.

### Subir

```bash
git clone https://github.com/diegoliveiraa/foody-api.git foody-api
cd foody-api
docker compose up -d --build --wait
```

Os comandos são os mesmos no PowerShell, no Prompt de Comando, no Git Bash e nos terminais do macOS e
do Linux. O `docker compose up` sobe três serviços:

1. **`keys`** gera o par de chaves RSA que assina os tokens, num volume do Docker (só na primeira vez),
   e termina;
2. **`postgres`** sobe o PostgreSQL 18 e espera ficar saudável;
3. **`api`** é compilada a partir deste código, dentro do Docker, pelo
   [`Dockerfile.demo`](./Dockerfile.demo); ao subir, aplica as migrations, cria
   os dados de demonstração e só então se declara saudável — o `--wait` só devolve o terminal nessa
   hora.

**Quanto demora:** na primeira vez, **de 2 a 6 minutos**, conforme a rede e a máquina — o Docker baixa
cerca de 600 MB (380 MB de imagens base e 200 MB de dependências Maven) e compila o projeto. Com tudo
já baixado, a subida do zero leva pouco mais de um minuto; das próximas vezes, segundos.

### Como saber que está pronto

- O comando terminou sem erro, com `Container foody-demo-api-1 Healthy`.
- `docker compose ps` mostra a API saudável:

  ```text
  SERVICE    STATUS                    PORTS
  api        Up 1 minute (healthy)     0.0.0.0:8080->8080/tcp
  postgres   Up 1 minute (healthy)     5432/tcp
  ```

  O `keys` só aparece em `docker compose ps -a`, como `Exited (0)` — é o esperado.
- <http://localhost:8080/actuator/health> responde `{"groups":["liveness","readiness"],"status":"UP"}`.
- **Swagger UI:** <http://localhost:8080/swagger-ui.html>. O contrato completo está em
  [`docs/openapi.json`](./docs/openapi.json).

### Parar, recomeçar e mudar o volume de dados

| Para… | Comando |
|---|---|
| parar (os dados ficam guardados) | `docker compose down` |
| subir de novo | `docker compose up -d --wait` |
| apagar banco e chaves e recomeçar do zero (o seed roda de novo) | `docker compose down -v` e depois `docker compose up -d --build --wait` |
| ver os logs da API | `docker compose logs -f api` |
| mais dados (precisa do banco vazio) | `docker compose down -v` e depois `SEED_SCALE=LARGE docker compose up -d --build --wait` |
| usar outra porta | `API_PORT=8081 docker compose up -d --build --wait` |

No PowerShell, variáveis antes do comando são escritas assim:

```powershell
$env:SEED_SCALE = "LARGE"; docker compose up -d --build --wait; Remove-Item Env:SEED_SCALE
```

- O seed só roda com o banco **vazio**: mudar `SEED_SCALE` sem `down -v` não muda nada.
- Os "pedidos de agora" são criados relativos à hora em que o seed rodou. Horas depois eles já parecem
  antigos — `docker compose down -v` e `up` de novo os trazem para o presente.
- Se existir um `.env` nesta pasta, os valores dele substituem os padrões da demonstração (contas,
  senhas, escala, porta). Veja [Variáveis de ambiente](#variáveis-de-ambiente).

---

## Contas de demonstração

| Papel | E-mail | Senha | O que experimentar |
|---|---|---|---|
| Cliente | `cliente@foody.local` | `Foody#2026` | cardápio, cotação da sacola, fazer um pedido e acompanhar; cancelar direto o pedido **Recebido**, pedir o cancelamento do **Em preparo** |
| Equipe | `equipe@foody.local` | `Foody#2026` | quadro de pedidos (avançar status, cancelar), decidir as 2 solicitações de cancelamento pendentes, histórico com filtros, dashboard, catálogo |
| Administrador | `admin@foody.local` | `Foody#2026` | tudo o que a equipe faz, mais a gestão de usuários (busca, troca de papel, desabilitar) |

Outros 4 membros da equipe e 50 clientes são gerados com nome e e-mail coerentes
(`eduarda.rios.4@yahoo.com.br`, por exemplo), todos com a senha `Foody#2026`. Novos clientes também se
cadastram por `POST /auth/register`; a equipe nasce de um cliente promovido pelo administrador.

Essas credenciais são públicas de propósito e valem **só para esta demonstração local**
([ADR-0028](./docs/adr/0028-one-command-runs-a-seeded-demo-stack-separate-from-the-production-image.md)).
A configuração de produção não tem nenhum valor padrão para senhas.

### O que o seed cria (escala `SMALL`, a padrão)

- **Usuários:** 57 — 1 administrador, 5 da equipe e 51 clientes (2 desabilitados).
- **Catálogo:** 8 categorias (Lanches, Pizzas, Pratos, Saladas, Porções, Bebidas, Sobremesas, Combos)
  e 50 produtos, 5 deles desabilitados (10%) — o cardápio do cliente mostra 45.
- **Pedidos:** 100, espalhados pelos últimos 60 dias com picos no almoço e no jantar. Os antigos estão
  quase todos entregues (cerca de 85%) e os demais cancelados pelo cliente ou pelo restaurante em
  etapas diferentes, sempre com o histórico de status em horários coerentes.
- **Pedidos ativos agora:** 2 recebidos, 4 em preparo — 2 deles com **solicitação de cancelamento
  pendente** — e 2 saindo para entrega, além de 2 entregues nas últimas duas horas. O quadro e o
  dashboard já começam com movimento.
- **O cliente fixo** (`cliente@foody.local`) tem 5 pedidos: um **Recebido** (pode cancelar direto), um
  **Em preparo** sem solicitação (pode pedir o cancelamento) e 3 antigos.
- **Solicitações já decididas:** 2 aprovadas (o pedido foi cancelado) e 2 recusadas (o pedido seguiu e
  foi entregue).
- **Ontem:** 4 pedidos antes da hora atual, para o dashboard comparar "hoje × ontem até agora".
- Os dados vêm de um gerador com semente fixa: nomes, produtos e preços são os mesmos em toda máquina;
  só os horários acompanham o relógio.

| Escala | Multiplicador | Pedidos | Produtos | Usuários |
|---|---|---|---|---|
| `SMALL` (padrão) | ×1 | 100 | 50 | 57 |
| `MEDIUM` | ×10 | 1.000 | 500 | 507 |
| `LARGE` | ×100 | 10.000 | 5.000 | 5.007 |

Em `MEDIUM` e `LARGE` os pedidos ativos param em 3 por situação, para o quadro continuar legível.
`MEDIUM` sobe em segundos. `LARGE` serve para testar volume e leva **cerca de 15 minutos** para semear
(medido numa máquina de 12 núcleos; o seed usa um só) — o `--wait` espera até 30 minutos, e
`docker compose logs -f api` mostra o andamento.

---

## Roteiro sugerido (5 min)

Dá para fazer tudo pelo **Swagger UI** (<http://localhost:8080/swagger-ui.html>): rode
`POST /auth/login`, copie o `accessToken`, clique em **Authorize** e experimente os endpoints. Abaixo,
o mesmo roteiro pelo terminal, só com `curl` e `sed` — no Windows use o **Git Bash**, que vem com o Git
(no PowerShell, `curl` é outro comando). Com o `jq` instalado, acrescente `| jq` para ver o JSON
formatado.

### 0. Preparar

```bash
API=http://localhost:8080
login() {
  curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"Foody#2026\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'
}
```

### 1. Como cliente: cardápio, sacola e pedido

```bash
CLIENTE=$(login cliente@foody.local)
curl -s "$API/me" -H "Authorization: Bearer $CLIENTE"
```

O `/me` devolve o usuário e a **navegação permitida**: o módulo `store`, com as telas `store.menu`
(ação `PLACE_ORDER`) e `store.my-orders` (`CANCEL`, `REQUEST_CANCELLATION`). O front monta menus e
botões só a partir disso; o papel nunca é exposto.

```bash
curl -s "$API/menu" -H "Authorization: Bearer $CLIENTE"
PRODUTO=$(curl -s "$API/menu" -H "Authorization: Bearer $CLIENTE" | sed -E 's/.*"products":\[\{"id":"([^"]+)".*/\1/')
curl -s -X POST "$API/menu/quote" -H "Authorization: Bearer $CLIENTE" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"productId\":\"$PRODUTO\",\"quantity\":2}]}"
```

A cotação devolve preço unitário, subtotal e total calculados no servidor — a sacola guarda só ids e
quantidades.

```bash
PEDIDO=$(curl -s -X POST "$API/my-orders" -H "Authorization: Bearer $CLIENTE" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"productId\":\"$PRODUTO\",\"quantity\":2}],\"deliveryAddress\":{\"zipCode\":\"01310-100\",\"street\":\"Avenida Paulista\",\"number\":\"1000\",\"neighborhood\":\"Bela Vista\",\"city\":\"Sao Paulo\",\"state\":\"SP\"}}" \
  | sed -E 's/^\{"id":"([^"]+)".*/\1/')
echo "$PEDIDO"
curl -s "$API/my-orders" -H "Authorization: Bearer $CLIENTE"
```

O pedido nasce `RECEIVED`. A lista mostra também os pedidos que o seed deu ao cliente fixo, entre eles
um Recebido e um Em preparo.

### 2. Como equipe: quadro, status, cancelamento e dashboard

```bash
EQUIPE=$(login equipe@foody.local)
curl -s "$API/orders?status=RECEIVED&status=PREPARING" -H "Authorization: Bearer $EQUIPE"
for STATUS in PREPARING OUT_FOR_DELIVERY DELIVERED; do
  curl -s -o /dev/null -w "$STATUS -> %{http_code}\n" -X PATCH "$API/orders/$PEDIDO/status" \
    -H "Authorization: Bearer $EQUIPE" -H 'Content-Type: application/json' -d "{\"status\":\"$STATUS\"}"
done
curl -s "$API/my-orders/$PEDIDO" -H "Authorization: Bearer $CLIENTE"
```

Cada item do quadro já traz `allowedTransitions`, `canCancel` e `hasPendingCancellationRequest`. As
três mudanças respondem `200`, e o cliente vê o histórico com cada etapa. Uma transição inválida é
recusada com um Problem Details:

```bash
curl -s -X PATCH "$API/orders/$PEDIDO/status" -H "Authorization: Bearer $EQUIPE" \
  -H 'Content-Type: application/json' -d '{"status":"PREPARING"}'
```

```json
{"detail":"O pedido não pode ir para esse status agora","instance":"/orders/…/status","status":422,"title":"Unprocessable Content","code":"INVALID_STATUS_TRANSITION","requestId":"…"}
```

Aprovar uma das solicitações de cancelamento pendentes (o dashboard lista os pedidos):

```bash
SOLICITACAO=$(curl -s "$API/dashboard" -H "Authorization: Bearer $EQUIPE" | grep -o '"orderId":"[^"]*"' | head -1 | cut -d'"' -f4)
curl -s -X POST "$API/orders/$SOLICITACAO/cancellation-request/approve" -H "Authorization: Bearer $EQUIPE" \
  -H 'Content-Type: application/json' -d '{}'
```

Para recusar, use `/decline` com `{"note":"Ja saiu para entrega"}`. O seed traz duas solicitações
pendentes; decididas as duas, `docker compose down -v` e um novo `up` as recriam. E o dashboard:

```bash
curl -s "$API/dashboard?period=TODAY" -H "Authorization: Bearer $EQUIPE"
```

Pedidos de hoje contra ontem até a mesma hora, faturamento, ticket médio, série por hora, produtos mais
vendidos, tempo médio de cada etapa, cancelamentos por motivo, pedidos ativos por status e solicitações
pendentes — calculados na hora, no fuso do restaurante. Troque por `LAST_7_DAYS` ou `LAST_30_DAYS`.

### 3. Como administrador: usuários

```bash
ADMIN=$(login admin@foody.local)
curl -s "$API/admin/users?role=STAFF" -H "Authorization: Bearer $ADMIN"
curl -s "$API/admin/users?search=joao" -H "Authorization: Bearer $ADMIN"
curl -s -o /dev/null -w '%{http_code}\n' "$API/menu" -H "Authorization: Bearer $ADMIN"
```

A busca ignora acentos e maiúsculas ("joao" encontra "João"); a lista é paginada (`meta.totalItems`).
O `/menu` responde `403`: o cardápio é uma tela do cliente, e cada endpoint confere a permissão da tela.

### 4. Pela interface

O roteiro visual — vitrine, sacola, acompanhamento ao vivo, quadro da equipe, dashboard com gráficos e
gestão de usuários — está no README do **`foody-app`**. Com esta API no ar, ele sobe com o mesmo
`docker compose up -d --build --wait` e abre em <http://localhost:5173>, com as mesmas contas.

---

## Para desenvolver

Precisa de **JDK 21** (Temurin recomendado) e do Docker. O Maven vem pelo wrapper (`./mvnw`; no
PowerShell, `.\mvnw.cmd`). No Windows, rode os scripts `bash` no **Git Bash**.

```bash
cp .env.example .env
bash scripts/generate-jwt-keys.sh
./mvnw spring-boot:test-run
```

1. No `.env`, preencha `POSTGRES_PASSWORD` e as variáveis `SEED_*` (senhas com 8 caracteres ou mais).
   Pode usar os mesmos valores da demonstração:

   ```dotenv
   POSTGRES_PASSWORD=foody-dev
   SEED_ADMIN_EMAIL=admin@foody.local
   SEED_ADMIN_PASSWORD=Foody#2026
   SEED_STAFF_EMAIL=equipe@foody.local
   SEED_CUSTOMER_EMAIL=cliente@foody.local
   SEED_DEFAULT_PASSWORD=Foody#2026
   ```

2. `generate-jwt-keys.sh` grava `.secrets/jwt-private.pem` e `.secrets/jwt-public.pem` (fora do Git) e
   se recusa a sobrescrevê-las sem `--force`.
3. `./mvnw spring-boot:test-run` sobe o **banco de desenvolvimento** do
   [`compose.dev.yaml`](./compose.dev.yaml) — projeto `foody-local`, PostgreSQL em `127.0.0.1:5432`,
   volume `foody-local_postgres-data` — pelo suporte a Docker Compose do Spring Boot, roda o seed e
   deixa a API em <http://localhost:8080>, com Swagger UI e recarga automática (DevTools).

Outras formas:

| Para… | Comando |
|---|---|
| a API sem seed (banco vazio na primeira vez) | `./mvnw spring-boot:run` |
| seed com volume | `./mvnw spring-boot:test-run "-Dspring-boot.run.arguments=--foody.seed.scale=LARGE"` |
| apagar o banco de desenvolvimento | `docker compose -f compose.dev.yaml down -v` |

- A demonstração e a API de desenvolvimento usam a porta 8080: rode `docker compose down` antes do
  `test-run`. Os bancos são separados (`foody-demo` e `foody-local`).
- `docker compose` sem `-f` age sobre a demonstração; para o banco de desenvolvimento, use
  `-f compose.dev.yaml`.
- O mesmo `.env` também vale para a demonstração: preenchido, os valores dele substituem os padrões.
- As regras de código do projeto estão em [`CLAUDE.md`](./CLAUDE.md).

---

## Testes e qualidade

```bash
./mvnw verify
bash scripts/check-comments.sh
```

- `./mvnw verify` confere a formatação, compila e roda os **550 testes** — unidade, integração contra
  PostgreSQL real em Testcontainers (nada de H2 ou repositório falso), regras de arquitetura com
  ArchUnit e o teste que regenera `docs/openapi.json`. Precisa do **Docker rodando** e leva de 3 a 5
  minutos. Se acusar formatação, `./mvnw spring-javaformat:apply` corrige.
- `scripts/check-comments.sh` garante que o código Java não tem comentários (regra R14).
- A CI ([`.github/workflows/ci.yml`](./.github/workflows/ci.yml)) roda três jobs:
  - **verify**: `./mvnw verify`, o `docs/openapi.json` commitado igual ao gerado e a checagem de
    comentários;
  - **image**: constrói a imagem de produção, sobe o `compose.deploy.yaml` até ficar saudável e
    cadastra um cliente para ler o `/me`;
  - **demo**: roda o `docker compose up -d --build --wait` deste README, entra como o administrador da
    demonstração e espera `200` em `GET /me` e `GET /dashboard`.

---

## Produção

[`compose.deploy.yaml`](./compose.deploy.yaml) sobe a **imagem de produção** e um PostgreSQL próprio
(projeto `foody`), construída pelo [`Dockerfile`](./Dockerfile): JRE recortado com jlink sobre
Alpine (229 MB), usuário sem root, sem ferramentas de build, **sem seed e sem Swagger** — o código de
seed é código de teste e nunca entra nela
([ADR-0007](./docs/adr/0007-seed-local-data-with-factories-and-seeders-on-the-test-classpath.md)). A
demonstração tem o seu próprio arquivo, o [`Dockerfile.demo`](./Dockerfile.demo), e o `.dockerignore`
da produção nem deixa o código de teste entrar no build.

```bash
cp .env.example .env
bash scripts/generate-jwt-keys.sh
docker compose -f compose.deploy.yaml up -d --build --wait
```

- **Nada tem padrão para segredo:** sem `POSTGRES_PASSWORD` no `.env` o compose se recusa a subir, e as
  chaves vêm de `.secrets/` (ou de onde `JWT_PRIVATE_KEY_FILE` e `JWT_PUBLIC_KEY_FILE` apontarem),
  montadas como *compose secrets*.
- O banco começa vazio: não há administrador nem dados; clientes se cadastram por `POST /auth/register`.
- **Limites de requisição reais:** a 6ª tentativa de login com o mesmo e-mail, do mesmo IP, dentro de um
  minuto recebe `429` com `Retry-After` (veja a [tabela](#limites-de-requisição)).

  ```bash
  for i in 1 2 3 4 5 6; do curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/auth/login \
    -H 'Content-Type: application/json' -d '{"email":"alguem@foody.local","password":"senha-errada"}'; done
  ```

- **CORS:** `FOODY_WEB_ALLOWEDORIGINS` lista as origens do front (padrão `http://localhost:5173`).
- Banco sem porta publicada, sistema de arquivos só leitura, `no-new-privileges`, reinício automático,
  limites de memória (`API_MEMORY_LIMIT`, 512 MB; o heap usa 50%) e logs em JSON (ECS).
- A 8080 é a mesma da demonstração: rode `docker compose down` antes, ou mude `API_PORT`.
- Para derrubar: `docker compose -f compose.deploy.yaml down`.

---

## A API em resumo

### Autenticação

1. `POST /auth/register` ou `POST /auth/login` devolvem `{"accessToken": "…", "expiresIn": 900}` e o
   cookie `foody_refresh` (`HttpOnly; Secure; SameSite=Strict; Path=/auth`, 7 dias).
2. As chamadas autenticadas mandam `Authorization: Bearer <accessToken>`; o token é um JWT RS256 de 15
   minutos.
3. `POST /auth/refresh` troca o cookie por um novo par — o refresh é rotativo, e reusar um antigo
   revoga a família inteira. `POST /auth/logout` revoga a sessão.
4. `GET /me` devolve o usuário e os módulos, telas e ações que ele pode usar.

### Endpoints

| Quem | Recurso | Operações |
|---|---|---|
| Qualquer um | `/auth` | `POST register`, `login`, `refresh`, `logout` |
| Qualquer um | `/actuator/health` | saúde da API |
| Autenticado | `/me` | `GET` usuário e navegação |
| Cliente | `/menu` | `GET` cardápio ativo; `POST /menu/quote` cota a sacola |
| Cliente | `/my-orders` | `POST` faz o pedido; `GET` lista e detalha; `POST /{id}/cancel` (recebido) e `POST /{id}/cancellation-request` (em preparo) |
| Equipe | `/orders` | `GET` quadro e histórico com filtros (`status`, `from`, `to`, `search`); `GET /{id}`; `PATCH /{id}/status`; `POST /{id}/cancel`; `POST /{id}/cancellation-request/approve` e `/decline` |
| Equipe | `/catalog/categories`, `/catalog/products` | `GET`, `POST`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/disable` e `/enable` |
| Equipe | `/dashboard` | `GET ?period=TODAY` (padrão), `LAST_7_DAYS` ou `LAST_30_DAYS` |
| Administrador | `/admin/users` | `GET` com filtros (`search`, `role`, `status`); `PATCH /{id}/role`; `POST /{id}/disable` e `/enable` |

O administrador tem todas as telas da equipe. O contrato completo, com cada campo, está no
[`docs/openapi.json`](./docs/openapi.json) e no Swagger UI da demonstração.

### Convenções

- **Erros** em Problem Details (`application/problem+json`) com `code` estável, `detail` em português
  e, na validação, `errors[]` por campo. Toda resposta traz o cabeçalho `X-Request-Id`, e todo erro, o
  `requestId` no corpo — o mesmo id das linhas de log daquela requisição.
- **Listas paginadas no servidor:** `page` (a partir de 1), `pageSize` (10, 25, 50 ou 100) e
  `sort=campo,asc|desc`, só nos campos permitidos; a resposta é `{"data": [...], "meta": {...}}`.
- **Dinheiro só na API:** preços, subtotais e totais são calculados no servidor, em números decimais; o
  pedido guarda uma cópia dos itens e do endereço.
- **Busca** ignora acentos e maiúsculas ("joao" acha "João", "acai" acha "Açaí"), com índices trigram;
  nomes são ordenados em português.
- **Horários** saem em UTC (ISO-8601); o dashboard agrupa no fuso do restaurante, `America/Sao_Paulo`.

### Limites de requisição

| Rota | Chave | Limite |
|---|---|---|
| `POST /auth/login` | IP · e-mail + IP | 20/min · 5/min |
| `POST /auth/register` | IP | 5/h |
| `POST /auth/refresh` | IP | 30/min |
| `POST /my-orders` | usuário | 10/min |
| `POST /my-orders/{id}/cancellation-request` | usuário | 5/min |
| qualquer outra, autenticado · anônimo | usuário · IP | 300/min · 60/min |

Ao estourar: `429` com `Retry-After`. O `/actuator/health` não tem limite. A demonstração e o
`test-run` elevam todos os limites, para a exploração e os testes E2E nunca esbarrarem neles; para
vê-los, use a [produção](#produção).

---

## Organização do código

```text
src/main/java/com/foody/api/
├── auth/  user/  me/      identidade, sessões e navegação
├── catalog/               categorias, produtos, cardápio e cotação da sacola
├── order/                 pedidos, ciclo de vida e solicitações de cancelamento
├── dashboard/             indicadores
└── shared/                segurança, erros, paginação, limites, persistência, utilidades
src/main/resources/db/migration/   migrations do Flyway
src/test/java/com/foody/seed/      factories e seeders (dados de demonstração)
docs/adr/                          decisões de arquitetura
```

- Um pacote por funcionalidade, com `controller → service → repository` separados; o ArchUnit barra
  camadas puladas, ciclos entre pacotes, controllers sem permissão declarada e tratamento de erro fora
  do lugar.
- As **decisões de arquitetura** estão em [`docs/adr`](./docs/adr/README.md), cada uma com o porquê,
  as alternativas descartadas e os contras — por exemplo, o ciclo de vida do pedido
  ([ADR-0024](./docs/adr/0024-drive-the-order-lifecycle-through-one-policy-per-side-endpoints-and-a-single-cancellation-request.md)),
  o dashboard calculado sob demanda
  ([ADR-0025](./docs/adr/0025-compute-dashboard-kpis-on-demand-in-the-restaurants-time-zone.md)), a
  busca sem acento
  ([ADR-0027](./docs/adr/0027-search-names-ignoring-accents-with-unaccent-and-trigram-indexes.md)) e
  esta demonstração em um comando
  ([ADR-0028](./docs/adr/0028-one-command-runs-a-seeded-demo-stack-separate-from-the-production-image.md)).
- As **regras de código** — e por que existem — estão em [`CLAUDE.md`](./CLAUDE.md).

---

## Variáveis de ambiente

Todas são opcionais na demonstração: valem do `.env` desta pasta ou do ambiente do terminal, e um valor
vazio usa o padrão. O [`.env.example`](./.env.example) lista todas.

| Variável | Demonstração (padrão) | `test-run` | Produção | Para quê |
|---|---|---|---|---|
| `POSTGRES_PASSWORD` | `foody-demo` | obrigatória | obrigatória | senha do PostgreSQL |
| `API_PORT` | `8080` | — | `8080` | porta publicada da API |
| `FOODY_WEB_ALLOWEDORIGINS` | `http://localhost:5173,http://localhost:4174` | — | `http://localhost:5173` | origens do front aceitas no CORS |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | `admin@foody.local` / `Foody#2026` | obrigatórias | — | conta de administrador |
| `SEED_STAFF_EMAIL` | `equipe@foody.local` | obrigatória | — | conta fixa da equipe |
| `SEED_CUSTOMER_EMAIL` | `cliente@foody.local` | obrigatória | — | conta fixa de cliente |
| `SEED_DEFAULT_PASSWORD` | `Foody#2026` | obrigatória | — | senha de todas as contas, menos o admin |
| `SEED_SCALE` | `SMALL` | — | — | volume do seed (`SMALL`, `MEDIUM`, `LARGE`) |
| `JWT_PRIVATE_KEY_FILE` / `JWT_PUBLIC_KEY_FILE` | — | — | `./.secrets/jwt-*.pem` | chaves que assinam os tokens |
| `API_MEMORY_LIMIT` / `POSTGRES_MEMORY_LIMIT` | — | — | `512M` | limite de memória dos containers |
| `FOODY_API_IMAGE` | — | — | `foody-api:latest` | nome da imagem gerada |

Na demonstração as chaves são geradas num volume; no `test-run`, vêm de `.secrets/` (gere com o script);
a escala do `test-run` se escolhe com `--foody.seed.scale`. Sem as variáveis `SEED_*`, o `test-run` se
recusa a subir e diz qual falta.

---

## Solução de problemas

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `Cannot connect to the Docker daemon`, `error during connect` ou `dockerDesktopLinuxEngine` | Docker parado | abra o Docker Desktop e espere o "Engine running" |
| `ports are not available` / `port is already allocated` na 8080 | outra API (`test-run`, produção) ou outro programa na porta | pare o outro processo, ou `API_PORT=8081 docker compose up -d --build --wait` e use <http://localhost:8081>. Para achar quem usa a porta: `netstat -ano \| findstr :8080` (Windows) ou `lsof -i :8080` |
| A primeira subida parece parada em `mvnw` | o Maven baixando dependências | é normal na primeira vez; acompanhe com `docker compose build --progress=plain` |
| Com `SEED_SCALE=LARGE` o comando fica muitos minutos em `Waiting` | o seed grande ainda rodando (cerca de 15 min) | espere; `docker compose logs -f api` mostra `Seed OrderSeeder done` ao terminar. Para volume sem a espera, use `MEDIUM` |
| `container foody-demo-api-1 is unhealthy` | a API não ficou saudável em 30 minutos | veja `docker compose logs api`; com o banco já semeado, `docker compose up -d --wait` tenta de novo |
| Os dados parecem velhos, ou mudar `SEED_SCALE` não teve efeito | o seed só roda com o banco vazio | `docker compose down -v` e `docker compose up -d --build --wait` |
| O login com `Foody#2026` falha | um `.env` nesta pasta trocou as contas | confira o `.env` (ou apague-o) e rode `docker compose down -v` antes de subir de novo — as contas só são criadas uma vez |
| `keys` falha ao instalar o OpenSSL | sem internet ou com proxy | a primeira subida precisa de rede; confira a conexão do Docker |
| `curl` no PowerShell pede parâmetros ou devolve um objeto | no Windows PowerShell, `curl` é apelido de `Invoke-WebRequest` | use o Git Bash, ou `curl.exe` |
| `400` com `MALFORMED_REQUEST` num `curl` com acento | o terminal do Windows não mandou o texto em UTF-8 | use texto sem acento no corpo, ou o Swagger UI |
| `\r: command not found` ou `bad interpreter` num script | quebra de linha do Windows | o `.gitattributes` já mantém `mvnw` e `scripts/*.sh` com LF; rode-os no Git Bash. A demonstração não depende disso |
| Mac com Apple Silicon | — | todas as imagens têm versão arm64 nativa; nada a ajustar |
| O front mostra erro de conexão ou de CORS | API fora do ar, ou o front aberto em outra origem | confira o health e abra o front exatamente em <http://localhost:5173> |
