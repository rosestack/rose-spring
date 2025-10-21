package io.github.rosestack.spring.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import io.github.rosestack.core.util.StringPool;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet 工具类
 *
 * <p>提供 HTTP 请求响应处理的常用工具方法，支持参数提取、客户端信息获取、响应渲染等功能。
 *
 * <p>
 *
 * <h3>核心特性：</h3>
 *
 * <ul>
 *   <li>类型安全的参数提取和转换
 *   <li>客户端 IP 地址和 User-Agent 获取
 *   <li>Cookie 操作和会话管理
 *   <li>URL 编码解码缓存优化
 * </ul>
 *
 * <p>
 *
 * <h3>使用示例：</h3>
 *
 * <pre>{@code
 * // 参数提取
 * String name = ServletUtils.getParameter("name", "默认值");
 *
 * // 客户端信息
 * String clientIp = ServletUtils.getClientIp();
 *
 * // 响应渲染
 * ServletUtils.renderJson(response, "{\"status\":\"success\"}");
 * }</pre>
 *
 * <p><strong>注意：</strong>所有方法都是线程安全的，支持高并发访问。
 *
 * @author chensoul
 * @see HttpServletRequest
 * @see HttpServletResponse
 * @see RequestContextHolder
 * @since 1.0.0
 */
@Slf4j
public abstract class ServletUtils {
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_USER_ID = "X-User-ID";

    private static final Map<String, String> URL_DECODE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> URL_ENCODE_CACHE = new ConcurrentHashMap<>();
    private static final String UNKNOWN = "unknown";

