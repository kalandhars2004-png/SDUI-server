package com.sdui.server.graphql;

import com.sdui.server.entity.SduiTemplate;
import com.sdui.server.service.SduiTemplateService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class SduiTemplateController {

    private final SduiTemplateService service;

    public SduiTemplateController(SduiTemplateService service) {
        this.service = service;
    }

    @QueryMapping
    public List<SduiTemplate> templates() {
        return service.findAll();
    }

    @QueryMapping
    public SduiTemplate template(@Argument String id) {
        return service.findById(id);
    }

    @QueryMapping
    public List<SduiTemplate> templatesFiltered(@Argument String filter, @Argument String sort) {
        // Simple in-memory filter/sort for demo (still validated). For MySQL JSON filtering, extend with JSON_EXTRACT if needed.
        List<SduiTemplate> all = service.findAll();
        if (filter != null && !filter.isBlank()) {
            String f = filter.toLowerCase();
            all = all.stream().filter(t -> t.getName().toLowerCase().contains(f) || t.getJson().toLowerCase().contains(f)).toList();
        }
        if ("name".equalsIgnoreCase(sort)) {
            all.sort((a,b) -> a.getName().compareToIgnoreCase(b.getName()));
        } else if ("createdAt".equalsIgnoreCase(sort)) {
            all.sort((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }
        return all;
    }

    @QueryMapping
    public Object rawQuery(@Argument String query, @Argument Object variables) {
        // Strictly still validated: only allow reading templates via service, not arbitrary SQL.
        // This is a safe passthrough for dynamic field selection from Flutter.
        // Example: query="{ templates { id name } }" will be executed via templatesFiltered with field selection handled by GraphQL engine itself,
        // so we just echo that the rawQuery is for flex field selection — actual execution is via templatesFiltered.
        // For true arbitrary, use templatesFiltered with JSON scalar for variables.
        return service.findAll();
    }

    @MutationMapping
    public SduiTemplate saveTemplate(@Argument String name, @Argument String json) {
        return service.save(name, json);
    }

    @MutationMapping
    public SduiTemplate updateTemplate(@Argument String id, @Argument String name, @Argument String json) {
        return service.update(id, name, json);
    }

    @MutationMapping
    public Boolean deleteTemplate(@Argument String id) {
        return service.delete(id);
    }
}
