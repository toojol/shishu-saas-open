package com.shishu.open.device.dahua;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 示例：把帧通过 TCP 发给秤（默认端口 4001）。
 * 生产环境的重试、超时、并发控制请按门店网络情况自行加强。
 */
public final class TcpPluWriter {

    public static final int DEFAULT_PORT = 4001;

    private final int connectTimeoutMs;

    public TcpPluWriter() {
        this(3000);
    }

    public TcpPluWriter(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /** 按顺序发送多个帧（例如先清 PLU 再逐条下传）。 */
    public void send(String host, int port, byte[]... frames) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(connectTimeoutMs);
            try (OutputStream out = socket.getOutputStream()) {
                for (byte[] frame : frames) {
                    out.write(frame);
                }
                out.flush();
            }
        }
    }
}
