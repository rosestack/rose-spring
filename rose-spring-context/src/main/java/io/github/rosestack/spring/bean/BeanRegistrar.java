package io.github.rosestack.spring.bean;

import io.github.rosestack.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.*;
import org.springframework.core.AliasRegistry;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.lang.reflect.Method;

public abstract class BeanRegistrar {
    private static final Logger logger = LoggerFactory.getLogger(BeanRegistrar.class);

    /**
     * 创建通用BeanDefinition
     *
     * @param beanType Bean类型
     * @return BeanDefinition
     */
    public static AbstractBeanDefinition genericBeanDefinition(Class<?> beanType) {
        return genericBeanDefinition(beanType, ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 创建通用BeanDefinition，带构造参数
     *
     * @param beanType             Bean类型
     * @param constructorArguments 构造参数
     * @return BeanDefinition
     */
    public static AbstractBeanDefinition genericBeanDefinition(Class<?> beanType, Object... constructorArguments) {
        return genericBeanDefinition(beanType, 0, constructorArguments);
    }

    /**
     * 创建通用BeanDefinition，指定角色
     *
     * @param beanType Bean类型
     * @param role     Bean角色
     * @return BeanDefinition
     */
    public static AbstractBeanDefinition genericBeanDefinition(Class<?> beanType, int role) {
        return genericBeanDefinition(beanType, role, ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 创建通用BeanDefinition，完整参数
     *
     * @param beanType             Bean类型
     * @param role                 Bean角色
     * @param constructorArguments 构造参数
     * @return BeanDefinition
     */
    public static AbstractBeanDefinition genericBeanDefinition(
            Class<?> beanType, int role, Object[] constructorArguments) {
        BeanDefinitionBuilder beanDefinitionBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(beanType).setRole(role);
        int length = ArrayUtils.length(constructorArguments);

        for (int i = 0; i < length; ++i) {
            Object constructorArgument = constructorArguments[i];
            beanDefinitionBuilder.addConstructorArgValue(constructorArgument);
        }

        return beanDefinitionBuilder.getBeanDefinition();
    }

    /**
     * 解析Bean类型
     *
     * @param beanDefinition Bean定义
     * @return Bean类型
     */
    public static Class<?> resolveBeanType(RootBeanDefinition beanDefinition) {
        return resolveBeanType(beanDefinition, ClassUtils.getDefaultClassLoader());
    }

    /**
     * 解析Bean类型，指定类加载器
     *
     * @param beanDefinition Bean定义
     * @param classLoader    类加载器
     * @return Bean类型
     */
    public static Class<?> resolveBeanType(RootBeanDefinition beanDefinition, @Nullable ClassLoader classLoader) {
        Class<?> beanClass = null;
        Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
        if (factoryMethod == null) {
            if (beanDefinition.hasBeanClass()) {
                beanClass = beanDefinition.getBeanClass();
            } else {
                String beanClassName = beanDefinition.getBeanClassName();
                if (StringUtils.hasText(beanClassName)) {
                    ClassLoader targetClassLoader =
                            classLoader == null ? ClassUtils.getDefaultClassLoader() : classLoader;
                    beanClass = ClassUtils.resolveClassName(beanClassName, targetClassLoader);
                }
            }
        } else {
            beanClass = factoryMethod.getReturnType();
        }

        return beanClass;
    }

    /**
     * 判断是否为基础设施Bean
     *
     * @param beanDefinition Bean定义
     * @return 是否为基础设施Bean
     */
    private static boolean isInfrastructureBean(BeanDefinition beanDefinition) {
        return beanDefinition != null && BeanDefinition.ROLE_INFRASTRUCTURE == beanDefinition.getRole();
    }

    // ==================== Bean注册方法 ====================

    /**
     * 注册基础设施Bean
     *
     * @param registry Bean注册表
     * @param beanType Bean类型
     * @return 是否注册成功
     */
    public static boolean registerInfrastructureBean(BeanDefinitionRegistry registry, Class<?> beanType) {
        BeanDefinition beanDefinition = genericBeanDefinition(beanType, BeanDefinition.ROLE_INFRASTRUCTURE);
        String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, registry);
        return registerBeanDefinition(registry, beanName, beanDefinition);
    }

    /**
     * 注册基础设施Bean，指定名称
     *
     * @param registry Bean注册表
     * @param beanName Bean名称
     * @param beanType Bean类型
     * @return 是否注册成功
     */
    public static boolean registerInfrastructureBean(
            BeanDefinitionRegistry registry, String beanName, Class<?> beanType) {
        BeanDefinition beanDefinition = genericBeanDefinition(beanType, BeanDefinition.ROLE_INFRASTRUCTURE);
        return registerBeanDefinition(registry, beanName, beanDefinition);
    }

    /**
     * 注册BeanDefinition
     *
     * @param registry Bean注册表
     * @param beanType Bean类型
     * @return 是否注册成功
     */
    public static boolean registerBeanDefinition(BeanDefinitionRegistry registry, Class<?> beanType) {
        BeanDefinition beanDefinition = genericBeanDefinition(beanType);
        String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, registry);
        return registerBeanDefinition(registry, beanName, beanDefinition);
    }

    /**
     * 注册BeanDefinition，指定名称
     *
     * @param registry Bean注册表
     * @param beanName Bean名称
     * @param beanType Bean类型
     * @return 是否注册成功
     */
    public static boolean registerBeanDefinition(BeanDefinitionRegistry registry, String beanName, Class<?> beanType) {
        BeanDefinition beanDefinition = genericBeanDefinition(beanType);
        return registerBeanDefinition(registry, beanName, beanDefinition);
    }

    /**
     * 注册BeanDefinition，带构造参数
     *
     * @param registry             Bean注册表
     * @param beanName             Bean名称
     * @param beanType             Bean类型
     * @param constructorArguments 构造参数
     * @return 是否注册成功
     */
    public static boolean registerBeanDefinition(
            BeanDefinitionRegistry registry, String beanName, Class<?> beanType, Object... constructorArguments) {
        BeanDefinition beanDefinition = genericBeanDefinition(beanType, constructorArguments);
        return registerBeanDefinition(registry, beanName, beanDefinition);
    }

    /**
     * 注册BeanDefinition，指定角色
     *
     * @param registry Bean注册表
     * @param beanName Bean名称
     * @param beanType Bean类型
     * @param role     Bean角色
     * @return 是否注册成功
     */
    public static boolean registerBeanDefinition(
            BeanDefinitionRegistry registry, String beanName, Class<?> beanType, int role) {
        BeanDefinition beanDefinition = genericBeanDefinition(beanType, role);
        return registerBeanDefinition(registry, beanName, beanDefinition);
    }

    public static final boolean registerBeanDefinition(
            BeanDefinitionRegistry registry, String beanName, BeanDefinition beanDefinition) {
        return registerBeanDefinition(registry, beanName, beanDefinition, false);
    }

    public static final boolean registerBeanDefinition(
            BeanDefinitionRegistry registry,
            String beanName,
            BeanDefinition beanDefinition,
            boolean allowBeanDefinitionOverriding) {
        boolean registered = false;
        if (!allowBeanDefinitionOverriding && registry.containsBeanDefinition(beanName)) {
            BeanDefinition oldBeanDefinition = registry.getBeanDefinition(beanName);
            if (logger.isWarnEnabled()) {
                logger.warn("The bean[name : '{}'] definition [{}] was registered!", beanName, oldBeanDefinition);
            }
        } else {
            try {
                registry.registerBeanDefinition(beanName, beanDefinition);
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "The bean[name : '{}' , role : {}] definition [{}] has been registered.",
                            new Object[]{beanName, beanDefinition.getRole(), beanDefinition});
                }

                registered = true;
            } catch (BeanDefinitionStoreException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(
                            "The bean[name : '{}' , role : {}] definition [{}] can't be registered ",
                            new Object[]{beanName, beanDefinition.getRole(), e});
                }

                registered = false;
            }
        }

