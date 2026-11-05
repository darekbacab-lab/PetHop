package com.pethop.user.dto;
import com.pethop.user.AdminUserAction; import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Size;
public record AdminUserActionRequest(@NotNull AdminUserAction action,@Size(max=2000) String reason){}
