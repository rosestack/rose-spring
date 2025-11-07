/*
 * Copyright © 2025 rosestack.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.rosestack.spring.expression;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

import io.github.rosestack.util.StringUtils;
import io.github.rosestack.util.date.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import io.github.rosestack.spring.util.SpringContextUtils;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since 0.0.1
 */
public class SpringExpressionResolver implements Function<Object, Object> {
    private static final Logger log = LoggerFactory.getLogger(SpringExpressionResolver.class);

    private static final ParserContext PARSER_CONTEXT = new TemplateParserContext("${", "}");
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, SpringExpressionResolver.class.getClassLoader()));
    private static SpringExpressionResolver INSTANCE;
    private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

    private SpringExpressionResolver() {
        Properties properties = System.getProperties();
        evaluationContext.setVariable("systemProperties", properties);
        evaluationContext.setVariable("sysProps", properties);

        Map<String, String> environment = System.getenv();
        evaluationContext.setVariable("environmentVars", environment);
        evaluationContext.setVariable("environmentVariables", environment);
        evaluationContext.setVariable("envVars", environment);
        evaluationContext.setVariable("env", environment);

        evaluationContext.setVariable("tempDir", System.getProperty("java.io.tmpdir"));
        evaluationContext.setVariable("zoneId", ZoneId.systemDefault().getId());

        evaluationContext.setBeanResolver(new BeanFactoryResolver(SpringContextUtils.getApplicationContext()));
    }

    /**
     * Gets instance of the resolver as a singleton.
     *
     * @return the instance
     */
    public static synchronized SpringExpressionResolver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpringExpressionResolver();
        }
        INSTANCE.initializeDynamicVariables();
        return INSTANCE;
    }

    /**
     * Resolve string.
     *
     * @param value the value
     * @return the string
     */
    public Object resolve(final String value) {
        if (StringUtils.isNotBlank(value)) {
            log.trace("Parsing expression as [{}]", value);
            Expression expression = EXPRESSION_PARSER.parseExpression(value, PARSER_CONTEXT);
            Object result = expression.getValue(evaluationContext);
            log.trace("Parsed expression result is [{}]", result);
            return result;
        }
        return value;
    }

    @Override
    public Object apply(final Object o) {
        return resolve(o.toString());
    }

    private void initializeDynamicVariables() {
//        evaluationContext.setVariable("randomNumber2", RandomStringUtils.randomNumeric(2));
//        evaluationContext.setVariable("randomNumber4", RandomStringUtils.randomNumeric(4));
//        evaluationContext.setVariable("randomNumber6", RandomStringUtils.randomNumeric(6));
//        evaluationContext.setVariable("randomNumber8", RandomStringUtils.randomNumeric(8));
//        evaluationContext.setVariable("randomString4", RandomStringUtils.randomAlphabetic(4));
//        evaluationContext.setVariable("randomString6", RandomStringUtils.randomAlphabetic(6));
//        evaluationContext.setVariable("randomString8", RandomStringUtils.randomAlphabetic(8));
        evaluationContext.setVariable("uuid", UUID.randomUUID().toString());

        evaluationContext.setVariable(
                "localStartWorkDay", DateUtils.getStartWorkDay().toString());
        evaluationContext.setVariable(
                "localEndWorkDay", DateUtils.getEndWorkDay().toString());
        evaluationContext.setVariable("localStartDay", DateUtils.getStartDay().toString());
        evaluationContext.setVariable("localEndDay", DateUtils.getEndDay().toString());
        evaluationContext.setVariable(
                "localDateTime", DateUtils.getLocalDateTime().toString());
        evaluationContext.setVariable(
                "localDateTimeUtc", DateUtils.getLocalDateTimeUTC().toString());
        evaluationContext.setVariable(
                "localDate", LocalDate.now(ZoneId.systemDefault()).toString());
        evaluationContext.setVariable(
                "localDateUtc", LocalDate.now(Clock.systemUTC()).toString());
        evaluationContext.setVariable(
                "zonedDateTime", ZonedDateTime.now(ZoneId.systemDefault()).toString());
        evaluationContext.setVariable(
                "zonedDateTimeUtc", ZonedDateTime.now(Clock.systemUTC()).toString());
    }
}
