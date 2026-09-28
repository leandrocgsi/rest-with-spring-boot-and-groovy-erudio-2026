package br.com.erudio.services

import br.com.erudio.config.AwsS3Properties
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import groovy.util.logging.Slf4j
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Slf4j
@Service
class FileStorageService {

    private final S3Client s3Client
    private final String bucket

    FileStorageService(S3Client s3Client, AwsS3Properties properties) {
        this.s3Client = s3Client
        this.bucket = properties.bucket
        createBucketIfMissing()
    }

    private void createBucketIfMissing() {
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
            } catch (NoSuchBucketException e) {
                log.info("Creating S3 bucket $bucket")
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
            }
        } catch (Exception e) {
            log.error('Could not verify or create the S3 bucket where files will be stored!')
            throw new FileStorageException('Could not verify or create the S3 bucket where files will be stored!', e)
        }
    }

    String storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.originalFilename)

        try {
            if (fileName.contains('..')) {
                log.error("Sorry! Filename Contains a Invalid path Sequence $fileName")
                throw new FileStorageException("Sorry! Filename Contains a Invalid path Sequence $fileName")
            }

            log.info('Saving file in S3')

            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.contentType)
                .build()

            s3Client.putObject(request, RequestBody.fromBytes(file.bytes))
            fileName
        } catch (Exception e) {
            log.error("Could not store file $fileName. Please try Again!")
            throw new FileStorageException("Could not store file $fileName. Please try Again!", e)
        }
    }

    Resource loadFileAsResource(String fileName) {
        try {
            byte[] content = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(fileName).build()).asByteArray()

            new ByteArrayResource(content) {
                @Override
                String getFilename() {
                    fileName
                }

                @Override
                File getFile() {
                    new File(fileName)
                }

                @Override
                boolean exists() {
                    true
                }
            }
        } catch (Exception e) {
            log.error("File not found $fileName")
            throw new FileNotFoundException("File not found $fileName", e)
        }
    }
}
