package com.ratingsandreviews.config;

import liquibase.database.ObjectQuotingStrategy;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MigrationsConfiguration {
    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.json");
        liquibase.setDefaultSchema("ratings_reviews");

        liquibase.setContexts(String.valueOf(ObjectQuotingStrategy.QUOTE_ALL_OBJECTS));

        return liquibase;
    }
}