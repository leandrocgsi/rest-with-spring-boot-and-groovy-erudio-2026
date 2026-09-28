package br.com.erudio.unittests.services

import br.com.erudio.config.AwsS3Properties
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.services.FileStorageService
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import spock.lang.Specification

import static java.nio.charset.StandardCharsets.UTF_8

class FileStorageServiceSpec extends Specification {

    static final String BUCKET = 'erudio-files-test'

    S3Client s3Client
    FileStorageService service

    def setup() {
        s3Client = Mock {
            headBucket(_ as HeadBucketRequest) >> HeadBucketResponse.builder().build()
        }
        service = new FileStorageService(s3Client, propertiesFor(BUCKET))
    }

    def 'does not recreate the bucket when it already exists'() {
        given:
        def freshClient = Mock(S3Client)

        when:
        new FileStorageService(freshClient, propertiesFor(BUCKET))

        then:
        1 * freshClient.headBucket(_ as HeadBucketRequest) >> HeadBucketResponse.builder().build()
        0 * freshClient.createBucket(_)
    }

    def 'creates the bucket when it does not exist'() {
        given:
        def freshClient = Mock(S3Client)
        freshClient.headBucket(_ as HeadBucketRequest) >> { throw NoSuchBucketException.builder().message('missing').build() }

        when:
        new FileStorageService(freshClient, propertiesFor(BUCKET))

        then:
        1 * freshClient.createBucket { CreateBucketRequest r -> r.bucket() == BUCKET }
    }

    def 'fails when the bucket cannot be verified or created'() {
        given:
        def brokenClient = Mock(S3Client)
        brokenClient.headBucket(_ as HeadBucketRequest) >> { throw NoSuchBucketException.builder().message('missing').build() }
        brokenClient.createBucket(_ as CreateBucketRequest) >> { throw S3Exception.builder().message('boom').build() }

        when:
        new FileStorageService(brokenClient, propertiesFor(BUCKET))

        then:
        def e = thrown(FileStorageException)
        e.message == 'Could not verify or create the S3 bucket where files will be stored!'
    }

    def 'stores the file with its content and returns its name'() {
        when:
        def stored = service.storeFile(file('notes.txt', 'hello upload'))

        then:
        stored == 'notes.txt'
        1 * s3Client.putObject(
            { PutObjectRequest r -> r.bucket() == BUCKET && r.key() == 'notes.txt' },
            { RequestBody b -> contentOf(b) == 'hello upload' })
    }

    def 'replaces a file that already exists'() {
        given:
        def bodies = []
        s3Client.putObject(_ as PutObjectRequest, _ as RequestBody) >> { PutObjectRequest req, RequestBody body ->
            bodies << contentOf(body)
            null
        }

        when:
        service.storeFile(file('notes.txt', 'first'))
        service.storeFile(file('notes.txt', 'second'))

        then:
        bodies == ['first', 'second']
    }

    def 'cleans redundant path segments from the name'() {
        when:
        def stored = service.storeFile(file('folder/../notes.txt', 'content'))

        then:
        stored == 'notes.txt'
        1 * s3Client.putObject({ PutObjectRequest r -> r.key() == 'notes.txt' }, _ as RequestBody)
    }

    def 'rejects a name that escapes the upload directory'() {
        when:
        service.storeFile(file('../evil.txt', 'boom'))

        then:
        def e = thrown(FileStorageException)
        e.message == 'Could not store file ../evil.txt. Please try Again!'
        e.cause instanceof FileStorageException
        e.cause.message.contains('Invalid path Sequence')
        0 * s3Client.putObject(_, _)
    }

    def 'reports a failure reading the uploaded content'() {
        given:
        MultipartFile broken = Mock {
            getOriginalFilename() >> 'broken.txt'
            getBytes() >> { throw new IOException('connection reset') }
        }

        when:
        service.storeFile(broken)

        then:
        def e = thrown(FileStorageException)
        e.message == 'Could not store file broken.txt. Please try Again!'
        e.cause instanceof IOException
    }

    def 'loads a file that was stored'() {
        given:
        s3Client.getObjectAsBytes({ GetObjectRequest r -> r.key() == 'notes.txt' }) >>
            ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), 'stored content'.getBytes(UTF_8))

        when:
        def resource = service.loadFileAsResource('notes.txt')

        then:
        resource.exists()
        resource.filename == 'notes.txt'
        new String(resource.contentAsByteArray, UTF_8) == 'stored content'
    }

    def 'fails to load a file that does not exist'() {
        given:
        s3Client.getObjectAsBytes(_ as GetObjectRequest) >> { throw NoSuchKeyException.builder().message('missing').build() }

        when:
        service.loadFileAsResource('missing.txt')

        then:
        def e = thrown(FileNotFoundException)
        e.message == 'File not found missing.txt'
    }

    private static AwsS3Properties propertiesFor(String bucket) {
        new AwsS3Properties(bucket: bucket)
    }

    private static MultipartFile file(String name, String content) {
        new MockMultipartFile('file', name, 'text/plain', content.getBytes(UTF_8))
    }

    private static String contentOf(RequestBody body) {
        new String(body.contentStreamProvider().newStream().readAllBytes(), UTF_8)
    }
}
