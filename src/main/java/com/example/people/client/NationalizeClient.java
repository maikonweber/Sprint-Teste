package com.example.people.client;

import com.example.people.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Cliente HTTP da API pública <a href="https://nationalize.io">Nationalize.io</a>.
 * Qualquer falha de comunicação é traduzida para {@link ExternalServiceException}.
 */
@Component
public class NationalizeClient {

    private static final Logger log = LoggerFactory.getLogger(NationalizeClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public NationalizeClient(WebClient nationalizeWebClient, NationalizeProperties properties) {
        this.webClient = nationalizeWebClient;
        this.timeout = properties.timeout();
    }

    public NationalizeResponse predictNationality(String name) {
        log.debug("Consultando Nationalize para o nome '{}'", name);
        return webClient.get()
                .uri(uri -> uri.path("/").queryParam("name", name).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(NationalizeResponse.class)
                .timeout(timeout)
                .onErrorMap(NationalizeClient::isCommunicationFailure, this::toExternalServiceException)
                .blockOptional()
                .orElseThrow(() -> new ExternalServiceException("A API Nationalize retornou uma resposta vazia", null));
    }

    private static boolean isCommunicationFailure(Throwable error) {
        return error instanceof WebClientException
                || error instanceof TimeoutException
                || error instanceof CodecException;
    }

    private ExternalServiceException toExternalServiceException(Throwable error) {
        if (error instanceof WebClientResponseException responseError) {
            log.warn("Nationalize respondeu HTTP {}", responseError.getStatusCode().value());
            if (responseError.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                return new ExternalServiceException(
                        "Limite de requisições da API Nationalize atingido; tente novamente mais tarde", error);
            }
        } else {
            log.warn("Falha ao comunicar com a Nationalize: {}", error.toString());
        }
        return new ExternalServiceException("Serviço de previsão de nacionalidade indisponível no momento", error);
    }
}
