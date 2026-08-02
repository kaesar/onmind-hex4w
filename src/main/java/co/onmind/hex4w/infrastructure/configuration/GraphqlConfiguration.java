package co.onmind.hex4w.infrastructure.configuration;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphqlConfiguration {

    @Bean
    public RuntimeWiringConfigurer jsonScalarConfigurer() {
        GraphQLScalarType jsonScalar = GraphQLScalarType.newScalar()
                .name("JSON")
                .coercing(new Coercing<Object, Object>() {
                    @Override
                    public Object serialize(Object dataFetcherValue) {
                        return dataFetcherValue;
                    }

                    @Override
                    public Object parseValue(Object input) {
                        return input;
                    }

                    @Override
                    public Object parseLiteral(Object input) {
                        return input;
                    }
                })
                .build();

        return builder -> builder.scalar(jsonScalar);
    }
}
