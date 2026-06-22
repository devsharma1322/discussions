package com.anoncircles.graph.config;

import com.anoncircles.graph.model.DescriptionGenerated;
import com.anoncircles.graph.model.DescriptionUnavailable;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.scalars.ExtendedScalars;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * GraphQL runtime configuration:
 *   - depth-limit instrumentation, toggled via {@code graph.query.depth-limit.enabled}
 *     (off by default in dev, on in {@code application-prod.yml}) to prevent DoS via deep queries
 *   - complexity-limit instrumentation, toggled via {@code graph.query.complexity-limit.enabled}
 *     (off by default in dev, on in prod) — bounds total field count across a query
 *   - Scalars: {@code UUID} + {@code DateTime} from {@code graphql-java-extended-scalars}
 *   - TypeResolver for the {@code GenerateDescriptionResult} union so Spring for
 *     GraphQL can emit the correct {@code __typename} for each branch
 *
 * Introspection is toggled via {@code spring.graphql.schema.introspection.enabled}
 * in application.yml / application-prod.yml.
 */
@Configuration
public class GraphQlConfig {

    @Bean
    @ConditionalOnProperty(name = "graph.query.depth-limit.enabled", havingValue = "true")
    public Instrumentation depthLimitInstrumentation(
            @Value("${graph.query.depth-limit.max:7}") int maxDepth) {
        return new MaxQueryDepthInstrumentation(maxDepth);
    }

    @Bean
    @ConditionalOnProperty(name = "graph.query.complexity-limit.enabled", havingValue = "true")
    public Instrumentation complexityLimitInstrumentation(
            @Value("${graph.query.complexity-limit.max:200}") int maxComplexity) {
        return new MaxQueryComplexityInstrumentation(maxComplexity);
    }

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.DateTime)
                .type("GenerateDescriptionResult", typeWiring ->
                        typeWiring.typeResolver(env -> {
                            Object obj = env.getObject();
                            if (obj instanceof DescriptionGenerated) {
                                return env.getSchema().getObjectType("DescriptionGenerated");
                            }
                            if (obj instanceof DescriptionUnavailable) {
                                return env.getSchema().getObjectType("DescriptionUnavailable");
                            }
                            throw new IllegalStateException(
                                    "Unknown GenerateDescriptionResult variant: " + obj);
                        }));
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

