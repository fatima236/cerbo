package com.example.cerbo.service.blacklistService;

public interface BlacklistService {
    void add(String token);
    boolean isBlacklisted(String token);
}
