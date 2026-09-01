package com.sdui.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdui.server.entity.SduiTemplate;
import com.sdui.server.repository.SduiTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SduiTemplateService {

    private final SduiTemplateRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public SduiTemplateService(SduiTemplateRepository repository) {
        this.repository = repository;
    }

    public List<SduiTemplate> findAll() {
        return repository.findAll();
    }

    public SduiTemplate findById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public SduiTemplate save(String name, String json) {
        validateJson(json);
        SduiTemplate t = new SduiTemplate();
        t.setName(name);
        t.setJson(json);
        // extract version if present
        try {
            JsonNode node = mapper.readTree(json);
            if (node.has("version")) t.setVersion(node.get("version").asText("1.0"));
        } catch (Exception ignored) {}
        return repository.save(t);
    }

    @Transactional
    public SduiTemplate update(String id, String name, String json) {
        SduiTemplate t = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        if (json != null) {
            validateJson(json);
            t.setJson(json);
            try {
                JsonNode node = mapper.readTree(json);
                if (node.has("version")) t.setVersion(node.get("version").asText("1.0"));
            } catch (Exception ignored) {}
        }
        if (name != null && !name.isBlank()) t.setName(name);
        return repository.save(t);
    }

    public boolean delete(String id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }

    private void validateJson(String json) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("json must not be empty");
        try {
            JsonNode node = mapper.readTree(json);
            if (!node.has("type") && !node.has("children") && !node.has("child")) {
                // allow root without type only if it has version+children? Keep lenient like Flutter validator
                // For strict SDUI, require type at root or children
            }
            // Basic check: if has type, must be non-empty
            if (node.has("type") && node.get("type").asText().isBlank()) {
                throw new IllegalArgumentException("type must be non-empty");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }
}
