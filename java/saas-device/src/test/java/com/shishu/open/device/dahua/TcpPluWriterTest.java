package com.shishu.open.device.dahua;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TcpPluWriterTest {

    @Test
    void sendsFramesInOrderOverTcp() throws Exception {
        byte[] clear = DahuaTmCodec.clearAllPlu();
        byte[] plu = DahuaTmCodec.buildPluFrame(7, "香蕉", new java.math.BigDecimal("5.00"));
        byte[] expected = new byte[clear.length + plu.length];
        System.arraycopy(clear, 0, expected, 0, clear.length);
        System.arraycopy(plu, 0, expected, clear.length, plu.length);

        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<byte[]> received = CompletableFuture.supplyAsync(() -> {
                try (Socket s = server.accept(); InputStream in = s.getInputStream()) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    in.transferTo(buf);
                    return buf.toByteArray();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            new TcpPluWriter().send("127.0.0.1", server.getLocalPort(), clear, plu);
            assertEquals(new String(expected, Charset.forName("GB2312")),
                    new String(received.get(), Charset.forName("GB2312")));
        }
    }
}
