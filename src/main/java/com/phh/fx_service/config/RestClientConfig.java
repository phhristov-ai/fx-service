package com.phh.fx_service.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	public RestClient frankfurterRestClient() {

		RequestConfig requestConfig =
				RequestConfig.custom()
						.setConnectionRequestTimeout(
								Timeout.ofSeconds(2)
						)
						.setResponseTimeout(
								Timeout.ofSeconds(3)
						)
						.build();

		HttpClient httpClient =
				HttpClients.custom()
						.setDefaultRequestConfig(requestConfig)
						.build();

		HttpComponentsClientHttpRequestFactory factory =
				new HttpComponentsClientHttpRequestFactory(
						httpClient
				);

		return RestClient.builder()
				.baseUrl("https://api.frankfurter.dev")
				.requestFactory(factory)
				.build();
	}
}