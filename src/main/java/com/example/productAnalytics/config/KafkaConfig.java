package com.example.productAnalytics.config;

import com.example.productAnalytics.dto.ViewEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
    private final Environment env;
    private final Map<String, Object> commonConfigs = new HashMap<>();

    @PostConstruct
    public void init() {
        String prefix = "spring.kafka.";

        // Bootstrap servers
        set(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, prefix, commonConfigs, env);

        // Security protocol
        set(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, prefix, commonConfigs, env);

        // SASL configuration
        set(SaslConfigs.SASL_MECHANISM, prefix, commonConfigs, env);
        set(SaslConfigs.SASL_JAAS_CONFIG, prefix, commonConfigs, env);

        // SSL configuration
        set(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, prefix, commonConfigs, env);
        set(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, prefix, commonConfigs, env);
        set(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, prefix, commonConfigs, env);

        // Truststore location
//        String trustStoreLocation = env.getProperty(prefix + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
//        if (trustStoreLocation != null && trustStoreLocation.startsWith("classpath:")) {
//            try {
//                String path = trustStoreLocation.replace("classpath:", "");
//                commonConfigs.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, new ClassPathResource(path).getFile().getAbsolutePath());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            commonConfigs.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStoreLocation);
//        }
        String trustStoreLocation = KafkaTruststoreBootstrap.ensureTruststore(env);
        commonConfigs.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStoreLocation);
    }

    // Bean 1: Producer Factory
    @Bean
    public ProducerFactory<String, ViewEvent> producerFactory() {
        String prefix = "spring.kafka.producer.";
        Map<String, Object> configs = new HashMap<>(commonConfigs);

        set(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, prefix, configs, env);
        set(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, prefix, configs, env);

        configs.put(ProducerConfig.ACKS_CONFIG, "all");
        configs.put(ProducerConfig.RETRIES_CONFIG, 3);
        configs.put(ProducerConfig.LINGER_MS_CONFIG, 1);

        return new DefaultKafkaProducerFactory<>(configs);
    }

    // Bean 2: KafkaTemplate (uses ProducerFactory)
    @Bean
    public KafkaTemplate<String, ViewEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Bean 3: Consumer Factory
    @Bean
    public ConsumerFactory<String, ViewEvent> consumerFactory() {
        String prefix = "spring.kafka.consumer.";
        Map<String, Object> configs = new HashMap<>(commonConfigs);

        set(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, prefix, configs, env);
        set(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, prefix, configs, env);
        set(JsonDeserializer.TRUSTED_PACKAGES, prefix, configs, env);

        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        configs.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        configs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    // Bean 4: Kafka Listener Container Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ViewEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ViewEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(false);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler((record, e) -> log.error("Error consuming 1 record: {}, {}", record, e.toString())));

        return factory;
    }

    private static void set(String prop, String prefix, Map<String, Object> configs, Environment env) {
        if (prefix == null || prefix.isBlank()) prefix = "";
        String value = env.getProperty(prefix + prop);
        if (value != null) configs.put(prop, value);
    }

    public static class KafkaTruststoreBootstrap {

        public static String ensureTruststore(Environment env) {
            String caPemSource = env.getProperty("KAFKA_CAPEM_PATH", "/etc/secrets/ca.pem");
            String password = env.getProperty("spring.kafka." + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG);

            try {
                Path tmpDir = Paths.get("/tmp");
                Path caPemTarget = tmpDir.resolve("ca.pem");
                Path truststorePath = tmpDir.resolve("client.truststore.jks");

                // 1) Copy ca.pem into /tmp
                if (!Files.exists(caPemTarget) || Files.size(caPemTarget) == 0) {
                    Files.copy(Paths.get(caPemSource), caPemTarget, StandardCopyOption.REPLACE_EXISTING);
                }

                // 2) Generate truststore if missing
                if (!Files.exists(truststorePath) || Files.size(truststorePath) == 0) {

                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate cert;

                    try (InputStream in = Files.newInputStream(caPemTarget)) {
                        cert = (X509Certificate) cf.generateCertificate(in);
                    }

                    KeyStore ks = KeyStore.getInstance("JKS");
                    ks.load(null, null);
                    ks.setCertificateEntry("CA", cert);

                    try (OutputStream out = Files.newOutputStream(
                            truststorePath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    )) {
                        ks.store(out, password.toCharArray());
                    }
                }

                return truststorePath.toAbsolutePath().toString();

            } catch (Exception e) {
                throw new RuntimeException("Failed to bootstrap Kafka truststore in /tmp", e);
            }
        }
    }
}
