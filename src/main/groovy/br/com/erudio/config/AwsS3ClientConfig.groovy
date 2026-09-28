package br.com.erudio.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.StringUtils
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class AwsS3ClientConfig {

    @Bean
    S3Client s3Client(AwsS3Properties properties) {
        def builder = S3Client.builder().region(Region.of(properties.region))

        if (StringUtils.hasText(properties.endpoint)) {
            builder.endpointOverride(URI.create(properties.endpoint))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey, properties.secretKey)))
        }

        builder.build()
    }
}
