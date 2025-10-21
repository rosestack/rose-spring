package io.github.rosestack.spring.desensitization;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import io.github.rosestack.core.util.SensitiveUtils;
import io.github.rosestack.spring.annotation.Sensitive;
import io.github.rosestack.spring.expression.SpringExpressionResolver;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since 0.0.1
 */
public class FieldSensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private Sensitive sensitive;

    public FieldSensitiveSerializer(Sensitive sensitive) {
        this.sensitive = sensitive;
    }

    public FieldSensitiveSerializer() {}

    private String handler(Sensitive sensitive, String origin) {
        Object disable = SpringExpressionResolver.getInstance().resolve(sensitive.expression());
        if (Boolean.TRUE.equals(disable)) {
            return origin;
        }

        return SensitiveUtils.mask(origin, sensitive.type());
    }

    @Override
    public void serialize(
            final String origin, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeString(handler(sensitive, origin));
    }

    @Override
    public JsonSerializer<?> createContextual(
            final SerializerProvider serializerProvider, final BeanProperty beanProperty) throws JsonMappingException {
        Sensitive annotation = beanProperty.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation)
                && Objects.equals(String.class, beanProperty.getType().getRawClass())) {
            return new FieldSensitiveSerializer(annotation);
        }
        return serializerProvider.findValueSerializer(beanProperty.getType(), beanProperty);
    }
}
