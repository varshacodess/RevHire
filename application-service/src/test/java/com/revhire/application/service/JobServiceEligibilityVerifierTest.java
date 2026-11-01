package com.revhire.application.service;

import com.revhire.application.exception.DependencyUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobServiceEligibilityVerifierTest {

    @Test
    void acceptsOpenJobAndForwardsTheBearerToken() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = startServer(200, "{\"exists\":true,\"acceptingApplications\":true}", 0,
                exchange -> authorization.set(exchange.getRequestHeaders().getFirst("Authorization")));
        try {
            verifier(server).verifyJobIsOpen(12L, "candidate-jwt-value");
            assertEquals("Bearer candidate-jwt-value", authorization.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retrievesRecruiterJobsAndForwardsTheAuthenticatedBearerToken() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/jobs/recruiter/me", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200,
                    "[{\"jobId\":12,\"title\":\"Backend Engineer\",\"exists\":true,\"status\":\"OPEN\","
                            + "\"acceptingApplications\":true}]");
        });
        server.start();
        try {
            assertEquals(
                    List.of(new JobEligibilityVerifier.OwnedJob(12L, "Backend Engineer")),
                    verifier(server).findJobsOwnedByRecruiter("recruiter-jwt-value")
            );
            assertEquals("Bearer recruiter-jwt-value", authorization.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsInvalidRecruiterJobsResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/jobs/recruiter/me", exchange ->
                respond(exchange, 200, "[{\"jobId\":null,\"title\":\"Invalid job\"}]"));
        server.start();
        try {
            assertThrows(
                    DependencyUnavailableException.class,
                    () -> verifier(server).findJobsOwnedByRecruiter("recruiter-jwt-value")
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsRecruiterOwnershipEndpointFailuresToServiceUnavailable() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/jobs/recruiter/me", exchange -> respond(exchange, 403, ""));
        server.start();
        try {
            assertThrows(
                    DependencyUnavailableException.class,
                    () -> verifier(server).findJobsOwnedByRecruiter("recruiter-jwt-value")
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsClosedJob() throws Exception {
        HttpServer server = startServer(200, "{\"exists\":true,\"acceptingApplications\":false}");
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> verifier(server).verifyJobIsOpen(12L, "candidate-jwt-value")
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsMissingJob() throws Exception {
        HttpServer server = startServer(200, "{\"exists\":false,\"acceptingApplications\":false}");
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> verifier(server).verifyJobIsOpen(12L, "candidate-jwt-value")
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsDownstreamAuthenticationFailuresToServiceUnavailable() throws Exception {
        for (int status : new int[]{401, 403}) {
            HttpServer server = startServer(status, "");
            try {
                assertThrows(
                        DependencyUnavailableException.class,
                        () -> verifier(server).verifyJobIsOpen(12L, "candidate-jwt-value")
                );
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    void mapsJobServiceServerErrorsToServiceUnavailable() throws Exception {
        HttpServer server = startServer(503, "");
        try {
            assertThrows(
                    DependencyUnavailableException.class,
                    () -> verifier(server).verifyJobIsOpen(12L, "candidate-jwt-value")
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsConnectionFailureToServiceUnavailable() throws Exception {
        HttpServer server = startServer(200, "{\"exists\":true,\"acceptingApplications\":true}");
        int port = server.getAddress().getPort();
        server.stop(0);

        assertThrows(
                DependencyUnavailableException.class,
                () -> new JobServiceEligibilityVerifier("http://localhost:" + port)
                        .verifyJobIsOpen(12L, "candidate-jwt-value")
        );
    }

    @Test
    void mapsResponseTimeoutToServiceUnavailable() throws Exception {
        HttpServer server = startServer(
                200,
                "{\"exists\":true,\"acceptingApplications\":true}",
                4000,
                exchange -> { }
        );
        try {
            assertThrows(
                    DependencyUnavailableException.class,
                    () -> verifier(server).verifyJobIsOpen(12L, "candidate-jwt-value")
            );
        } finally {
            server.stop(0);
        }
    }

    private JobServiceEligibilityVerifier verifier(HttpServer server) {
        return new JobServiceEligibilityVerifier("http://localhost:" + server.getAddress().getPort());
    }

    private HttpServer startServer(int status, String body) throws IOException {
        return startServer(status, body, 0, exchange -> { });
    }

    private HttpServer startServer(int status, String body, int delayMillis, ExchangeObserver observer)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/jobs/12", exchange -> {
            observer.observe(exchange);
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            respond(exchange, status, body);
        });
        server.start();
        return server;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    @FunctionalInterface
    private interface ExchangeObserver {
        void observe(HttpExchange exchange);
    }
}
