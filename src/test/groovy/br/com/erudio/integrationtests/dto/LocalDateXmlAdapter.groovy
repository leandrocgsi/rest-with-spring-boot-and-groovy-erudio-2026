package br.com.erudio.integrationtests.dto

import jakarta.xml.bind.annotation.adapters.XmlAdapter

import java.time.LocalDate

class LocalDateXmlAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    LocalDate unmarshal(String value) {
        value == null ? null : LocalDate.parse(value)
    }

    @Override
    String marshal(LocalDate value) {
        value?.toString()
    }
}
