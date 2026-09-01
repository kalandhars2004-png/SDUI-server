# SDUI Server — Spring Boot 3 + GraphQL + MySQL (Strictly GraphQL)

> Stores SDUI JSON strictly via GraphQL. MySQL is only a JSON store. Flutter `SduiEngine` stays generic.

This is the **outside server** for `D:\MONTH-2\week-5\Flutter\SDUI` (Flutter SDUI). As you asked, it lives at **`D:\MONTH-2\week-5\Flutter\SDUI-Server`** (outside current project).

---

## How It Works

```
[Flutter Manage JSON Screen]  ── GraphQL ──► [Spring Boot :8080/graphql] ── JPA ──► [MySQL sdui.sdui_template]
        │  saveTemplate(name, json)                         │  validate type
        │  templates {id name json}  ◄──────────────────────┘
        ▼
[Flutter Builder Main]  ← controller.loadFromJsonStringAsync(json, id) ← Load UI click
        │
        └─► SduiView(data: json, engine: AppzillonPlugin) → Widget
```

- **Flutter never talks to MySQL directly** — only `String json` via `POST http://127.0.0.1:8080/graphql`.
- **MySQL table** `sdui_template` is dumb store: `id VARCHAR(36) PK, name VARCHAR(200), json LONGTEXT, version VARCHAR(20), created_at, updated_at`.
- **Validation** ported from Flutter `SduiValidator` — Java checks `json` is valid JSON and `type` not blank before save.

---

## Run (Local MySQL, No Docker)

- **MySQL 8** service `MySQL80` **Running**, `root / root`, DB `sdui` auto-created (`createDatabaseIfNotExist=true`)
- **No Docker** as you requested — uses local `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe` + `127.0.0.1:3306`

```bash
cd D:\MONTH-2\week-5\Flutter\SDUI-Server
mvn spring-boot:run
# Tomcat started on port 8080
# GraphQL http://127.0.0.1:8080/graphql
# GraphiQL http://127.0.0.1:8080/graphiql
```

`src/main/resources/application.yml`:
```yaml
server.port: 8080
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/sdui?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
    username: root
    password: root
  jpa.hibernate.ddl-auto: update
  graphql.graphiql.enabled: true
```

---

## GraphQL Schema `src/main/resources/graphql/schema.graphqls` (Strictly GraphQL, No REST)

```graphql
type SduiTemplate {
  id: ID!
  name: String!
  json: String!
  version: String
  createdAt: String
  updatedAt: String
}
type Query {
  templates: [SduiTemplate!]!
  template(id: ID!): SduiTemplate
}
type Mutation {
  saveTemplate(name: String!, json: String!): SduiTemplate!
  updateTemplate(id: ID!, name: String, json: String): SduiTemplate!
  deleteTemplate(id: ID!): Boolean!
}
```

**Java:**
- `entity/SduiTemplate.java` — `@Entity @Table(sdui_template)` `json LONGTEXT`
- `repository/SduiTemplateRepository.java` — `JpaRepository<String>`
- `service/SduiTemplateService.java` — `validateJson()` via `ObjectMapper.readTree`, then `save/update/delete`
- `graphql/SduiTemplateController.java` — `@QueryMapping` `templates()` / `template(id)` and `@MutationMapping` `saveTemplate`, `updateTemplate`, `deleteTemplate`
- `config/CorsConfig.java` — `allowedOrigins "*"` for `http://127.0.0.1:8082` (Flutter web)

---

## Test via curl / GraphiQL

```bash
# List
curl -X POST http://127.0.0.1:8080/graphql -H "Content-Type: application/json" -d '{"query":"{ templates { id name version } }"}'

# Save
curl -X POST http://127.0.0.1:8080/graphql -H "Content-Type: application/json" -d '{"query":"mutation { saveTemplate(name:\"Home\", json:\"{\\\"type\\\":\\\"column\\\"}\") { id name } }"}'

# Get one
curl -X POST http://127.0.0.1:8080/graphql -H "Content-Type: application/json" -d '{"query":"{ template(id:\"<id>\") { id name json } }"}'
```

Or open `http://127.0.0.1:8080/graphiql` in browser.

---

## Flutter Usage (How it connects)

`apps/playground/lib/services/sdui_graphql_service.dart` (`graphql_flutter` `HttpLink http://127.0.0.1:8080/graphql`):

```dart
final gql = SduiGraphqlService.create(); // InMemoryStore, no Hive needed
await gql.saveTemplate(name: "Home", json: jsonString); // from Builder header Save to DB
final list = await gql.fetchTemplates(); // for Manage JSON list
await controller.loadFromJsonStringAsync(list[0].json, templateId: list[0].id); // auto-redirect to Builder tab
```

`apps/playground/lib/screens/manage_json_screen.dart` — paste/pick → `service.saveTemplate` → `FutureBuilder(service.fetchTemplates())` → `Load UI` → `controller.loadFromJsonStringAsync` → `onLoaded: setState(()=>_tab=0)` → Builder canvas shows it.

`apps/playground/lib/main.dart:31` — `engine.registerPlugin(AppzillonPlugin())` stays, `SduiView` renders same JSON.

---

## Build

```bash
cd D:\MONTH-2\week-5\Flutter\SDUI-Server
mvn -DskipTests package
java -jar target/sdui-server-0.0.1-SNAPSHOT.jar
# or mvn spring-boot:run
```

## Project Injection

Big project just adds `sdui_engine` as path/git dependency and does `engine.registerPlugin(AppzillonPlugin())` + `SduiView(data: jsonFromGraphQL, engine: engine)` — no copy-paste of server code. Server stays at `D:\...\SDUI-Server`.

## DB

```sql
SHOW TABLES; -- sdui_template
SELECT id, name, LEFT(json,80), version FROM sdui_template;
```

`json` is stored verbatim (wire `UiNode.toWireJson`), `version` extracted from JSON if present.

---

MIT — MySQL local, GraphQL strictly, no REST.
