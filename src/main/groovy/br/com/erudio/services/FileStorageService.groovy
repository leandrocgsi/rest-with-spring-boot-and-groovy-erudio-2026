package br.com.erudio.services

import br.com.erudio.config.FileStorageConfig
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Slf4j
@Service
class FileStorageService {

    private final Path fileStorageLocation

    FileStorageService(FileStorageConfig fileStorageConfig) {
        fileStorageLocation = Paths.get(fileStorageConfig.uploadDir).toAbsolutePath().normalize()

        try {
            log.info('Creating Directories')
            Files.createDirectories(fileStorageLocation)
        } catch (Exception e) {
            log.error('Could not create the directory where files will be stored!')
            throw new FileStorageException('Could not create the directory where files will be stored!', e)
        }
    }

    String storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.originalFilename)

        try {
            if (fileName.contains('..')) {
                log.error("Sorry! Filename Contains a Invalid path Sequence $fileName")
                throw new FileStorageException("Sorry! Filename Contains a Invalid path Sequence $fileName")
            }

            log.info('Saving file in Disk')

            Path targetLocation = fileStorageLocation.resolve(fileName)
            file.inputStream.withCloseable { InputStream input ->
                Files.copy(input, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            }
            fileName
        } catch (Exception e) {
            log.error("Could not store file $fileName. Please try Again!")
            throw new FileStorageException("Could not store file $fileName. Please try Again!", e)
        }
    }

    Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize()
            Resource resource = new UrlResource(filePath.toUri())
            if (resource.exists()) {
                return resource
            }

            log.error("File not found $fileName")
            throw new FileNotFoundException("File not found $fileName")
        } catch (Exception e) {
            log.error("File not found $fileName")
            throw new FileNotFoundException("File not found $fileName", e)
        }
    }
}
