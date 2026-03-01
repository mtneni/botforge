package org.legendstack.basebot.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;

/**
 * Wraps HttpServletRequest to allow reading the body multiple times.
 * Required by ContentModerationFilter since the default request body
 * stream can only be read once.
 */
class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final String cachedBody;

    CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        try (var reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            var sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            this.cachedBody = sb.toString();
        }
    }

    String getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        var stream = new ByteArrayInputStream(cachedBody.getBytes());
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return stream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
            }

            @Override
            public int read() {
                return stream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}
