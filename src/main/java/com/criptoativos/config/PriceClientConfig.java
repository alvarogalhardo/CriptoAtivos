package com.criptoativos.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PriceClientConfig {

    /**
     * Explicit timeouts matter here: without them a hung market-data call would tie up the
     * scheduler thread indefinitely.
     */
    @Bean
    RestClient coinGeckoRestClient(
            @Value("${app.prices.coingecko.base-url}") String baseUrl,
            @Value("${app.prices.coingecko.timeout}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