    /**
     * 用于检测客户端真实 IP 的 HTTP 头列表，按优先级排序
     */
    private static final List<String> DEFAULT_IP_HEADERS = Arrays.asList(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR");
    /**
     * 敏感字段模式（用于请求体脱敏）
     */
    private static final String[] SENSITIVE_BODY_PATTERNS = {
        "password",
        "passwd",
        "pwd",
        "secret",
        "token",
        "key",
        "credential",
        "auth",
        "authorization",
        "signature",
        "apiKey",
        "api_key",
        "accessToken",
        "access_token",
        "x-auth-token",
        "cookie",
        "set-cookie",
        "x-api-key",
        "x-secret",
        "refreshToken",
        "refresh_token",
        "sessionId",
        "session_id"
    };

    /**
     * 私有构造函数，防止实例化
     */
    private ServletUtils() {}

    /**
     * 获取请求参数值
     *
     * @param name 参数名
     * @return 参数值，不存在时返回 null
     */
    public static String getRequestParameter(String name) {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getParameter(name) : null;
    }

    public static String getRequestParameter(String name, String defaultValue) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return defaultValue;
        }
        String value = request.getParameter(name);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    public static Map<String, String[]> getRequestParams(ServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        final Map<String, String[]> map = request.getParameterMap();
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, String> getRequestParams() {
        HttpServletRequest request = getCurrentRequest();

        if (request == null) {
            return Collections.emptyMap();
        }

        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : getRequestParams(request).entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), String.join(StringPool.COMMA, values));
            }
        }
        return params;
    }

    /**
     * 获得所有请求参数（保留多值）
     *
     * @return 参数Map
     */
    public static Map<String, List<String>> getRequestParamList() {
        HttpServletRequest request = getCurrentRequest();

        if (request == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : getRequestParams(request).entrySet()) {
            String[] values = entry.getValue();
            if (values != null) {
                params.put(entry.getKey(), Arrays.asList(values));
            }
        }
        return params;
    }

    /**
     * Retrieves a request header value by name.
     *
     * @param name The header name to retrieve
     * @return The header value, or null if not found or no request context
     */
    public static String getRequestHeader(String name) {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader(name) : null;
    }

    public static String getRequestHeader(String name, String defaultValue) {
        String value = getRequestHeader(name);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    public static Map<String, String> getRequestHeaders() {
        return getRequestHeaders(null);
    }

    public static Map<String, String> getRequestHeaders(Function<String, String> function) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return Collections.emptyMap();
        }

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (function != null) {
                    headers.put(name, function.apply(name));
                } else {
                    headers.put(name, request.getHeader(name));
                }
            }
        }
        return headers;
    }

    public static Map<String, String> getResponseHeaders() {
        return getResponseHeaders(null);
    }

    public static Map<String, String> getResponseHeaders(Function<String, String> function) {
        HttpServletResponse response = getCurrentResponse();
        if (response == null) {
            return Collections.emptyMap();
        }

        Map<String, String> headers = new HashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        if (headerNames != null) {
            for (String name : headerNames) {
                if (function != null) {
                    headers.put(name, function.apply(name));
                } else {
                    headers.put(name, response.getHeader(name));
                }
            }
        }
        return headers;
    }

    /**
     * 获取当前 HTTP 请求对象
     *
     * @return 当前请求对象，无请求上下文时返回 null
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes requestAttributes = getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return requestAttributes.getRequest();
    }

    public static HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes requestAttributes = getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        return requestAttributes.getResponse();
    }

    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes ? (ServletRequestAttributes) attributes : null;
    }

    /**
     * 获取当前请求的 Rose Web 认证详情
     *
     * @return RoseWebAuthenticationDetails 对象，如果不存在则返回 null
     */
    public static Object getRoseWebAuthenticationDetails() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        // 使用字符串常量避免直接依赖 security 模块
        return request.getAttribute("ROSE_WEB_AUTH_DETAILS");
    }

    /**
     * 将字符串渲染到客户端
     *
     * @param response    响应对象
     * @param string      待渲染的字符串
     * @param contentType 内容类型
     */
    public static void renderString(HttpServletResponse response, String string, String contentType) {
        try {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(contentType);
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            response.getWriter().print(string);
        } catch (IOException e) {
            log.error("Failed to render string to response", e);
            throw new RuntimeException("Failed to render string to response", e);
        }
    }

    /**
     * 将JSON字符串渲染到客户端
     *
     * @param response 响应对象
     * @param json     JSON字符串
     */
    public static void renderJson(HttpServletResponse response, String json) {
        renderString(response, json, MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * URL编码
     *
     * @param str 待编码的字符串
     * @return 编码后的字符串
     * @throws UnsupportedEncodingException 编码异常
     */
    public static String urlEncode(String str) throws UnsupportedEncodingException {
        if (StringUtils.isBlank(str)) {
            return str;
        }

        // 检查缓存
        String cached = URL_ENCODE_CACHE.get(str);
        if (cached != null) {
            return cached;
        }

        String encoded = URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        URL_ENCODE_CACHE.put(str, encoded);
        return encoded;
    }

    /**
     * URL解码
     *
     * @param str 待解码的字符串
     * @return 解码后的字符串
     * @throws UnsupportedEncodingException 解码异常
     */
    public static String urlDecode(String str) throws UnsupportedEncodingException {
        if (StringUtils.isBlank(str)) {
            return str;
        }

        // 检查缓存
        String cached = URL_DECODE_CACHE.get(str);
        if (cached != null) {
            return cached;
        }

        String decoded = URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        URL_DECODE_CACHE.put(str, decoded);
        return decoded;
    }

    /**
     * 安全的URL编码（不抛出异常）
     *
     * @param str 待编码的字符串
     * @return 编码后的字符串，失败时返回原字符串
     */
    public static String urlEncodeSafe(String str) {
        try {
            return urlEncode(str);
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to URL encode string: {}", str, e);
            return str;
        }
    }

    /**
     * 安全的URL解码（不抛出异常）
     *
     * @param str 待解码的字符串
     * @return 解码后的字符串，失败时返回原字符串
     */
    public static String urlDecodeSafe(String str) {
        try {
            return urlDecode(str);
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to URL decode string: {}", str, e);
            return str;
        }
    }

    public static String getClientIp(HttpServletRequest request, String... otherHeaderNames) {
        if (request == null) {
            return null;
        }

        Set<String> headerNames = new LinkedHashSet<>(DEFAULT_IP_HEADERS);
        headerNames.addAll(Arrays.asList(otherHeaderNames));

        String ip;
        for (String header : headerNames) {
            ip = request.getHeader(header);
            ip = getReverseProxyIp(ip);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return normalizeIpAddress(ip);
            }
        }

        // Fall back to remote address if no proxy headers found
        ip = request.getRemoteAddr();
        ip = getReverseProxyIp(ip);
        return normalizeIpAddress(ip);
    }

    /**
     * Processes a potentially comma-separated IP address string from proxy headers.
     *
     * <p>When requests pass through multiple proxies or load balancers, the forwarded IP headers
     * often contain comma-separated lists of IP addresses representing the chain of proxies. This
     * method extracts the first valid IP address from such lists.
     *
     * <p><strong>Processing Logic:</strong>
     *
     * <ul>
     *   <li>If IP contains commas, split and check each part
     *   <li>Return first non-blank, non-"unknown" IP found
     *   <li>Return original IP if no commas or no valid IPs found
     * </ul>
     *
     * <p><strong>Example Input/Output:</strong>
     *
     * <pre>
     * "192.168.1.100, 10.0.0.1, unknown" → "192.168.1.100"
     * "unknown, 203.0.113.1" → "203.0.113.1"
     * "192.168.1.100" → "192.168.1.100"
     * </pre>
     *
     * @param ip The IP address string to process, potentially comma-separated. Can be null or empty.
     * @return The first valid IP address found, or the original string if no processing needed
     */
    public static String getReverseProxyIp(String ip) {
        if (ip != null && ip.contains(StringPool.COMMA)) {
            for (String subIp : ip.split(StringPool.COMMA)) {
                String trimmedIp = subIp.trim();
                if (StringUtils.isNotBlank(trimmedIp) && !"unknown".equalsIgnoreCase(trimmedIp)) {
                    return trimmedIp;
                }
            }
        }
        return ip;
    }

    /**
     * 获取客户端真实 IP 地址
     *
     * <p>可以设置优先使用 IPv4: <code>System.setProperty("java.net.preferIPv4Stack", "true");</code>
     *
     * @return 客户端 IP 地址，获取失败返回 null
     */
    public static String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        String ip = getClientIp(request);
        return normalizeIpAddress(ip);
    }

    /**
     * 标准化 IP 地址，将 IPv6 本地回环地址转换为 IPv4
     *
     * <p>将 IPv6 本地回环地址 "0:0:0:0:0:0:0:1" 或 "::1" 转换为 IPv4 的 "127.0.0.1" 其他地址保持不变
     *
     * @param ip 原始 IP 地址
     * @return 标准化后的 IP 地址
     */
    public static String normalizeIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        // 将 IPv6 本地回环地址转换为 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }

    /**
     * 获取Cookie值
     *
     * @param name Cookie名称
     * @return Cookie值
     */
    public static String getCookieValue(String name) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 获取Cookie对象
     *
     * @param name Cookie名称
     * @return Cookie对象
     */
    public static Cookie getCookie(String name) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * 添加Cookie
     *
     * @param response 响应对象
     * @param name     Cookie名称
     * @param value    Cookie值
     */
    public static void addCookie(HttpServletResponse response, String name, String value) {
        addCookie(response, name, value, -1);
    }

    /**
     * 添加Cookie
     *
     * @param response 响应对象
     * @param name     Cookie名称
     * @param value    Cookie值
     * @param maxAge   最大存活时间（秒）
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * 清理缓存
     */
    public static void clearCache() {
        URL_DECODE_CACHE.clear();
        URL_ENCODE_CACHE.clear();
    }

    /**
     * 获取请求的完整URL（使用当前请求）
     *
     * @return 完整URL
     */
    public static String getFullUrl() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String queryString = request.getQueryString();
        String requestURI = request.getRequestURI();

        if (StringUtils.isNotBlank(queryString)) {
            return requestURI + "?" + queryString;
        }
        return requestURI;
    }

    /**
     * 获取请求的User-Agent（使用当前请求）
     *
     * @return User-Agent
     */
    public static String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader(HttpHeaders.USER_AGENT) : null;
    }

    public static String getReferer() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader(HttpHeaders.REFERER) : null;
    }

    /**
     * 获取请求路径
     *
     * <p>优先使用 pathInfo，如果为空则使用 servletPath
     *
     * @param request HTTP 请求
     * @return 请求路径
     */
    public static String getRequestPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();

        if (pathInfo != null) {
            return servletPath + pathInfo;
        }

        return servletPath != null ? servletPath : request.getRequestURI();
    }

    /**
     * 从 URI 字符串中提取路径
     *
     * @param uri URI 字符串
     * @return 路径部分
     */
    public static String extractPathFromUri(String uri) {
        if (ObjectUtils.isEmpty(uri)) {
            return "";
        }

        // 移除查询参数
        int queryIndex = uri.indexOf('?');
        if (queryIndex != -1) {
            uri = uri.substring(0, queryIndex);
        }

        // 移除片段标识符
        int fragmentIndex = uri.indexOf('#');
        if (fragmentIndex != -1) {
            uri = uri.substring(0, fragmentIndex);
        }

        return uri;
    }

    public static String getSessionId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null && request.getSession(false) != null) {
            return request.getSession().getId();
        }
        return UNKNOWN;
    }

    /**
     * 获取当前用户ID
     */
    public static String getUserId() {
        return getValueFromRequestAndMdc(HEADER_USER_ID, false);
    }

    public static String getRequestId() {
        return getValueFromRequestAndMdc(HEADER_REQUEST_ID, true);
    }

    public static String getUsername() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return request.getUserPrincipal().getName();
    }

    /**
     * 收集请求头（排除敏感头）
     */
    public static Map<String, String> collectRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();

        if (request.getHeaderNames() != null) {
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerNameLower = headerName.toLowerCase();

                // 检查是否为敏感头
                boolean isSensitive = false;
                for (String sensitiveHeader : SENSITIVE_BODY_PATTERNS) {
                    if (headerNameLower.contains(sensitiveHeader)) {
                        isSensitive = true;
                        break;
                    }
                }

                if (!isSensitive) {
                    headers.put(headerName, request.getHeader(headerName));
                } else {
                    // 敏感头用 *** 替代
                    headers.put(headerName, "***");
                }
            }
        }

        return headers;
    }

    /**
     * 收集请求参数（排除敏感参数）
     */
    public static Map<String, String[]> collectRequestParameters(HttpServletRequest request) {
        Map<String, String[]> parameters = new HashMap<>();

        if (request.getParameterMap() != null) {
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                String paramName = entry.getKey().toLowerCase();

                // 检查是否为敏感参数
                boolean isSensitive = false;
                for (String sensitiveParam : SENSITIVE_BODY_PATTERNS) {
                    if (paramName.contains(sensitiveParam)) {
                        isSensitive = true;
                        break;
                    }
                }

                if (!isSensitive) {
                    parameters.put(entry.getKey(), entry.getValue());
                } else {
                    // 敏感参数用 **** 替代
                    parameters.put(entry.getKey(), new String[] {"****"});
                }
            }
        }

        return parameters;
    }

    /**
     * 提取请求体（仅对小文件和文本类型）
     */
    public static String extractRequestBody(HttpServletRequest request) {
        String contentType = request.getContentType();

        // 只处理文本类型的请求体
        if (contentType == null
                || (!contentType.startsWith("application/json")
                        && !contentType.startsWith("application/xml")
                        && !contentType.startsWith("text/"))) {
            return null;
        }

        // 限制请求体大小，避免记录过大的内容
        int contentLength = request.getContentLength();
        if (contentLength > 10240) { // 10KB 限制
            return "Request body too large (" + contentLength + " bytes)";
        }

        // 只记录 POST/PUT 等方法的请求体
        if (!("POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod()))) {
            return null;
        }

        try {
            // 优先尝试 Spring 的 ContentCachingRequestWrapper（高性能，直接类型检查）
            if (request instanceof ContentCachingRequestWrapper) {
                return extractFromSpringContentCaching(request);
            }

            // 对于其他请求，不尝试读取流以保证性能和安全
            return "Request body available (not extracted for performance and stream safety)";

        } catch (NoClassDefFoundError e) {
            // Spring Web 类不存在时
            return "Request body available (Spring Web not available)";
        } catch (Exception e) {
            return "Failed to extract request body: " + e.getMessage();
        }
    }

    public static String generateDeviceFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();

        // User-Agent
        String userAgent = getUserAgent();
        if (userAgent != null) {
            fingerprint.append(userAgent.hashCode());
        }

        // Accept-Language
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null) {
            fingerprint.append("-").append(acceptLanguage.hashCode());
        }

        // Accept-Encoding
        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (acceptEncoding != null) {
            fingerprint.append("-").append(acceptEncoding.hashCode());
        }

        return "FP-" + Math.abs(fingerprint.toString().hashCode());
    }

    /**
     * 从 Spring ContentCachingRequestWrapper 提取请求体（高性能）
     */
    private static String extractFromSpringContentCaching(HttpServletRequest request) {
        try {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();

            if (content != null && content.length > 0) {
                String encoding = request.getCharacterEncoding();
                if (encoding == null) {
                    encoding = "UTF-8";
                }
                String bodyContent = new String(content, encoding);

                // 对请求体进行脱敏处理
                return sanitizeRequestBody(bodyContent);
            }

            return "Request body is empty";
        } catch (Exception e) {
            return "Failed to extract from ContentCachingRequestWrapper: " + e.getMessage();
        }
    }

    /**
     * 对请求体进行脱敏处理
     *
     * @param bodyContent 原始请求体内容
     * @return 脱敏后的请求体内容
     */
    private static String sanitizeRequestBody(String bodyContent) {
        if (StringUtils.isBlank(bodyContent)) {
            return bodyContent;
        }

        try {
            String contentType =
                    getCurrentRequest() != null ? getCurrentRequest().getContentType() : "";

            if (contentType != null && contentType.startsWith("application/json")) {
                // JSON 格式脱敏
                return sanitizeJsonBody(bodyContent);
            } else if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                // 表单格式脱敏
                return sanitizeFormBody(bodyContent);
            } else {
                // 其他格式使用通用脱敏
                return sanitizeGenericBody(bodyContent);
            }
        } catch (Exception e) {
            log.warn("Failed to sanitize request body: {}", e.getMessage());
            return "Request body sanitization failed";
        }
    }

    /**
     * JSON 格式请求体脱敏
     */
    private static String sanitizeJsonBody(String jsonBody) {
        for (String pattern : SENSITIVE_BODY_PATTERNS) {
            // JSON 字段模式：\"fieldName\"\s*:\s*\"value\"
            String regex = "\"" + pattern + "\"\\s*:\\s*\"[^\"]*\"";
            jsonBody = jsonBody.replaceAll("(?i)" + regex, "\"" + pattern + "\":\"****\"");

            // 无引号值模式：\"fieldName\"\s*:\s*value
            String regexNoQuotes = "\"" + pattern + "\"\\s*:\\s*[^,}\\]\\s]+";
            jsonBody = jsonBody.replaceAll("(?i)" + regexNoQuotes, "\"" + pattern + "\":\"****\"");
        }
        return jsonBody;
    }

    /**
     * 表单格式请求体脱敏
     */
    private static String sanitizeFormBody(String formBody) {
        for (String pattern : SENSITIVE_BODY_PATTERNS) {
            // 表单字段模式：fieldName=value&
            String regex = pattern + "=[^&]*";
            formBody = formBody.replaceAll("(?i)" + regex, pattern + "=****");
        }
        return formBody;
    }

    /**
     * 通用格式请求体脱敏
     */
    private static String sanitizeGenericBody(String body) {
        for (String pattern : SENSITIVE_BODY_PATTERNS) {
            // 通用模式：查找敏感词并替换其后的值
            String regex = "(?i)" + pattern + "\\s*[=:]\\s*\\S+";
            body = body.replaceAll(regex, pattern + "=****");
        }
        return body;
    }

    private static String getValueFromRequestAndMdc(String name, boolean generate) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String requestId = StringUtils.defaultString(request.getHeader(name), MDC.get(name));
        if (StringUtils.isBlank(requestId) && generate) {
            requestId = generateId();
        }
        return requestId;
    }

    /**
     * 生成请求ID
     */
    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
