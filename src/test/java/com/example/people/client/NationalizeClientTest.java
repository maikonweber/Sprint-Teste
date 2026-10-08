package com.example.people.client;

import com.example.people.exception.ExternalServiceException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercita o cliente HTTP real contra um servidor WireMock local — sem acesso à internet.
 */
class NationalizeClientTest {

    @RegisterExtension
    static WireMockExtension nationalize = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private NationalizeClient client;

    @BeforeEach
    void setUp() {
        NationalizeProperties properties = new NationalizeProperties(nationalize.baseUrl(), Duration.ofSeconds(2));
        client = new NationalizeClient(WebClient.create(nationalize.baseUrl()), properties);
    }

    @Test
    void shouldParseSuccessfulResponse() {
        nationalize.stubFor(get(urlPathEqualTo("/")).withQueryParam("name", equalTo("Nathaniel"))
                .willReturn(okJson("""
                        {"count": 1234, "name": "Nathaniel",
                         "country": [{"country_id": "US", "probability": 0.42},
                                     {"country_id": "GB", "probability": 0.1}]}
                        """)));

        NationalizeResponse response = client.predictNationality("Nathaniel");

        assertThat(response.country()).hasSize(2);
        assertThat(response.mostProbableCountry()).get()
                .extracting(NationalizeResponse.CountryProbability::countryId)
                .isEqualTo("US");
        nationalize.verify(getRequestedFor(urlPathEqualTo("/")).withQueryParam("name", equalTo("Nathaniel")));
    }

    @Test
    void shouldEncodeNamesWithSpecialCharacters() {
        nationalize.stubFor(get(urlPathEqualTo("/")).withQueryParam("name", equalTo("João"))
                .willReturn(okJson("{\"count\": 1, \"name\": \"João\", \"country\": []}")));

        assertThat(client.predictNationality("João").mostProbableCountry()).isEmpty();
    }

    @Test
    void shouldTranslateServerErrorToExternalServiceException() {
        nationalize.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.predictNationality("Nathaniel"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("indisponível");
    }

    @Test
    void shouldReportRateLimit() {
        nationalize.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(429).withBody("{\"error\":\"Request limit reached\"}")));

        assertThatThrownBy(() -> client.predictNationality("Nathaniel"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Limite de requisições");
    }

    @Test
    void shouldTimeoutSlowResponses() {
        nationalize.stubFor(get(urlPathEqualTo("/"))
                .willReturn(okJson("{\"count\": 0, \"name\": \"x\", \"country\": []}").withFixedDelay(4_000)));

        assertThatThrownBy(() -> client.predictNationality("Nathaniel"))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void shouldTranslateMalformedBody() {
        nationalize.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{oops")));

        assertThatThrownBy(() -> client.predictNationality("Nathaniel"))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void shouldTranslateConnectionFailure() {
        NationalizeClient unreachable = new NationalizeClient(
                WebClient.create("http://localhost:1"),
                new NationalizeProperties("http://localhost:1", Duration.ofMillis(500)));

        assertThatThrownBy(() -> unreachable.predictNationality("Nathaniel"))
                .isInstanceOf(ExternalServiceException.class);
    }
}
