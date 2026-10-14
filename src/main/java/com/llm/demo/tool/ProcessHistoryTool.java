package com.llm.demo.tool;

import com.llm.demo.client.LinemanagerClient;
import com.llm.demo.dto.ProcessHistoryResponse;
import com.llm.demo.dto.ProcessLog;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ProcessHistoryTool {

    private static final int MAX_RESULTS = 50;
    private static final Set<String> RESULTS = Set.of("OK", "NG", "INIT");

    private final LinemanagerClient client;

    public ProcessHistoryTool(LinemanagerClient client) {
        this.client = client;
    }
    // Claude에게 돌려줄 결과
    public record SearchResult(int returnedCount, boolean truncated, List<ProcessLog> logs) {}

    @McpTool(
            name = "search_process_history",
            description = """
                    MES 라인의 공정 처리 이력을 조회한다. 조회 전용이며 데이터를 변경하지 않는다.
                    모든 조건은 선택이고, 여러 조건을 주면 모두 만족하는 이력만 반환한다(AND).
                    processNo는 해당 공정 번호와 정확히 일치하는 이력만 조회한다.
                    '3번 이후 공정' 같은 범위 조건은 지원하지 않는다.
                    결과는 최대 50건이며, 더 있으면 truncated가 true다.
                    returnedCount는 이번에 반환한 건수이며 전체 건수가 아니다.
                    """,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public SearchResult searchProcessHistory(
            @McpToolParam(description = "제품 시리얼 번호 (예: C9A15C7A9B)", required = false)
            String serialNumber,
            @McpToolParam(description = "공정 번호 (정수, 정확히 일치)", required = false)
            Integer processNo,
            @McpToolParam(description = "공정 판정 결과: OK, NG, INIT 중 하나", required = false)
            String result) {

        if (result != null) {
            result = result.toUpperCase();
            if (!RESULTS.contains(result)) {
                throw new IllegalArgumentException("result는 OK, NG, INIT 중 하나여야 합니다: " + result);
            }
        }

        List<ProcessLog> collected = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages && collected.size() < MAX_RESULTS) {
            ProcessHistoryResponse response =
                    client.searchProcessHistory(serialNumber, processNo, result, page);

            if (response == null || response.list() == null || response.list().isEmpty()) {
                break;
            }
            collected.addAll(response.list());
            totalPages = response.totalPages() == null ? page : response.totalPages();
            page++;
        }

        boolean truncated = collected.size() > MAX_RESULTS || (collected.size() >= MAX_RESULTS && page <= totalPages);
        List<ProcessLog> logs = collected.size() > MAX_RESULTS
                ? collected.subList(0, MAX_RESULTS)
                : collected;

        return new SearchResult(logs.size(), truncated, logs);
    }
}