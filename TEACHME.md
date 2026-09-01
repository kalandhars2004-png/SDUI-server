# TEACHME — SDUI Server (GraphQL + Spring Boot + MySQL) from Zero

> For: `D:\MONTH-2\week-5\Flutter\SDUI-Server` — strictly GraphQL, MySQL local `root/root`, outside Flutter `D:\...\SDUI`.

This file teaches **What is GraphQL?** → **How this server works** → **How Flutter talks to it**, step-by-step like classroom.

---

## 1. What is GraphQL? (In 2 Minutes)

**REST (old):** Many URLs, fixed data.
```
GET /templates           → returns id,name,json,version,createdAt (always all)
GET /templates/123       → returns one
POST /templates          → creates
```
If you need only `name`, you still get everything. If you need `name` + `json` + `version` from two tables, you call 2 URLs.

**GraphQL (new):** One URL, you ask exactly what you want.
```
POST http://127.0.0.1:8080/graphql
Body: { "query": "{ templates { id name } }" }  → returns only id+name
Body: { "query": "{ templates { id name json } }" } → returns id+name+json
```
- **One endpoint:** `http://127.0.0.1:8080/graphql` (POST)
- **You write the query** in the request, server returns exactly those fields.
- **No over-fetching, no under-fetching.**

**Three words in GraphQL:**
- `Query` = read (like SELECT)
- `Mutation` = write (like INSERT/UPDATE/DELETE)
- `Schema` = contract that says what you can query/mutate.

---

## 2. Our Schema (Contract) — `src/main/resources/graphql/schema.graphqls`

```graphql
type SduiTemplate {          # What a saved UI looks like in MySQL
  id: ID!                    # UUID, PK
  name: String!              # e.g., "HomeScreen v1"
  json: String!              # The whole SDUI JSON as String (LONGTEXT)
  version: String            # "1.0"
  createdAt: String
  updatedAt: String
}

type Query {                 # Read
  templates: [SduiTemplate!]!        # list all
  template(id: ID!): SduiTemplate    # get one
}

type Mutation {              # Write
  saveTemplate(name: String!, json: String!): SduiTemplate!
  updateTemplate(id: ID!, name: String, json: String): SduiTemplate!
  deleteTemplate(id: ID!): Boolean!
}
```

