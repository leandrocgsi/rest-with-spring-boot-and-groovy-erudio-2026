package br.com.erudio.data.dto

import com.fasterxml.jackson.annotation.JsonProperty

record UploadFileResponseDTO(
        String fileName,
        String fileDownloadUri,
        String fileType,
        @JsonProperty('size') long fileSize) {}
