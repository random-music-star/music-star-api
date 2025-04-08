package com.curioussong.alsongdalsong.member.service.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class ToxicUserName {

    private final String EXTERNAL_API_ADDRESS = "http://70.12.130.131:5000";
    private final WebClient webClient;

    public ToxicUserName(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(EXTERNAL_API_ADDRESS).build();
    }


    public boolean isToxic(String nickname) {
        ToxicRequest request = new ToxicRequest(nickname);

        ToxicResponse response = webClient.post()
                .uri("/check-toxic")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ToxicResponse.class)
                .block();

        if (response != null && response.getResult() != null) {
            for (ToxicResponse.ResultItem item : response.getResult()) {
                if ("toxic".equalsIgnoreCase(item.getLabel()) && item.getScore() > 0.8) {
                    return true;
                }
            }
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    static class ToxicRequest {
        private String nickname;
    }

    @Data
    static class ToxicResponse {
        private List<ResultItem> result;

        @Data
        static class ResultItem {
            private String label;
            private double score;
        }
    }
}
