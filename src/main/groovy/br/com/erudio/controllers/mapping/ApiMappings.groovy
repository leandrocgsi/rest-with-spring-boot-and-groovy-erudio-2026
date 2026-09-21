package br.com.erudio.controllers.mapping

import groovy.transform.AnnotationCollector
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping

@GetMapping(produces = [
    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
    org.springframework.http.MediaType.APPLICATION_XML_VALUE,
    org.springframework.http.MediaType.APPLICATION_YAML_VALUE])
@AnnotationCollector
@interface ApiGet {}

@PostMapping(
    consumes = [
        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
        org.springframework.http.MediaType.APPLICATION_XML_VALUE,
        org.springframework.http.MediaType.APPLICATION_YAML_VALUE],
    produces = [
        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
        org.springframework.http.MediaType.APPLICATION_XML_VALUE,
        org.springframework.http.MediaType.APPLICATION_YAML_VALUE])
@AnnotationCollector
@interface ApiPost {}

@PutMapping(
    consumes = [
        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
        org.springframework.http.MediaType.APPLICATION_XML_VALUE,
        org.springframework.http.MediaType.APPLICATION_YAML_VALUE],
    produces = [
        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
        org.springframework.http.MediaType.APPLICATION_XML_VALUE,
        org.springframework.http.MediaType.APPLICATION_YAML_VALUE])
@AnnotationCollector
@interface ApiPut {}

@PatchMapping(produces = [
    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
    org.springframework.http.MediaType.APPLICATION_XML_VALUE,
    org.springframework.http.MediaType.APPLICATION_YAML_VALUE])
@AnnotationCollector
@interface ApiPatch {}
