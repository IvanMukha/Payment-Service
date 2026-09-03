package com.mukha.paymentservice.config;

import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class LiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @PostConstruct
    public void migrate() throws Exception {
        String changeLogPath = changeLog.replace("classpath:", "");

        try (MongoLiquibaseDatabase database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, MongoDriverProperties.class.getName(), null);
             Liquibase liquibase = new Liquibase(changeLogPath, new ClassLoaderResourceAccessor(), database)) {

            liquibase.update(new Contexts());
        }
    }

    public static class MongoDriverProperties extends Properties {
        public MongoDriverProperties() {
            put("appName", "payment-service");
        }
    }
}