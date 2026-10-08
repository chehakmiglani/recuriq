package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.dto.customer.PaymentMethodDTO;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.entity.PaymentMethod;
import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.PaymentMethodRepository;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.request.AddPaymentMethodRequest;

@ExtendWith(MockitoExtension.class)
class CustomerPaymentServiceImplTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerPaymentServiceImpl customerPaymentService;

    private Customer customer;
    private User user;
    private PaymentMethod paymentMethod;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com").fullName("Test User").build();
        customer = Customer.builder().id(1L).user(user).status(Status.ACTIVE).build();

        paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setCustomer(customer);
        paymentMethod.setPaymentType(PaymentType.CARD);
        paymentMethod.setIsDefault(true);
        paymentMethod.setCardLast4("1234");
        paymentMethod.setStatus(Status.ACTIVE);
    }

    @Test
    void testGetPaymentMethods() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(paymentMethod));

        List<PaymentMethodDTO> dtos = customerPaymentService.getPaymentMethods("test@test.com");

        assertEquals(1, dtos.size());
        assertEquals("1234", dtos.get(0).getCardLast4());
    }

    @Test
    void testAddPaymentMethod_Card() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList());
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);

        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setPaymentType(PaymentType.CARD);
        request.setCardNumber("1234567890124321");
        request.setExpiryMonth(12);
        request.setExpiryYear(2030);
        request.setIsDefault(true);

        PaymentMethodDTO dto = customerPaymentService.addPaymentMethod("test@test.com", request);

        assertNotNull(dto);
        verify(paymentMethodRepository, times(1)).save(any(PaymentMethod.class));
    }

    @Test
    void testAddPaymentMethod_UPI() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList());

        paymentMethod.setPaymentType(PaymentType.UPI);
        paymentMethod.setUpiId("test@upi");
        paymentMethod.setCardLast4(null);
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);

        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setPaymentType(PaymentType.UPI);
        request.setUpiId("test@upi");
        request.setIsDefault(true);

        PaymentMethodDTO dto = customerPaymentService.addPaymentMethod("test@test.com", request);

        assertNotNull(dto);
        assertEquals("test@upi", dto.getUpiId());
    }

    @Test
    void testSetDefaultPaymentMethod() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(paymentMethod));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));

        customerPaymentService.setDefaultPaymentMethod("test@test.com", 1L);

        assertTrue(paymentMethod.getIsDefault());
        verify(paymentMethodRepository, times(2)).save(any(PaymentMethod.class));
    }

    @Test
    void testDeletePaymentMethod_NotDefault() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));

        PaymentMethod otherMethod = new PaymentMethod();
        otherMethod.setId(2L);
        otherMethod.setCustomer(customer);
        otherMethod.setIsDefault(false);

        when(paymentMethodRepository.findById(2L)).thenReturn(Optional.of(otherMethod));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(paymentMethod, otherMethod));

        customerPaymentService.deletePaymentMethod("test@test.com", 2L);

        verify(paymentMethodRepository, times(1)).delete(otherMethod);
    }

    @Test
    void testDeletePaymentMethod_Default_ThrowsException() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(paymentMethod));

        assertThrows(RuntimeException.class, () -> customerPaymentService.deletePaymentMethod("test@test.com", 1L));
    }

    @Test
    void testAddPaymentMethod_Card_ExistingDefault() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        
        PaymentMethod existingMethod = new PaymentMethod();
        existingMethod.setId(2L);
        existingMethod.setIsDefault(true);
        
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(existingMethod));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);

        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setPaymentType(PaymentType.CARD);
        request.setCardNumber("1234567890124321");
        request.setExpiryMonth(12);
        request.setExpiryYear(2030);
        request.setIsDefault(true);

        PaymentMethodDTO dto = customerPaymentService.addPaymentMethod("test@test.com", request);

        assertNotNull(dto);
        assertFalse(existingMethod.getIsDefault());
        verify(paymentMethodRepository, times(2)).save(any(PaymentMethod.class));
    }

    @Test
    void testDeletePaymentMethod_Default_WithRemaining() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        
        PaymentMethod otherMethod = new PaymentMethod();
        otherMethod.setId(2L);
        otherMethod.setCustomer(customer);
        otherMethod.setIsDefault(false);
        
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(paymentMethod, otherMethod));

        customerPaymentService.deletePaymentMethod("test@test.com", 1L);

        assertTrue(otherMethod.getIsDefault());
        verify(paymentMethodRepository, times(1)).save(otherMethod);
        verify(paymentMethodRepository, times(1)).delete(paymentMethod);
    }

    @Test
    void testSetDefaultPaymentMethod_NotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> customerPaymentService.setDefaultPaymentMethod("test@test.com", 1L));
    }

    @Test
    void testSetDefaultPaymentMethod_Unauthorized() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        
        Customer otherCustomer = new Customer();
        otherCustomer.setId(2L);
        paymentMethod.setCustomer(otherCustomer);

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));

        assertThrows(RuntimeException.class, () -> customerPaymentService.setDefaultPaymentMethod("test@test.com", 1L));
    }

    @Test
    void testDeletePaymentMethod_NotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> customerPaymentService.deletePaymentMethod("test@test.com", 1L));
    }

    @Test
    void testDeletePaymentMethod_Unauthorized() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        
        Customer otherCustomer = new Customer();
        otherCustomer.setId(2L);
        paymentMethod.setCustomer(otherCustomer);

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));

        assertThrows(RuntimeException.class, () -> customerPaymentService.deletePaymentMethod("test@test.com", 1L));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "4111111111111111, VISA",
        "5111111111111111, MASTERCARD",
        "341111111111111, AMEX"
    })
    void testAddPaymentMethod_CardBrands(String cardNumber, String expectedBrand) {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList());
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(i -> i.getArgument(0));

        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setPaymentType(PaymentType.CARD);
        request.setCardNumber(cardNumber);
        request.setExpiryMonth(12);
        request.setExpiryYear(2030);
        request.setIsDefault(true);

        PaymentMethodDTO dto = customerPaymentService.addPaymentMethod("test@test.com", request);

        assertNotNull(dto);
        assertEquals(expectedBrand, dto.getCardBrand());
    }

    @Test
    void testAddPaymentMethod_NotDefault_WithExisting() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        
        PaymentMethod existingMethod = new PaymentMethod();
        existingMethod.setId(2L);
        existingMethod.setIsDefault(true);
        
        when(paymentMethodRepository.findByCustomer_Id(1L)).thenReturn(Arrays.asList(existingMethod));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(i -> i.getArgument(0));

        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setPaymentType(PaymentType.CARD);
        request.setCardNumber("1234567890124321");
        request.setExpiryMonth(12);
        request.setExpiryYear(2030);
        request.setIsDefault(false);

        PaymentMethodDTO dto = customerPaymentService.addPaymentMethod("test@test.com", request);

        assertNotNull(dto);
        assertFalse(dto.getIsDefault()); // The newly added method should NOT be default
        verify(paymentMethodRepository, never()).save(existingMethod); // Existing method should not be updated
    }
}
