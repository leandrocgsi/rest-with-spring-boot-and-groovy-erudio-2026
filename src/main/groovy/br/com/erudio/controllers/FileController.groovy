package br.com.erudio.controllers

import br.com.erudio.controllers.docs.FileControllerDocs
import br.com.erudio.data.dto.UploadFileResponseDTO
import br.com.erudio.services.FileStorageService
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Slf4j
@RestController
@RequestMapping('/api/file/v1')
@TupleConstructor(includeFields = true, defaults = false)
class FileController implements FileControllerDocs {

    private final FileStorageService service

    @PostMapping('/uploadFile')
    @Override
    UploadFileResponseDTO uploadFile(@RequestParam('file') MultipartFile file) {
        String fileName = service.storeFile(file)

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path('/api/file/v1/downloadFile/')
            .path(fileName)
            .toUriString()

        new UploadFileResponseDTO(fileName, fileDownloadUri, file.contentType, file.size)
    }

    @PostMapping('/uploadMultipleFiles')
    @Override
    List<UploadFileResponseDTO> uploadMultipleFiles(@RequestParam('files') MultipartFile[] files) {
        files.collect { MultipartFile file -> uploadFile(file) }
    }

    @GetMapping('/downloadFile/{fileName:.+}')
    @Override
    ResponseEntity<Resource> downloadFile(@PathVariable('fileName') String fileName, HttpServletRequest request) {
        Resource resource = service.loadFileAsResource(fileName)

        String contentType = null
        try {
            contentType = request.servletContext.getMimeType(resource.getFile().absolutePath)
        } catch (Exception ignored) {
            log.error('Could not determine file type!')
        }

        String contentDisposition = "attachment; filename=\"$resource.filename\""

        ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType ?: 'application/octet-stream'))
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .body(resource)
    }
}
