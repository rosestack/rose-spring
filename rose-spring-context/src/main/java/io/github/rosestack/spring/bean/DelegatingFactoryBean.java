package io.github.rosestack.spring.bean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DelegatingFactoryBean
        implements FactoryBean<Object>, InitializingBean, DisposableBean, ApplicationContextAware, BeanNameAware {
    private final Object delegate;
    private final Class<?> objectType;

    public DelegatingFactoryBean(Object delegate) {
        this.delegate = delegate;
        this.objectType = delegate.getClass();
    }

    public Object getObject() throws Exception {
        return this.delegate;
    }

    public Class<?> getObjectType() {
        return this.objectType;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.delegate instanceof InitializingBean) {
            ((InitializingBean) this.delegate).afterPropertiesSet();
        }
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (this.delegate instanceof ApplicationContextAware) {
            ((ApplicationContextAware) this.delegate).setApplicationContext(context);
        }
    }

    public void setBeanName(String name) {
        if (this.delegate instanceof BeanNameAware) {
            ((BeanNameAware) this.delegate).setBeanName(name);
        }
    }

    public void destroy() throws Exception {
        if (this.delegate instanceof DisposableBean) {
            ((DisposableBean) this.delegate).destroy();
        }
    }
}
