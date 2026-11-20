package com.shishu.open.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地演示服务（仅绑定 127.0.0.1）。
 * <p>
 * 接口：
 * <ul>
 *   <li>POST /api/consign/settle —— 联营对账，文本每行一条</li>
 *   <li>POST /api/finance/day-close —— 日结支付对账</li>
 *   <li>GET  /api/device/scale-decode?code= —— 电子秤条码解析</li>
 *   <li>GET  /api/device/plu-frame?plu=&amp;name=&amp;price= —— PLU 下发帧预览</li>
 * </ul>
 * 其余路径从 web 目录提供静态页面。
 */
public final class DemoServer {

    private static final int DEFAULT_PORT = 8090;

    private DemoServer() {
    }

    public static HttpServer start(int port, Path webRoot) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/api/", DemoServer::handleApi);
        server.createContext("/", exchange -> handleStatic(exchange, webRoot));
        server.start();
        return server;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Path webRoot = Path.of(args.length > 1 ? args[1] : "web-demo").toAbsolutePath().normalize();
        HttpServer server = start(port, webRoot);
        System.out.println("市枢开源演示已启动：http://127.0.0.1:" + server.getAddress().getPort() + "/");
        System.out.println("静态页面目录：" + webRoot);
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
            String result;
            if ("/api/consign/settle".equals(path)) {
                requireMethod(method, "POST");
                result = DemoApi.consignSettle(readBody(ex));
            } else if ("/api/finance/day-close".equals(path)) {
                requireMethod(method, "POST");
                result = DemoApi.dayClose(readBody(ex));
            } else if ("/api/device/scale-decode".equals(path)) {
                requireMethod(method, "GET");
                result = DemoApi.decodeScale(query.get("code"));
            } else if ("/api/device/plu-frame".equals(path)) {
                requireMethod(method, "GET");
                result = DemoApi.pluFrame(query.get("plu"), query.get("name"), query.get("price"));
            } else {
                send(ex, 404, "{\"error\":\"接口不存在\"}");
                return;
            }
            send(ex, 200, result);
        } catch (MethodNotAllowed e) {
            send(ex, 405, "{\"error\":\"请求方法不允许\"}");
        } catch (IllegalArgumentException e) {
            send(ex, 400, "{\"error\":" + Json.quote(e.getMessage()) + "}");
        }
    }

    private static void handleStatic(HttpExchange ex, Path webRoot) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, "{\"error\":\"请求方法不允许\"}");
            return;
        }
        String raw = ex.getRequestURI().getPath();
        String rel = "/".equals(raw) ? "index.html" : raw.substring(1);
        Path file = webRoot.resolve(rel).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            send(ex, 404, "{\"error\":\"页面不存在\"}");
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType(file.getFileName().toString()));
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private static void requireMethod(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new MethodNotAllowed();
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            out.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return out;
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static final class MethodNotAllowed extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
