package com.recuriq.billing.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Maps to the USER table in the schema.
 * Implements UserDetails so Spring Security can use it directly.
 */
@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "user_id")
   private Long id;

   @NotBlank(message = "Full name is required")
   @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name must contain only letters and spaces")
   @Column(name = "full_name", nullable = false, length = 100)
   private String fullName;

   @Column(nullable = false, unique = true, length = 120)
   private String email;

   @Column(name = "password_hash")
   private String passwordHash;

   @Builder.Default
   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private UserRole role = UserRole.CUSTOMER;

   @Builder.Default
   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status = Status.ACTIVE;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @UpdateTimestamp
   @Column(name = "updated_at", nullable = false)
   private LocalDateTime updatedAt;

   // --- SPRING SECURITY METHODS ---
   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
       return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
   }

   @Override
   public String getPassword() { return passwordHash; }
   @Override
   public String getUsername() { return email; }
   @Override
   public boolean isAccountNonExpired() { return true; }
   @Override
   public boolean isAccountNonLocked() { return status.equals(Status.ACTIVE); }
   @Override
   public boolean isCredentialsNonExpired() { return true; }
   @Override
   public boolean isEnabled() { return status.equals(Status.ACTIVE); }
}
