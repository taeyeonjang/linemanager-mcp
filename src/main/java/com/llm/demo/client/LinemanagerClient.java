package com.llm.demo.client;

import com.llm.demo.dto.ProcessHistoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

    @Component
    public class LinemanagerClient {

        private final RestClient restClient;
        private final String username;
        private final String password;

        private String token; // "Bearer eyJ..." 전체를 저장

        public LinemanagerClient(
                                 @Value("${linemanager.base-url}") String baseUrl,
                                 @Value("${linemanager.username}") String username,
                                 @Value("${linemanager.password}") String password) {
            this.restClient = RestClient.builder().baseUrl(baseUrl).build();
            this.username = username;
            this.password = password;
        }

        // POST /login (form 파라미터) → 응답 헤더의 Authorization 값 저장
        private void login() {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("username", username);
            form.add("password", password);

            String header = restClient.post()
                    .uri("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (header == null) {
                throw new IllegalStateException("로그인 응답에 Authorization 헤더가 없습니다.");
            }
            this.token = header;
        }

        // GET /api/history/process (조회 전용)
        public ProcessHistoryResponse searchProcessHistory(String serialNumber, Integer processNo,
                                                           String result, int page) {
            if (token == null) {
                login();
            }
            try {
                return requestHistory(serialNumber, processNo, result, page);
            } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
                // 토큰 만료 등 → 한 번만 다시 로그인해서 재시도
                login();
                return requestHistory(serialNumber, processNo, result, page);
            }
        }

        private ProcessHistoryResponse requestHistory(String serialNumber, Integer processNo,
                                                      String result, int page) {
            return restClient.get()
                    .uri(uri -> uri.path("/api/history/process")
                            .queryParamIfPresent("serialNumber", Optional.ofNullable(serialNumber))
                            .queryParamIfPresent("processNo", Optional.ofNullable(processNo))
                            .queryParamIfPresent("result", Optional.ofNullable(result))
                            .queryParam("page", page)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .body(ProcessHistoryResponse.class);
        }
    }
