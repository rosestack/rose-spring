package io.github.rosestack.spring.filter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import io.github.rosestack.util.date.DatePattern;
import io.github.rosestack.util.date.DateUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since 0.0.1
 */
public class LoggingRequestFilter extends CommonsRequestLoggingFilter {
    public static final String START_TIME = "x-request-start-time";
    private static final Logger log = LoggerFactory.getLogger(LoggingRequestFilter.class);
    private final int maxResponseTimeToLogInMs;

    private final List<String> ignoreHeaders =
            Arrays.asList("password", "authorization", "token", "accessToken", "access_token", "refreshToken");

    public LoggingRequestFilter(int maxResponseTimeToLogInMs) {
        this.maxResponseTimeToLogInMs = maxResponseTimeToLogInMs;
    }

    public void init() {
        Predicate<String> headerPredicate =
                headerName -> ObjectUtils.isEmpty(ignoreHeaders) || !ignoreHeaders.contains(headerName);
        Predicate<String> oldPredicate = getHeaderPredicate();
        setHeaderPredicate(oldPredicate == null ? headerPredicate : oldPredicate.or(headerPredicate));
    }

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        return true;
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        final Long startTime = (Long) request.getAttribute(START_TIME);
        if (startTime != null) {
            final long cost = System.currentTimeMillis() - startTime;
            message = message + ", cost " + cost + " ms";

            if (cost >= this.maxResponseTimeToLogInMs) {
                String execTime = DateUtils.format(DateUtils.fromMilliseconds(startTime), DatePattern.NORM_DATETIME_MS);
                log.warn("[SLOW_REQUEST] {} {} {} {}", execTime, request.getMethod(), request.getRequestURI(), cost);
            }
        }
        log.info(message);
    }
}
