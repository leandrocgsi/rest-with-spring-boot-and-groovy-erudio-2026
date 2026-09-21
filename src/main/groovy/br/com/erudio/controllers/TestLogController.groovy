package br.com.erudio.controllers

import groovy.util.logging.Slf4j
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
@RequestMapping('/api/test/v1')
class TestLogController {

    @GetMapping
    String testLog() {
        log.debug('This is an DEBUG log')
        log.info('This is an INFO log')
        log.warn('This is an WARN log')
        log.error('This is an ERROR log')
        'Logs generated successfully!'
    }
}
