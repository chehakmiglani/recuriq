package com.recuriq.billing.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CustomerRegisterRequest {
   @NotBlank(message = "{user.name.required}")
   @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name must contain only letters and spaces")
   private String fullName;

   @NotBlank(message = "{user.email.required}")
   @Email(message = "{user.email.invalid}")
   private String email;

   @NotBlank(message = "{user.password.required}")
   @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "{user.password.invalid}")
   private String password;
}
