package com.omarpolo.gemini;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Gemini {
    public static class DummyManager extends X509ExtendedTrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    public static class MalformedResponse extends Exception {
        public MalformedResponse() {}
        public MalformedResponse(String msg) {
            super(msg);
        }
    }

    public static class Response implements AutoCloseable {
        private final BufferedReader in;
        private final PrintWriter out;
        private final SSLSocket sock;

        private final int code;
        private final String meta;

        private Response(PrintWriter out, SSLSocket sock) throws IOException, MalformedResponse {
            var inStream = sock.getInputStream();
            this.in = new BufferedReader(new InputStreamReader(inStream));
            this.out = out;
            this.sock = sock;

            var reply = in.readLine();
            if (reply.length() > 1027) {
                throw new MalformedResponse("reply header too long");
            }

            var s = new Scanner(new StringReader(reply));
            try {
                code = s.nextInt();
                s.skip(" ");
                meta = s.nextLine();
            } catch (NoSuchElementException e) {
                throw new MalformedResponse();
            }
        }

        public int getCode() {
            return code;
        }

        public String getMeta() {
            return meta;
        }

        public BufferedReader body() {
            return in;
        }

        public void close() throws IOException {
            in.close();
            out.close();
            sock.close();
        }
    }

    public static SSLSocket connect(String host, int port) throws IOException {
        try {
            var params = new SSLParameters();
            params.setServerNames(Collections.singletonList(new SNIHostName(host)));

            var ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new DummyManager[]{new DummyManager()}, new SecureRandom());
            var factory = (SSLSocketFactory) ctx.getSocketFactory();

            var socket = (SSLSocket) factory.createSocket(host, port);
            socket.setSSLParameters(params);
            socket.startHandshake();
            return socket;
        }
        catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Unexpected failure", e);
        }
    }

    public static Response get(URL url) throws IOException, MalformedResponse {
        int port = url.getPort();
        if (port == -1) {
            port = 1965;
        }

        String req = url.toString() + "\r\n";
        return get(url.getHost(), port, req);
    }

    public static Response get(String host, int port, String req) throws IOException, MalformedResponse {
        var sock = connect(host, port);

        var outStream = sock.getOutputStream();
        var out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(outStream)));

        out.print(req);
        out.flush();

        return new Response(out, sock);
    }
}
