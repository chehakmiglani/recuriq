package com.recuriq.billing.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.recuriq.billing.config.SecurityConfig;
import com.recuriq.billing.dto.customer.NotificationResponse;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.service.NotificationService;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationService notificationService;

	@MockitoBean
	private CustomerRepository customerRepository;

	@MockitoBean
	private UserRepository userRepository;

	private User testUser;
	private Customer testCustomer;
	private UsernamePasswordAuthenticationToken auth;

	@BeforeEach
	void setUp() {
		testUser = User.builder().id(1L).email("test@test.com").role(UserRole.CUSTOMER).build();

		testCustomer = new Customer();
		testCustomer.setId(10L);
		testCustomer.setUser(testUser);

		auth = new UsernamePasswordAuthenticationToken(testUser, null,
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
	}

	@Test
	void testGetMyNotifications_Success() throws Exception {
		NotificationResponse response = new NotificationResponse();
		response.setNotificationId(100L);
		response.setBody("Test Notification");

		when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(testCustomer));
		when(notificationService.getCustomerNotifications(10L)).thenReturn(List.of(response));

		mockMvc.perform(get("/api/notifications/me").with(authentication(auth))).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].body").value("Test Notification"));

		verify(customerRepository, times(1)).findByUser_Id(1L);
		verify(notificationService, times(1)).getCustomerNotifications(10L);
	}

	@Test
	void testGetMyNotifications_CustomerNotFound() throws Exception {
		when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/notifications/me").with(authentication(auth))).andExpect(status().isNotFound());

		verify(customerRepository, times(1)).findByUser_Id(1L);
		verify(notificationService, never()).getCustomerNotifications(anyLong());
	}

	@Test
	void testMarkAsRead_Success() throws Exception {
		doNothing().when(notificationService).markAsRead(100L);

		mockMvc.perform(put("/api/notifications/100/read").with(csrf()).with(authentication(auth)))
				.andExpect(status().isOk());

		verify(notificationService, times(1)).markAsRead(100L);
	}

	@Test
	void testMarkAllAsRead_Success() throws Exception {
		when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(testCustomer));
		doNothing().when(notificationService).markAllAsRead(10L);

		mockMvc.perform(put("/api/notifications/read-all").with(csrf()).with(authentication(auth)))
				.andExpect(status().isOk());

		verify(customerRepository, times(1)).findByUser_Id(1L);
		verify(notificationService, times(1)).markAllAsRead(10L);
	}

	@Test
	void testMarkAllAsRead_CustomerNotFound() throws Exception {
		when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/notifications/read-all").with(csrf()).with(authentication(auth)))
				.andExpect(status().isNotFound());

		verify(customerRepository, times(1)).findByUser_Id(1L);
		verify(notificationService, never()).markAllAsRead(anyLong());
	}

	@Test
	void testSeedTestData_Success() throws Exception {
		UsernamePasswordAuthenticationToken staff = new UsernamePasswordAuthenticationToken(testUser, null,
				List.of(new SimpleGrantedAuthority("ROLE_SUPPORT")));
		mockMvc.perform(get("/api/notifications/seed-test-data").with(authentication(staff)))
				.andExpect(status().isOk())
				.andExpect(content().string("Triggered the real notification generation schedulers. Check your database/UI!"));

		verify(notificationService, times(1)).generateRenewalReminders();
		verify(notificationService, times(1)).generatePendingPaymentReminders();
		verify(notificationService, times(1)).processPendingNotifications();
	}
}