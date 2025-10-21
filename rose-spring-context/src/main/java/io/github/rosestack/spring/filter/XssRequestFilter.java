package io.github.rosestack.spring.filter;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * XSS 防护过滤器
 *
 * <p>防止跨站脚本攻击，过滤恶意脚本
 *
 * @author rosestack
 * @since 1.0.0
 */
public class XssRequestFilter extends AbstractRequestFilter {

    // 简化的 XSS 攻击模式，只保留最常见的
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE) // 匹配所有 on 事件
    };

    public XssRequestFilter(String[] excludePaths) {
        super(excludePaths);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 包装请求，过滤 XSS 内容
        XssHttpServletRequestWrapper wrappedRequest = new XssHttpServletRequestWrapper(request);

        filterChain.doFilter(wrappedRequest, response);
    }

    /**
     * XSS 请求包装器
     */
    private static class XssHttpServletRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return cleanXss(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }

            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = cleanXss(values[i]);
            }
            return cleanValues;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return cleanXss(value);
        }

        private String cleanXss(String value) {
            if (value == null) {
                return null;
            }

            // HTML 转义
            value = HtmlUtils.htmlEscape(value);

            // 移除 XSS 攻击模式
            for (Pattern pattern : XSS_PATTERNS) {
                value = pattern.matcher(value).replaceAll("");
            }

            return value;
        }
    }
}
