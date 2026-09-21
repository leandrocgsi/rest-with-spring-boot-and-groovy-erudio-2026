package br.com.erudio.data.dto

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@JsonPropertyOrder(['fileName', 'fileDownloadUri', 'fileType', 'size'])
@TupleConstructor
@EqualsAndHashCode
class UploadFileResponseDTO implements Serializable {

    String fileName
    String fileDownloadUri
    String fileType
    long size
}