        return registered;
    }

    public static void registerSingleton(SingletonBeanRegistry registry, String beanName, Object bean) {
        registry.registerSingleton(beanName, bean);
        if (logger.isInfoEnabled()) {
            logger.info(
                    "The singleton bean [name : '{}' , instance : {}] has been registered into the BeanFactory.",
                    beanName,
                    bean);
        }
    }

    public static boolean hasAlias(AliasRegistry registry, String beanName, String alias) {
        return StringUtils.hasText(beanName)
                && StringUtils.hasText(alias)
                && ObjectUtils.containsElement(registry.getAliases(beanName), alias);
    }

    public static int registerSpringFactoriesBeans(BeanDefinitionRegistry registry, Class<?>... factoryClasses) {
        int count = 0;
        ClassLoader classLoader = registry.getClass().getClassLoader();

        for (int i = 0; i < factoryClasses.length; ++i) {
            Class<?> factoryClass = factoryClasses[i];

            for (String factoryImplClassName : SpringFactoriesLoader.loadFactoryNames(factoryClass, classLoader)) {
                Class<?> factoryImplClass = ClassUtils.resolveClassName(factoryImplClassName, classLoader);
                String beanName = Introspector.decapitalize(ClassUtils.getShortName(factoryImplClassName));
                if (registerInfrastructureBean(registry, beanName, factoryImplClass)) {
                    ++count;
                } else if (logger.isWarnEnabled()) {
                    logger.warn(String.format(
                            "The Factory Class bean[%s] has been registered with bean name[%s]",
                            factoryImplClassName, beanName));
                }
            }
        }

        return count;
    }

    /**
     * 注册FactoryBean
     *
     * @param registry Bean注册表
     * @param beanName Bean名称
     * @param bean     Bean实例
     */
    public static final void registerFactoryBean(BeanDefinitionRegistry registry, String beanName, Object bean) {
        AbstractBeanDefinition beanDefinition = genericBeanDefinition(DelegatingFactoryBean.class, new Object[]{bean});
        beanDefinition.setSource(bean);
        registerBeanDefinition(registry, beanName, beanDefinition);
    }

    /**
     * 注册Bean实例
     *
     * @param registry Bean注册表
     * @param beanName Bean名称
     * @param bean     Bean实例
     */
    public static void registerBean(BeanDefinitionRegistry registry, String beanName, Object bean) {
        registerBean(registry, beanName, bean, false);
    }

    /**
     * 注册Bean实例，可指定是否为主要Bean
     *
     * @param registry Bean注册表
     * @param beanName Bean名称
     * @param bean     Bean实例
     * @param primary  是否为主要Bean
     */
    public static void registerBean(BeanDefinitionRegistry registry, String beanName, Object bean, boolean primary) {
        Class<?> beanClass = AopUtils.getTargetClass(bean);
        AbstractBeanDefinition beanDefinition = genericBeanDefinition(beanClass);
        beanDefinition.setInstanceSupplier(() -> bean);
        beanDefinition.setPrimary(primary);
        registerBeanDefinition(registry, beanName, beanDefinition);
    }
}
