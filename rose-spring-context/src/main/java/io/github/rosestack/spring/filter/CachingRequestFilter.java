package io.github.rosestack.spring.filter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 请求缓存过滤器
 *
 * <p>缓存请求体内容，允许多次读取
 *
 * @author zhijun.chen
 * @since 2.16.3
 */
public class CachingRequestFilter extends AbstractRequestFilter {

    public CachingRequestFilter(String[] excludePaths) {
        super(excludePaths);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ServletRequest requestWrapper = new CachingHttpServletRequestWrapper(request);
        chain.doFilter(requestWrapper, response);
    }

    public static class CachingHttpServletRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] bodyBytes;
        private final Map<String, String[]> parameterMap;

        public CachingHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.bodyBytes = readRequestBody(request);
            this.parameterMap = super.getParameterMap();
        }

        private byte[] readRequestBody(HttpServletRequest request) throws IOException {
            request.setCharacterEncoding("UTF-8");
            try (InputStream inputStream = request.getInputStream()) {
                return StreamUtils.copyToByteArray(inputStream);
            }
        }

        @Override
        public BufferedReader getReader() {
            return ObjectUtils.isEmpty(this.bodyBytes)
                    ? null
                    : new BufferedReader(new InputStreamReader(getInputStream(), Charset.forName("UTF-8")));
        }

        /**
         * 重写 getParameterMap() 方法，解决 undertow 中流被读取后，会进行标记，从而导致无法正确获取 body 中的表单数据的问题
         *
         * @return Map<String, String [ ]> parameterMap
         */
        @Override
        public Map<String, String[]> getParameterMap() {
            return this.parameterMap;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bodyBytes);
            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return false;
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("ReadListener is not supported");
                }
            };
        }
    }
}
