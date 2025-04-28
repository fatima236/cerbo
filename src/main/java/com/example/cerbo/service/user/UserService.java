package com.example.cerbo.service.user;

import com.example.cerbo.entity.PendingUser;
import com.example.cerbo.entity.User;

import java.util.Set;

public interface UserService {
    public void requestInvestigateurSignup(User userRequest);
    public User createUser(String email, String password, Set<String> roles);
    void sendValidationRequestToAdmin(PendingUser pendingUser);
    public User approveInvestigateur(Long pendingUserId);
    void sendApprovalConfirmation(String userEmail);
    public void rejectInvestigateur(Long pendingUserId);
    void sendPasswordResetEmail(String email, String token);
    void resetPassword(String token, String newPassword);
    User findByEmail(String email);
    
}
