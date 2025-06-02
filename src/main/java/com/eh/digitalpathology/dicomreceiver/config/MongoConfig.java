package com.eh.digitalpathology.dicomreceiver.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Value("${spring.data.mongodb.username:#{''}}")
    private String username;
    @Value("${spring.data.mongodb.password:#{''}}")
    private String password;
    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    private MongoClient mongoClient;
    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class.getName());

    @Bean
    public MongoClient mongoClient() {
        MongoClientSettings.Builder settingBuilder = MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri));
        if (!username.isEmpty() && !password.isEmpty()) {
            settingBuilder.credential(MongoCredential.createCredential(username, "admin", password.toCharArray()));
            log.info("username for mongo db :: {}", username);
        }
        mongoClient = MongoClients.create(settingBuilder.build());
        return mongoClient;
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory factory) {
        DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, new MongoMappingContext());
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.setMapKeyDotReplacement("_"); // Replace dots with underscores
        return converter;
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient, MappingMongoConverter converter) {
        return new MongoTemplate(mongoDatabaseFactory(mongoClient), converter);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
    }

    @PreDestroy
    public void cleanUp() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
