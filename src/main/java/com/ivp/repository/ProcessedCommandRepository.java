package com.ivp.repository;

import com.ivp.domain.model.ProcessedCommand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, String> {}
