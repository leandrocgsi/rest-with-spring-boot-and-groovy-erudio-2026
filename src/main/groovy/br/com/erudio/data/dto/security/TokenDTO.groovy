package br.com.erudio.data.dto.security

record TokenDTO(
        String username,
        Boolean authenticated,
        Date created,
        Date expiration,
        String accessToken,
        String refreshToken) {}
