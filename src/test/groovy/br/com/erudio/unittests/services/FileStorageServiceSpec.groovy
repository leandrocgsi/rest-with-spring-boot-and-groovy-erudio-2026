package br.com.erudio.unittests.services

import br.com.erudio.config.FileStorageConfig
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.services.FileStorageService
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static java.nio.charset.StandardCharsets.UTF_8

class FileStorageServiceSpec extends Specification {

    @TempDir
    Path tempDir

    Path uploadDir
    FileStorageService service

    def setup() {
        uploadDir = tempDir.resolve('uploads')
        service = new FileStorageService(configFor(uploadDir))
    }

    def 'creates the upload directory when it does not exist'() {
        given:
        def nested = tempDir.resolve('a').resolve('b').resolve('uploads')

        when:
        new FileStorageService(configFor(nested))

        then:
        Files.isDirectory(nested)
    }

    def 'fails when the upload directory cannot be created'() {
        given:
        def regularFile = Files.writeString(tempDir.resolve('not-a-directory'), 'x')

        when:
        new FileStorageService(configFor(regularFile.resolve('uploads')))

        then:
        def e = thrown(FileStorageException)
        e.message == 'Could not create the directory where files will be stored!'
    }

    def 'stores the file with its content and returns its name'() {
        when:
        def stored = service.storeFile(file('notes.txt', 'hello upload'))

        then:
        stored == 'notes.txt'
        Files.readString(uploadDir.resolve('notes.txt')) == 'hello upload'
    }

    def 'replaces a file that already exists'() {
        when:
        service.storeFile(file('notes.txt', 'first'))
        service.storeFile(file('notes.txt', 'second'))

        then:
        Files.readString(uploadDir.resolve('notes.txt')) == 'second'
    }

    def 'cleans redundant path segments from the name'() {
        when:
        def stored = service.storeFile(file('folder/../notes.txt', 'content'))

        then:
        stored == 'notes.txt'
        Files.exists(uploadDir.resolve('notes.txt'))
    }

    def 'rejects a name that escapes the upload directory'() {
        when:
        service.storeFile(file('../evil.txt', 'boom'))

        then:
        def e = thrown(FileStorageException)
        e.message == 'Could not store file ../evil.txt. Please try Again!'
        e.cause instanceof FileStorageException
        e.cause.message.contains('Invalid path Sequence')
        !Files.exists(tempDir.resolve('evil.txt'))
    }

    def 'reports a failure reading the uploaded content'() {
        given:
        MultipartFile broken = Mock {
            getOriginalFilename() >> 'broken.txt'
            getInputStream() >> { throw new IOException('connection reset') }
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
        service.storeFile(file('notes.txt', 'stored content'))

        when:
        def resource = service.loadFileAsResource('notes.txt')

        then:
        resource.exists()
        resource.filename == 'notes.txt'
        new String(resource.contentAsByteArray, UTF_8) == 'stored content'
    }

    def 'fails to load a file that does not exist'() {
        when:
        service.loadFileAsResource('missing.txt')

        then:
        def e = thrown(FileNotFoundException)
        e.message == 'File not found missing.txt'
    }

    private static FileStorageConfig configFor(Path directory) {
        new FileStorageConfig(uploadDir: directory.toString())
    }

    private static MultipartFile file(String name, String content) {
        new MockMultipartFile('file', name, 'text/plain', content.getBytes(UTF_8))
    }
}
