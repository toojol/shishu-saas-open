package com.shishu.open.demo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoServerTest {

    @TempDir
    Path webRoot;

    private HttpServer server;
    private String base;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void start() throws IOException {
        Files.writeString(webRoot.resolve("index.html"), "<title>市枢演示</title>", StandardCharsets.UTF_8);
        server = DemoServer.start(0, webRoot);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void servesStaticIndexPage() throws Exception {
        HttpResponse<String> res = get("/");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("市枢演示"));
    }

    @Test
    void consignEndpointReturnsStatements() throws Exception {
        HttpResponse<String> res = post("/api/consign/settle", "1,100,,20");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"payableToPartner\":\"80.00\""), res.body());
    }

    @Test
    void badInputReturns400WithMessage() throws Exception {
        HttpResponse<String> res = post("/api/finance/day-close", "CASH,10");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("\"error\""), res.body());
    }

    @Test
    void scaleEndpointDecodesQueryParameter() throws Exception {
        HttpResponse<String> res = get("/api/device/scale-decode?code=2100018012502");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"plu\":18"), res.body());
    }

    @Test
    void wrongMethodAndUnknownPathAreRejected() throws Exception {
        assertEquals(405, get("/api/consign/settle").statusCode());
        assertEquals(404, get("/api/nope").statusCode());
        assertEquals(404, get("/missing.html").statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
