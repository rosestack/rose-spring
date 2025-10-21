package io.github.rosestack.spring.factory;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import lombok.extern.slf4j.Slf4j;

/**
 * yml 配置源工厂
 */
@Slf4j
public class YmlPropertySourceFactory extends DefaultPropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        String sourceName = resource.getResource().getFilename();

        // 如果是 .properties 文件，使用父类处理
        if (StringUtils.isNotBlank(sourceName) && sourceName.endsWith(".properties")) {
            return super.createPropertySource(name, resource);
        }

        // 处理 .yml 和 .yaml 文件
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        factory.afterPropertiesSet();

        log.info("Loading yaml file: {}", sourceName);

        Properties properties = factory.getObject();
        if (properties == null) {
            properties = new Properties();
        }

        String actualSourceName = sourceName != null ? sourceName : "yaml-property-source";
        return new PropertiesPropertySource(actualSourceName, properties);
    }
}
