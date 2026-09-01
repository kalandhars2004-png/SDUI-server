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
