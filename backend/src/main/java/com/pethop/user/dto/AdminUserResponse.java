package com.pethop.user.dto;
import com.pethop.user.*; import java.time.Instant; import java.util.UUID;
public record AdminUserResponse(UUID id,String fullName,String email,String phone,UserRole role,AccountStatus accountStatus,Instant createdAt){public static AdminUserResponse from(User u){return new AdminUserResponse(u.getId(),u.getFullName(),u.getEmail(),u.getPhone(),u.getRole(),u.getAccountStatus(),u.getCreatedAt());}}
