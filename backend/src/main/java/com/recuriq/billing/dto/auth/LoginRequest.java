package com.recuriq.billing.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
   @NotBlank(message = "{user.email.required}")
   @Email(message = "{user.email.invalid}")
   private String email;

   @NotBlank(message = "{user.password.required}")
   private String password;
}
