# SDUI Server — Spring Boot + GraphQL + MySQL (Strictly GraphQL)

Stores SDUI JSON strictly via GraphQL, MySQL behind. No REST.

## Run (Local MySQL, no Docker as requested)

- MySQL 8 `MySQL80` Running, `root/root`, DB `sdui` auto-created
```bash
cd D:\MONTH-2\week-5\Flutter\SDUI-Server
mvn spring-boot:run
# Tomcat 8080, GraphQL http://127.0.0.1:8080/graphql
# GraphiQL http://127.0.0.1:8080/graphiql
```

`application.yml`:
```yaml
spring.datasource.url: jdbc:mysql://127.0.0.1:3306/sdui?createDatabaseIfNotExist=true
username: root
password: root
jpa.hibernate.ddl-auto: update
graphql.graphiql.enabled: true
```

## Schema `src/main/resources/graphql/schema.graphqls`

```graphql
type SduiTemplate { id:ID! name:String! json:String! version:String createdAt:String updatedAt:String }
type Query { templates:[SduiTemplate!]! template(id:ID!):SduiTemplate }
type Mutation { saveTemplate(name:String!, json:String!):SduiTemplate! updateTemplate(id:ID!, name:String, json:String):SduiTemplate! deleteTemplate(id:ID!):Boolean! }
```

Entity `SduiTemplate` `LONGTEXT json` + `SduiTemplateService` validates `type` (like Flutter `SduiValidator`).

## Test via curl

```bash
curl -X POST http://127.0.0.1:8080/graphql -H "Content-Type: application/json" -d '{"query":"{templates{id name}}"}'
curl -X POST http://127.0.0.1:8080/graphql -H "Content-Type: application/json" -d '{"query":"mutation{saveTemplate(name:\"Home\", json:\"{\\\"type\\\":\\\"column\\\"}\"){id}}"}'
```

## Flutter Usage

`SduiGraphqlService` `apps/playground/lib/services/sdui_graphql_service.dart` (`graphql_flutter` HttpLink `http://127.0.0.1:8080/graphql`):
```dart
await service.saveTemplate(name: name, json: jsonString);
final list = await service.fetchTemplates();
await controller.loadFromJsonStringAsync(template.json, templateId: template.id);
```

Injected into big project as `D:\...\SDUI-Server` outside Flutter `D:\...\SDUI`.

## Build

```bash
mvn -DskipTests package
java -jar target/sdui-server-0.0.1-SNAPSHOT.jar
```
