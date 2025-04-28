package com.example.cerbo.service.blacklistService;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class BlacklistServiceImpl implements BlacklistService {

    private Set<String> blacklistedTokens = new HashSet<>();

    @Override
    public void add(String token) {
        blacklistedTokens.add(token);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
