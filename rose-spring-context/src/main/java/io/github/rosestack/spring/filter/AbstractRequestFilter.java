package io.github.rosestack.spring.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.rosestack.spring.util.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Rose 基础过滤器抽象类
 *
 * <p>提供基础的路径排除机制，不依赖特定的配置类
 *
 * @author rosestack
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractRequestFilter extends OncePerRequestFilter {
    /**
     * 默认排除路径
     */
    public static final String[] DEFAULT_EXCLUDE_PATHS = {
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/favicon.ico",
        "/error",
        "/static/**",
        "/public/**"
    };

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> excludePathsCache = new HashSet<>();

    protected AbstractRequestFilter(String[] excludePaths) {
        initializeExcludePaths(excludePaths);
    }

    /**
     * 检查请求路径是否匹配排除路径模式
     *
     * @param requestPath  请求路径
     * @param excludePaths 排除路径集合
     * @return true 如果匹配排除路径，false 否则
     */
    public static boolean shouldExcludePath(String requestPath, Set<String> excludePaths) {
        if (ObjectUtils.isEmpty(excludePaths) || ObjectUtils.isEmpty(requestPath)) {
            return false;
        }

        return excludePaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, requestPath));
    }

    public static boolean shouldExcludePath(String requestPath) {
        return shouldExcludePath(requestPath, excludePathsCache);
    }

    /**
     * 初始化排除路径缓存
     */
    private void initializeExcludePaths(String[] excludePaths) {
        // 添加默认排除路径
        excludePathsCache.addAll(Arrays.asList(DEFAULT_EXCLUDE_PATHS));

        // 添加全局排除路径
        if (!ObjectUtils.isEmpty(excludePaths)) {
            excludePathsCache.addAll(Arrays.asList(excludePaths));
        }

        log.info("过滤器 {} 初始化排除路径: {}", getClass().getSimpleName(), excludePathsCache);
    }

    /**
     * 获取过滤器特定的排除路径
     *
     * <p>子类可以重写此方法来添加自己特有的排除路径
     *
     * @return 过滤器特定的排除路径数组
     */
    protected String[] getFilterSpecificExcludePaths() {
        return new String[0];
    }

    /**
     * 判断是否应该跳过过滤
     *
     * <p>检查请求路径是否匹配排除路径模式
     *
     * @param request HTTP 请求
     * @return true 如果应该跳过过滤，false 否则
     */
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = ServletUtils.getRequestPath(request);

        Set<String> excludePaths = getExcludePaths();
        // 添加过滤器特定的排除路径
        String[] filterSpecificPaths = getFilterSpecificExcludePaths();
        if (!ObjectUtils.isEmpty(filterSpecificPaths)) {
            excludePaths.addAll(Arrays.asList(filterSpecificPaths));
        }

        return shouldExcludePath(requestPath, excludePaths);
    }

    /**
     * 获取当前所有排除路径
     *
     * @return 排除路径集合的副本
     */
    protected Set<String> getExcludePaths() {
        return new HashSet<>(excludePathsCache);
    }
}
