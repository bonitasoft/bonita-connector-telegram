package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;

class MultipartBodyPublisherTest {

    @Test
    void should_create_valid_content_type() {
        MultipartBodyPublisher publisher = MultipartBodyPublisher.newBuilder()
                .textPart("test", "value")
                .build();
        assertThat(publisher.contentType()).startsWith("multipart/form-data; boundary=");
    }

    @Test
    void should_build_multipart_body_with_text_parts() {
        MultipartBodyPublisher publisher = MultipartBodyPublisher.newBuilder()
                .textPart("chat_id", "-100123")
                .textPart("document", "https://example.com/file.pdf")
                .build();

        HttpRequest.BodyPublisher bodyPublisher = publisher.toBodyPublisher();
        assertThat(bodyPublisher.contentLength()).isGreaterThan(0);

        String content = readBody(bodyPublisher);
        assertThat(content).contains("Content-Disposition: form-data; name=\"chat_id\"");
        assertThat(content).contains("-100123");
        assertThat(content).contains("Content-Disposition: form-data; name=\"document\"");
        assertThat(content).contains("https://example.com/file.pdf");
    }

    @Test
    void should_generate_unique_boundary() {
        MultipartBodyPublisher p1 = MultipartBodyPublisher.newBuilder().build();
        MultipartBodyPublisher p2 = MultipartBodyPublisher.newBuilder().build();
        assertThat(p1.contentType()).isNotEqualTo(p2.contentType());
    }

    @Test
    void should_handle_empty_parts() {
        MultipartBodyPublisher publisher = MultipartBodyPublisher.newBuilder().build();
        HttpRequest.BodyPublisher bodyPublisher = publisher.toBodyPublisher();
        String content = readBody(bodyPublisher);
        assertThat(content).contains("--");
        assertThat(content).endsWith("--\r\n");
    }

    @Test
    void should_skip_null_values_in_text_part() {
        MultipartBodyPublisher publisher = MultipartBodyPublisher.newBuilder()
                .textPart("key", null)
                .textPart("valid", "value")
                .build();

        String content = readBody(publisher.toBodyPublisher());
        assertThat(content).doesNotContain("name=\"key\"");
        assertThat(content).contains("name=\"valid\"");
    }

    @Test
    void should_build_file_part() {
        byte[] fileContent = "file-content".getBytes();
        MultipartBodyPublisher publisher = MultipartBodyPublisher.newBuilder()
                .filePart("document", "test.pdf", fileContent)
                .build();

        String content = readBody(publisher.toBodyPublisher());
        assertThat(content).contains("Content-Disposition: form-data; name=\"document\"; filename=\"test.pdf\"");
        assertThat(content).contains("Content-Type: application/octet-stream");
        assertThat(content).contains("file-content");
    }

    private String readBody(HttpRequest.BodyPublisher pub) {
        List<ByteBuffer> buffers = new ArrayList<>();
        pub.subscribe(new Flow.Subscriber<>() {
            Flow.Subscription sub;
            @Override public void onSubscribe(Flow.Subscription subscription) {
                this.sub = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            @Override public void onNext(ByteBuffer item) { buffers.add(item); }
            @Override public void onError(Throwable throwable) {}
            @Override public void onComplete() {}
        });
        StringBuilder sb = new StringBuilder();
        for (ByteBuffer buf : buffers) {
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            sb.append(new String(bytes));
        }
        return sb.toString();
    }
}
