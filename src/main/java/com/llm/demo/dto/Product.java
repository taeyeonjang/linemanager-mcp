package com.llm.demo.dto;

public record Product(Long id, String serialNumber, String status, String finalResult, Integer currentProcessNo, String createdAt, String completedAt) {}

