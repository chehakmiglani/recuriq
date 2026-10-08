package com.recuriq.billing.dto.admin;

import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class StaffResponse {
   private Long id;
   private String fullName;
   private String email;
   private String role;
   private Status status;

   public static StaffResponse from(User user) {
       StaffResponse r = new StaffResponse();
       r.setId(user.getId());
       r.setFullName(user.getFullName());
       r.setEmail(user.getEmail());
       r.setRole(user.getRole().name());
       r.setStatus(user.getStatus());
       return r;
   }
}
