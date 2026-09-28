package br.com.erudio.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = 'aws.s3')
class AwsS3Properties {

    String bucket
    String region
    String endpoint
    String accessKey
    String secretKey
}
