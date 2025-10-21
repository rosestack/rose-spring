package io.github.rosestack.spring.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.NonNull;
import org.springframework.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 简化的 Spring 上下文工具类
 *
 * <p>提供常用的 Spring Bean 获取和操作功能，替代复杂的 SpringBeanUtils
 *
 * @author chensoul
 * @since 1.0.0
 */
@Slf4j
@Lazy
public class SpringContextUtils implements ApplicationContextAware, DisposableBean {

    private static ApplicationContext applicationContext;

    /**
     * 获取 ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        SpringContextUtils.applicationContext = applicationContext;
        log.debug("SpringContextUtils 初始化完成");
    }

    /**
     * 获取应用名称
     */
    public static String getApplicationName() {
        return isInitialized() ? applicationContext.getApplicationName() : "unknown";
    }

    /**
     * 获取激活的配置文件
     */
    public static String[] getActiveProfiles() {
        return isInitialized() ? applicationContext.getEnvironment().getActiveProfiles() : new String[0];
    }

    /**
     * 获取第一个激活的配置文件
     */
    public static String getActiveProfile() {
        String[] activeProfiles = getActiveProfiles();
        return ObjectUtils.isEmpty(activeProfiles) ? null : activeProfiles[0];
    }

    /**
     * 根据类型获取 Bean
     */
    public static <T> T getBean(Class<T> beanClass) throws BeansException {
        return isInitialized() ? applicationContext.getBean(beanClass) : null;
    }

    /**
     * 根据名称和类型获取 Bean
     */
    public static <T> T getBean(String name, Class<T> beanClass) throws BeansException {
        return isInitialized() ? applicationContext.getBean(name, beanClass) : null;
    }

    /**
     * 根据名称获取 Bean
     */
    public static Object getBean(String name) throws BeansException {
        return isInitialized() ? applicationContext.getBean(name) : null;
    }

    /**
     * 获取指定类型的所有 Bean
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return isInitialized() ? applicationContext.getBeansOfType(type) : Collections.emptyMap();
    }

    /**
     * 获取指定类型的所有 Bean，并按 @Order 注解排序
     */
    public static <T> List<T> getSortedBeans(Class<T> type) {
        if (applicationContext == null) {
            return Collections.emptyList();
        }
        Map<String, T> beansOfType = getBeansOfType(type);
        List<T> beansList = new ArrayList<>(beansOfType.values());
        AnnotationAwareOrderComparator.sort(beansList);
        return Collections.unmodifiableList(beansList);
    }

    /**
     * 发布事件
     */
    public static void publishEvent(Object event) {
        if (isInitialized()) {
            applicationContext.publishEvent(event);
        }
    }

    /**
     * 获取环境变量
     */
    public static String getProperty(String key) {
        return isInitialized() ? applicationContext.getEnvironment().getProperty(key) : null;
    }

    /**
     * 获取环境变量，如果不存在则返回默认值
     */
    public static String getProperty(String key, String defaultValue) {
        return isInitialized() ? applicationContext.getEnvironment().getProperty(key, defaultValue) : defaultValue;
    }

    /**
     * 获取指定类型的环境变量
     */
    public static <T> T getProperty(String key, Class<T> targetType) {
        return isInitialized() ? applicationContext.getEnvironment().getProperty(key, targetType) : null;
    }

    /**
     * 获取指定类型的环境变量，如果不存在则返回默认值
     */
    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return isInitialized()
                ? applicationContext.getEnvironment().getProperty(key, targetType, defaultValue)
                : defaultValue;
    }

    /**
     * 检查是否存在指定的环境变量
     */
    public static boolean containsProperty(String key) {
        return isInitialized() && applicationContext.getEnvironment().containsProperty(key);
    }

    /**
     * 检查应用上下文是否已初始化
     */
    public static boolean isInitialized() {
        return applicationContext != null;
    }

    /**
     * 获取 Bean 的数量
     */
    public static int getBeanDefinitionCount() {
        return isInitialized() ? applicationContext.getBeanDefinitionCount() : 0;
    }

    /**
     * 获取所有 Bean 的名称
     */
    public static String[] getBeanDefinitionNames() {
        return isInitialized() ? applicationContext.getBeanDefinitionNames() : new String[0];
    }

    /**
     * 调用 Aware 接口方法
     *
     * @param bean        Bean 实例
     * @param beanFactory Bean 工厂
     */
    public static void invokeAwareInterfaces(Object bean, BeanFactory beanFactory) {
        if (bean instanceof ApplicationContextAware && beanFactory instanceof ApplicationContext) {
            ((ApplicationContextAware) bean).setApplicationContext((ApplicationContext) beanFactory);
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(beanFactory);
        }
        if (bean instanceof EnvironmentAware && beanFactory instanceof ApplicationContext) {
            ((EnvironmentAware) bean).setEnvironment(((ApplicationContext) beanFactory).getEnvironment());
        }
    }

    @Override
    public void destroy() throws Exception {
        log.debug("SpringContextUtils 正在销毁");
        applicationContext = null;
    }
}
