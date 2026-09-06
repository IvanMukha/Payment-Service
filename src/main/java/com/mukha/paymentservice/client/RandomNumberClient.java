package com.mukha.paymentservice.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestClient;

@Component
@EnableConfigurationProperties(RandomNumberClient.RandomNumberClientProperties.class)
public class RandomNumberClient {

    private final RestClient restClient;

    public RandomNumberClient(
            RestClient.Builder restClientBuilder,
            RandomNumberClientProperties properties) {

        this.restClient = restClientBuilder
                .baseUrl(properties.url())
                .build();
    }

    public String getRandomInteger(int num, int min, int max, int col, int base, String format) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/integers")
                        .queryParam("num", num)
                        .queryParam("min", min)
                        .queryParam("max", max)
                        .queryParam("col", col)
                        .queryParam("base", base)
                        .queryParam("format", format)
                        .build())
                .retrieve()
                .body(String.class);
    }

    @ConfigurationProperties(prefix = "random-number-client")
    public record RandomNumberClientProperties(String url) {
    }
}
