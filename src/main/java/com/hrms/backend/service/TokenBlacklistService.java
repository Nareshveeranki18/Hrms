package com.hrms.backend.service;

import com.hrms.backend.entity.TokenBlacklist;
import com.hrms.backend.repository.TokenBlacklistRepository;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    public TokenBlacklistService(TokenBlacklistRepository tokenBlacklistRepository) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    public void blacklistToken(String token) {
        if (!isTokenBlacklisted(token)) {
            TokenBlacklist blacklistedToken = new TokenBlacklist(token);
            tokenBlacklistRepository.save(blacklistedToken);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }
}