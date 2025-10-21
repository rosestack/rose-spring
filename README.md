# Rose Spring

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/rosestack/rose-spring)
[![Maven Build](https://github.com/rosestack/rose-spring/actions/workflows/maven-build.yml/badge.svg)](https://github.com/rosestack/rose-spring/actions/workflows/maven-build.yml)
[![Maven](https://img.shields.io/maven-central/v/io.github.rosestack/rose-spring.svg)](https://central.sonatype.com/artifact/io.github.rosestack/rose-spring)
[![Codecov](https://codecov.io/gh/rosestack/rose-spring/branch/main/graph/badge.svg)](https://app.codecov.io/gh/rosestack/rose-spring)
![License](https://img.shields.io/github/license/rosestack/rose-spring.svg)
[![Average time to resolve an issue](https://isitmaintained.com/badge/resolution/rosestack/rose-spring.svg)](https://isitmaintained.com/project/rosestack/rose-spring "Average time to resolve an issue")
[![Percentage of issues still open](https://isitmaintained.com/badge/open/rosestack/rose-spring.svg)](https://isitmaintained.com/project/rosestack/rose-spring "Percentage of issues still open")

Rose Spring 是一个轻量级 Spring 增强工具集，提供 Spring 应用开发中常用的工具类、过滤器和组件。

## 核心特性

- **Spring 上下文工具**：简化 Bean 获取和操作
- **数据脱敏**：字段级数据脱敏注解和序列化器
- **请求过滤器**：XSS 防护、请求日志、请求缓存
- **表达式解析**：Spring EL 表达式工具
- **YAML 配置**：自定义 YAML 属性源工厂
- **线程工具**：Spring 异步线程池管理

## 快速开始

### Maven 依赖管理

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.rosestack</groupId>
      <artifactId>rose-spring-bom</artifactId>
      <version>0.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 添加模块依赖

```xml
<dependency>
  <groupId>io.github.rosestack</groupId>
  <artifactId>rose-spring-context</artifactId>
</dependency>
```

## 功能模块

### 1. Spring 上下文工具 (SpringContextUtils)

简化 Spring Bean 的获取和操作：

```java
// 获取 Bean
UserService userService = SpringContextUtils.getBean(UserService.class);

// 获取多个 Bean（自动排序）
List<MyInterface> beans = SpringContextUtils.getOrderedBeans(MyInterface.class);

// 获取应用名称
String appName = SpringContextUtils.getApplicationName();

// 获取激活的配置文件
String[] profiles = SpringContextUtils.getActiveProfiles();
```

### 2. 数据脱敏 (@Sensitive)

字段级数据脱敏，支持多种脱敏类型：

```java
public class UserDTO {
    @Sensitive(type = SensitiveType.MOBILE_PHONE)
    private String phone;  // 输出: 138****5678
    
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;  // 输出: abc***@gmail.com
    
    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard; // 输出: 110101****1234
    
    @Sensitive(type = SensitiveType.CUSTOM, prefixKeep = 3, suffixKeep = 2)
    private String custom; // 自定义脱敏规则
    
    @Sensitive(expression = "#{@myService.maskData(#value)}")
    private String advanced; // 支持 SpEL 表达式
}
```

### 3. 请求过滤器

#### XSS 防护过滤器

```java
@Bean
public FilterRegistrationBean<XssRequestFilter> xssFilter() {
    FilterRegistrationBean<XssRequestFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new XssRequestFilter(new String[]{"/api/public/*"}));
    registration.setOrder(1);
    return registration;
}
```

#### 请求日志过滤器

```java
@Bean
public FilterRegistrationBean<LoggingRequestFilter> loggingFilter() {
    FilterRegistrationBean<LoggingRequestFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new LoggingRequestFilter(new String[]{"/health", "/metrics"}));
    registration.setOrder(2);
    return registration;
}
```

#### 请求缓存过滤器

用于重复读取请求体（如日志记录、签名验证）：

```java
@Bean
public FilterRegistrationBean<CachingRequestFilter> cachingFilter() {
    FilterRegistrationBean<CachingRequestFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new CachingRequestFilter(new String[]{"/api/*"}));
    registration.setOrder(0); // 需要最先执行
    return registration;
}
```

### 4. Bean 注册工具 (BeanRegistrar)

动态注册 Spring Bean：

```java
@Autowired
private BeanRegistrar beanRegistrar;

public void registerBean() {
    // 注册单例 Bean
    MyService service = new MyService();
    beanRegistrar.registerSingleton("myService", service);
    
    // 注册 FactoryBean
    beanRegistrar.registerFactoryBean("myFactory", MyFactoryBean.class);
}
```

### 5. Spring EL 表达式解析

```java
@Autowired
private SpringExpressionResolver expressionResolver;

public void evaluateExpression() {
    // 解析表达式
    String result = expressionResolver.resolve("#{@userService.getName()}");
    
    // 带参数解析
    Map<String, Object> context = Map.of("user", user);
    String greeting = expressionResolver.resolve("Hello #{#user.name}", context);
}
```

### 6. YAML 属性源工厂

支持自定义 YAML 配置文件加载：

```java
@Configuration
@PropertySource(value = "classpath:custom-config.yml", factory = YmlPropertySourceFactory.class)
public class CustomConfig {
    @Value("${custom.property}")
    private String customProperty;
}
```

### 7. Servlet 工具类

```java
// 获取当前请求
HttpServletRequest request = ServletUtils.getRequest();

// 获取请求参数
String param = ServletUtils.getParameter("paramName");

// 判断是否 Ajax 请求
boolean isAjax = ServletUtils.isAjaxRequest(request);

// 获取客户端 IP
String ip = ServletUtils.getClientIP(request);
```

### 8. Spring 线程工具

```java
// 获取 Spring 管理的线程池
ThreadPoolTaskExecutor executor = SpringThreadUtils.getThreadPoolTaskExecutor();

// 异步执行任务
SpringThreadUtils.execute(() -> {
    // 异步任务逻辑
});
```

## 环境要求

- Java 17+
- Spring Framework 6.x / Spring Boot 3.x
- Maven 3.9+

## 构建项目

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 生成覆盖率报告
mvn jacoco:report

# 代码格式化
mvn spotless:apply
```

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。