**Why `json: String!` and not decomposed fields?** SDUI JSON is already a tree (`type, props, style, children`). Storing it as `LONGTEXT` keeps Flutter's `UiNode.toWireJson()` verbatim. MySQL doesn't need to understand `props.fontSize` — it just stores the string. Validation happens in Java (`SduiTemplateService.validateJson` checks `type` not blank, like Flutter's `SduiValidator`).

---

## 3. How This Server Works — File by File

```
SDUI-Server/
├─ pom.xml
├─ src/main/resources/
│  ├─ application.yml          # Where MySQL + GraphQL port is configured
│  └─ graphql/schema.graphqls  # The contract above
└─ src/main/java/com/sdui/server/
   ├─ SduiServerApplication.java          # Main → SpringApplication.run()
   ├─ entity/SduiTemplate.java            # @Entity → maps to MySQL table sdui_template
   ├─ repository/SduiTemplateRepository.java # JpaRepository → SQL for free
   ├─ service/SduiTemplateService.java    # Business logic + validateJson()
   └─ graphql/SduiTemplateController.java # @QueryMapping / @MutationMapping → connects GraphQL to Service
```

### 3.1 `pom.xml` — What each dependency does

- `spring-boot-starter-web` — Tomcat on `:8080` to receive HTTP POST `/graphql`
- `spring-boot-starter-graphql` — Understands `schema.graphqls`, creates `/graphql` endpoint and `/graphiql` UI
- `spring-boot-starter-data-jpa` — Lets you write `repository.findAll()` instead of SQL
- `mysql-connector-j` — Driver to talk to MySQL `127.0.0.1:3306`
- `jackson-databind` — `ObjectMapper.readTree(json)` to validate JSON

### 3.2 `application.yml` — Where MySQL lives

```yaml
server.port: 8080
spring.datasource.url: jdbc:mysql://127.0.0.1:3306/sdui?createDatabaseIfNotExist=true
  username: root
  password: root
spring.jpa.hibernate.ddl-auto: update   # auto CREATE TABLE sdui_template if not exists
spring.graphql.graphiql.enabled: true   # GraphiQL at http://127.0.0.1:8080/graphiql
```

`MySQL80` service is **Running** (local, no Docker as you said). DB `sdui` + table `sdui_template` auto-created on first save.

### 3.3 `entity/SduiTemplate.java` — The MySQL Table

```java
@Entity @Table(name="sdui_template")
public class SduiTemplate {
  @Id String id;          // UUID
  String name;            // "Home"
  @Lob String json;       // LONGTEXT — whole SDUI JSON
  String version;         // "1.0"
  Instant createdAt, updatedAt; // @PrePersist, @PreUpdate
}
```
Maps to:
```sql
CREATE TABLE sdui_template (id VARCHAR(36) PK, name VARCHAR(200), json LONGTEXT, version VARCHAR(20), created_at DATETIME, updated_at DATETIME);
```

### 3.4 `repository/SduiTemplateRepository.java` — No SQL Written

```java
interface SduiTemplateRepository extends JpaRepository<SduiTemplate, String> {}
```
You get `findAll(), findById(id), save(entity), deleteById(id)` for free.

### 3.5 `service/SduiTemplateService.java` — The Brain

```java
validateJson(json) // ObjectMapper.readTree(json) → check type not blank
save(name, json)   // new SduiTemplate() → setName, setJson, setVersion(from json.version) → repository.save()
findAll()          // repository.findAll()
update(id,name,json) // findById → setName/setJson if not null → save()
```
**Why validate here?** Same rule as Flutter `SduiValidator` — ensures bad JSON never reaches MySQL.

### 3.6 `graphql/SduiTemplateController.java` — The Bridge

```java
@Controller
class SduiTemplateController {
  @QueryMapping List<SduiTemplate> templates() { return service.findAll(); }
  @MutationMapping SduiTemplate saveTemplate(@Argument String name, @Argument String json) { return service.save(name, json); }
}
```
Spring GraphQL auto-maps: GraphQL `saveTemplate(name:"Home", json:"{...}")` → Java method `saveTemplate(name, json)`.

---

## 4. How Flutter Talks to It

**Flutter** `apps/playground/lib/services/sdui_graphql_service.dart`:

```dart
final link = HttpLink('http://127.0.0.1:8080/graphql');
final client = GraphQLClient(link: link, cache: InMemoryStore());

// List
await client.query(QueryOptions(document: gql(r'{ templates { id name json } }')))

// Save
await client.mutate(MutationOptions(document: gql(r'mutation Save($name:String!,$json:String!){ saveTemplate(name:$name, json:$json){id name}}'), variables: {'name': name, 'json': jsonString}))

// Load into canvas
await controller.loadFromJsonStringAsync(template.json, templateId: template.id);
```

**Flow you use:**
1. **Manage JSON Screen** `screens/manage_json_screen.dart` — `TextField` paste/pick + `Name` → `Save to MySQL (GraphQL)` → `POST /graphql` `saveTemplate` → MySQL `INSERT`
2. **List** — `FutureBuilder(service.fetchTemplates())` → `POST /graphql` `templates` → MySQL `SELECT *` → shows `name`, `id 8chars`, `preview 80 chars`
3. **Load UI** → `controller.loadFromJsonStringAsync(json, templateId: id)` → `UiDocument.fromJson` → `BuilderCanvas` + `Preview SduiView` rebuild → auto `setState(()=>_tab=0)` redirects to Builder tab and shows UI
4. **After edit** — Builder header `Save to DB` (green) → if `loadedTemplateId != null` → `updateTemplate(id, name, json)` else `saveTemplate` → MySQL `UPDATE` or `INSERT`

**No REST, no direct MySQL from Flutter** — strictly `POST http://127.0.0.1:8080/graphql` with `{"query":"..."}`.

---

## 5. Try It Yourself (Local, No Docker)

**Start MySQL:** Already `MySQL80 Running` `root/root`

**Start Server:**
```bash
cd D:\MONTH-2\week-5\Flutter\SDUI-Server
mvn spring-boot:run
# or: java -jar target/sdui-server-0.0.1-SNAPSHOT.jar
# Tail: D:\MONTH-2\week-5\Flutter\SDUI-Server\server.log
```

**Test GraphQL (no Flutter needed):**
Open `http://127.0.0.1:8080/graphiql` → left pane:
```graphql
mutation { saveTemplate(name: "Test", json: "{\"type\":\"column\",\"children\":[]} ") { id name } }
```
Then:
```graphql
{ templates { id name version } }
```

**Flutter:** `cd D:\MONTH-2\week-5\Flutter\SDUI\apps\playground && flutter run -d web-server --web-port=8082` → `http://127.0.0.1:8082` → 4th tab `Manage JSON` → `Save` → `Load UI`.

---

## 6. Why This Design?

- **D:\...\SDUI-Server outside D:\...\SDUI** as you asked — Flutter stays injectable (`engine.registerPlugin(AppzillonPlugin())` + `registerComponent`), server is separate repo `https://github.com/kalandhars2004-png/SDUI-server.git`.
- **API-level only:** Flutter never sees MySQL, only `String json` via GraphQL — matches your `just API level Is ok`.
- **Strictly GraphQL:** No `RestController`, no `GET /api/templates` — only `POST /graphql`.
- **MySQL local:** `createDatabaseIfNotExist=true`, `LONGTEXT` for any size SDUI JSON, `update` keeps history via `updatedAt`.

---

## 7. Next Steps to Learn

1. Open `schema.graphqls` → change `SduiTemplate` add `description: String` → restart server → GraphiQL shows new field.
2. Add `searchTemplates(nameContains: String): [SduiTemplate]` to `Query` → implement `repository.findByNameContaining` → Flutter `service.search(name)`.
3. Add `Auth` — `GraphQL/WebSocket` with `Authorization: Bearer <token>` header in `HttpLink`.

Happy coding daa!
