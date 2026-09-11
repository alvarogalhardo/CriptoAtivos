package com.criptoativos.operation;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationRepository extends JpaRepository<Operation, UUID> {}
