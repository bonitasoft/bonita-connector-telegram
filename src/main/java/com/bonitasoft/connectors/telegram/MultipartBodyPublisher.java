package com.bonitasoft.connectors.telegram;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultipartBodyPublisher {
    private final String boundary;
    private final List<byte[]> parts;

    private MultipartBodyPublisher(String boundary, List<byte[]> parts) {
        this.boundary = boundary;
        this.parts = parts;
    }

    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    public HttpRequest.BodyPublisher toBodyPublisher() {
        var output = new ArrayList<byte[]>();
        for (byte[] part : parts) { output.add(part); }
        output.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        int totalLength = output.stream().mapToInt(b -> b.length).sum();
        byte[] body = new byte[totalLength];
        int pos = 0;
        for (byte[] chunk : output) {
            System.arraycopy(chunk, 0, body, pos, chunk.length);
            pos += chunk.length;
        }
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        private final String boundary = UUID.randomUUID().toString();
        private final List<byte[]> parts = new ArrayList<>();

        public Builder textPart(String name, String value) {
            if (value == null) return this;
            String part = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                    + value + "\r\n";
            parts.add(part.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public Builder filePart(String name, String filename, byte[] content) {
            String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
            byte[] footer = "\r\n".getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[headerBytes.length + content.length + footer.length];
            System.arraycopy(headerBytes, 0, combined, 0, headerBytes.length);
            System.arraycopy(content, 0, combined, headerBytes.length, content.length);
            System.arraycopy(footer, 0, combined, headerBytes.length + content.length, footer.length);
            parts.add(combined);
            return this;
        }

        public MultipartBodyPublisher build() { return new MultipartBodyPublisher(boundary, parts); }
    }
}
