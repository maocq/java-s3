package co.com.bancolombia.s3.config;

import co.com.bancolombia.s3.config.model.S3ConnectionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;

@Configuration
public class S3Config {

    @Profile({"dev", "cer", "pdn"})
    @Bean
    public S3AsyncClient s3AsyncClient(S3ConnectionProperties s3Properties) {
        return S3AsyncClient.builder()
                .region(Region.of(s3Properties.getRegion()))
                .build();
    }

    @Profile("local")
    @Bean
    public S3AsyncClient localS3AsyncClient(S3ConnectionProperties s3Properties) {
        return S3AsyncClient.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(ProfileCredentialsProvider.create("default"))
                .endpointOverride(URI.create(s3Properties.getEndpoint()))
                .build();
    }
}
