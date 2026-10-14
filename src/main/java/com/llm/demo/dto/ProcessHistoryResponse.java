package com.llm.demo.dto;

import java.util.List;


    public record ProcessHistoryResponse(Long count, Integer totalPages, List<ProcessLog> list){}

