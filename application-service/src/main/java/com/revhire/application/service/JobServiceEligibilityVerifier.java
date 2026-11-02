package com.revhire.application.service;

import com.revhire.application.exception.DependencyUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Component
public class JobServiceEligibilityVerifier implements JobEligibilityVerifier {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;

    public JobServiceEligibilityVerifier(@Value("${job.service.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(RESPONSE_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void verifyJobIsOpen(Long jobId, String bearerToken) {
        requireBearerToken(bearerToken);

        JobEligibilityResponse response;
        try {
            response = restClient.get()
                    .uri("/api/jobs/{jobId}", jobId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, downstreamResponse) -> {
                        throw new IllegalArgumentException("Job does not exist");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (request, downstreamResponse) -> {
                        throw new DependencyUnavailableException(
                                "Job Service rejected the eligibility request"
                        );
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, downstreamResponse) -> {
                        throw new DependencyUnavailableException("Job Service is unavailable");
                    })
                    .body(JobEligibilityResponse.class);
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException("Job Service is unavailable");
        }

        if (response == null || response.exists() == null || response.acceptingApplications() == null) {
            throw new DependencyUnavailableException("Job Service returned an invalid eligibility response");
        }
        if (!response.exists()) {
            throw new IllegalArgumentException("Job does not exist");
        }
        if (!response.acceptingApplications()) {
            throw new IllegalArgumentException("Job is not accepting applications");
        }
    }

    @Override
    public List<OwnedJob> findJobsOwnedByRecruiter(String bearerToken) {
        requireBearerToken(bearerToken);

        List<OwnedJobResponse> response;
        try {
            response = restClient.get()
                    .uri("/api/jobs/recruiter/me")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, downstreamResponse) -> {
                        throw new DependencyUnavailableException(
                                "Job Service rejected the recruiter ownership request"
                        );
                    })
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException("Job Service is unavailable");
        }

        if (response == null || response.stream().anyMatch(job ->
                job == null || job.jobId() == null || job.jobId() <= 0 || job.title() == null)) {
            throw new DependencyUnavailableException(
                    "Job Service returned an invalid recruiter jobs response"
            );
        }

        return response.stream()
                .map(job -> new OwnedJob(job.jobId(), job.title()))
                .toList();
    }

    private void requireBearerToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new DependencyUnavailableException("Authorization token is unavailable");
        }
    }

    private record OwnedJobResponse(Long jobId, String title) {
    }

    private record JobEligibilityResponse(Boolean exists, Boolean acceptingApplications) {
    }
}
