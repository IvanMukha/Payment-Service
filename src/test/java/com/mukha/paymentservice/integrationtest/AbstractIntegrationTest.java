package com.mukha.paymentservice.integrationtest;

import com.mukha.paymentservice.client.RandomNumberClient;
import com.mukha.paymentservice.client.UserServiceClient;
import com.mukha.paymentservice.kafka.PaymentEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@Testcontainers
public abstract class AbstractIntegrationTest {

    private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:7.0");

    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(MONGO_IMAGE);

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        String replicaSetUrl = mongoDBContainer.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> replicaSetUrl);
        registry.add("spring.liquibase.url", () -> replicaSetUrl);
    }

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected RandomNumberClient randomNumberClient;

    @MockitoBean
    protected PaymentEventProducer paymentEventProducer;

    @MockitoBean
    protected UserServiceClient userServiceClient;
}
