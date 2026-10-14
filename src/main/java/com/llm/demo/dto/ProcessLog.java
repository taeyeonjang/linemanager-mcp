package com.llm.demo.dto;

public record ProcessLog(Long id, Integer processNo, String result, String processedAt, String completedAt, Product product) {}

