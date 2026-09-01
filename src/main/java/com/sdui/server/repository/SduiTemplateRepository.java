package com.sdui.server.repository;

import com.sdui.server.entity.SduiTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiTemplateRepository extends JpaRepository<SduiTemplate, String> {
}
