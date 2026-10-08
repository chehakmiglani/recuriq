package com.recuriq.billing.service;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.recuriq.billing.dto.customer.CustomerProfileDTO;
import com.recuriq.billing.dto.customer.PlanDTO;
import com.recuriq.billing.dto.customer.AddOnDTO;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.entity.PriceBookEntry;
import com.recuriq.billing.entity.AddOn;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.repository.PlanRepository;
import com.recuriq.billing.repository.PriceBookEntryRepository;
import com.recuriq.billing.repository.AddOnRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlanRepository planRepository;
    @Mock private AddOnRepository addOnRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PriceBookEntryRepository priceBookEntryRepository;
    @InjectMocks
    private CustomerServiceImpl customerService;
    private Customer customer;
    private User user;
    private Plan plan;
    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com").fullName("Test User").build();
        customer = Customer.builder().id(1L).user(user).status(Status.ACTIVE).build();
        plan = Plan.builder()
                .id(1L)
                .name("Basic")
                .status(Status.ACTIVE)
                .effectiveFrom(LocalDate.now())
                .billingPeriod(BillingPeriod.MONTHLY)
                .build();
    }
    @Test
    void testGetProfile() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        CustomerProfileDTO dto = customerService.getProfile("test@test.com");
        assertNotNull(dto);
        assertEquals("Test User", dto.getFullName());
    }
    @Test
    void testUpdateProfile() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        CustomerProfileDTO updateDto = new CustomerProfileDTO();
        updateDto.setPhone("1234567890");
        CustomerProfileDTO result = customerService.updateProfile("test@test.com", updateDto);
        assertNotNull(result);
        assertEquals("1234567890", customer.getPhone());
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void testGetAvailableAddOns() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        Subscription sub = Subscription.builder().id(1L).plan(plan).build();
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Arrays.asList(sub));
        AddOn addOn = AddOn.builder()
                .id(1L)
                .name("Storage")
                .billingPeriod(BillingPeriod.MONTHLY)
                .status(Status.ACTIVE)
                .build();
        when(addOnRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(addOn));
        List<AddOnDTO> addOns = customerService.getAvailableAddOns("test@test.com");
        assertEquals(1, addOns.size());
        assertEquals("Storage", addOns.get(0).getName());
    }

    @Test
    void testGetAvailableAddOns_NoSubscription() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Arrays.asList());
        AddOn addOn = AddOn.builder()
                .id(1L)
                .name("Storage")
                .billingPeriod(BillingPeriod.MONTHLY)
                .status(Status.ACTIVE)
                .build();
        when(addOnRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(addOn));
        List<AddOnDTO> addOns = customerService.getAvailableAddOns("test@test.com");
        assertEquals(1, addOns.size());
        assertEquals("Storage", addOns.get(0).getName());
    }
    @Test
    void testGetProfile_UserNotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerService.getProfile("test@test.com"));
    }
    @Test
    void testGetProfile_CustomerNotFound() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> customerService.getProfile("test@test.com"));
    }

    @Test
    void testGetAvailableAddOns_MismatchedBillingPeriod() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
        Subscription sub = Subscription.builder().id(1L).plan(plan).build(); // MONTHLY plan
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Arrays.asList(sub));
        AddOn yearlyAddOn = AddOn.builder()
                .id(1L)
                .name("Yearly Storage")
                .billingPeriod(BillingPeriod.YEARLY)
                .status(Status.ACTIVE)
                .build();
        when(addOnRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(yearlyAddOn));
        List<AddOnDTO> addOns = customerService.getAvailableAddOns("test@test.com");
        // YEARLY addon should be filtered out for MONTHLY subscription
        assertEquals(0, addOns.size());
    }
    @Test
    void testGetAvailablePlans_Success() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setId(1L);
        entry.setPlan(plan);
        entry.setRegion("INDIA");
        entry.setCurrency("INR");
        entry.setPriceMinor(999L);
        entry.setEffectiveFrom(LocalDate.now());
        when(priceBookEntryRepository.findAll())
                .thenReturn(List.of(entry));
        List<PlanDTO> result =
                customerService.getAvailablePlans(null);
        assertEquals(1, result.size());
        assertEquals("Basic", result.get(0).getName());
        assertEquals("INR", result.get(0).getDefaultCurrency());
    }
    @Test
    void testGetAvailablePlans_WithRegionFilter() {
        PriceBookEntry indiaEntry = new PriceBookEntry();
        indiaEntry.setPlan(plan);
        indiaEntry.setRegion("INDIA");
        indiaEntry.setCurrency("INR");
        indiaEntry.setPriceMinor(999L);
        indiaEntry.setEffectiveFrom(LocalDate.now());
        PriceBookEntry usEntry = new PriceBookEntry();
        usEntry.setPlan(plan);
        usEntry.setRegion("USA");
        usEntry.setCurrency("USD");
        usEntry.setPriceMinor(1999L);
        usEntry.setEffectiveFrom(LocalDate.now());
        when(priceBookEntryRepository.findAll())
                .thenReturn(List.of(indiaEntry, usEntry));
        List<PlanDTO> result =
                customerService.getAvailablePlans("INDIA");
        assertEquals(1, result.size());
        assertEquals("INR", result.get(0).getDefaultCurrency());
    }
    @Test
    void testGetAvailablePlans_InactivePlanFiltered() {
        Plan inactivePlan = Plan.builder()
                .id(2L)
                .name("Inactive")
                .status(Status.DISABLED)
                .billingPeriod(BillingPeriod.MONTHLY)
                .build();
        PriceBookEntry entry = new PriceBookEntry();
        entry.setPlan(inactivePlan);
        entry.setRegion("INDIA");
        entry.setCurrency("INR");
        entry.setPriceMinor(999L);
        entry.setEffectiveFrom(LocalDate.now());
        when(priceBookEntryRepository.findAll())
                .thenReturn(List.of(entry));
        List<PlanDTO> result =
                customerService.getAvailablePlans(null);
        assertTrue(result.isEmpty());
    }
    @Test
    void testGetAvailablePlans_WithEffectiveTo() {
        plan.setEffectiveTo(LocalDate.now().plusDays(30));
        PriceBookEntry entry = new PriceBookEntry();
        entry.setPlan(plan);
        entry.setRegion("INDIA");
        entry.setCurrency("INR");
        entry.setPriceMinor(999L);
        entry.setEffectiveFrom(LocalDate.now());
        entry.setEffectiveTo(LocalDate.now().plusDays(30));
        when(priceBookEntryRepository.findAll())
                .thenReturn(List.of(entry));
        List<PlanDTO> result =
                customerService.getAvailablePlans(null);
        assertNotNull(result.get(0).getEffectiveTo());
    }
    @Test
    void testGetFeaturedPlans() {
        Plan featured1 = Plan.builder()
                .id(1L)
                .name("Basic")
                .status(Status.ACTIVE)
                .billingPeriod(BillingPeriod.MONTHLY)
                .build();
        Plan featured2 = Plan.builder()
                .id(3L)
                .name("Premium")
                .status(Status.ACTIVE)
                .billingPeriod(BillingPeriod.MONTHLY)
                .build();
        PriceBookEntry entry1 = new PriceBookEntry();
        entry1.setPlan(featured1);
        entry1.setRegion("INDIA");
        entry1.setCurrency("INR");
        entry1.setPriceMinor(999L);
        entry1.setEffectiveFrom(LocalDate.now());
        PriceBookEntry entry2 = new PriceBookEntry();
        entry2.setPlan(featured2);
        entry2.setRegion("INDIA");
        entry2.setCurrency("INR");
        entry2.setPriceMinor(1999L);
        entry2.setEffectiveFrom(LocalDate.now());
        when(priceBookEntryRepository.findByPlan_Id(1L))
                .thenReturn(List.of(entry1));
        when(priceBookEntryRepository.findByPlan_Id(3L))
                .thenReturn(List.of(entry2));
        List<PlanDTO> result =
                customerService.getFeaturedPlans(null);
        assertEquals(2, result.size());
    }
    @Test
    void testGetFeaturedPlans_RegionFilter() {
        PriceBookEntry indiaEntry = new PriceBookEntry();
        indiaEntry.setPlan(plan);
        indiaEntry.setRegion("INDIA");
        indiaEntry.setCurrency("INR");
        indiaEntry.setPriceMinor(999L);
        indiaEntry.setEffectiveFrom(LocalDate.now());
        PriceBookEntry usaEntry = new PriceBookEntry();
        usaEntry.setPlan(plan);
        usaEntry.setRegion("USA");
        usaEntry.setCurrency("USD");
        usaEntry.setPriceMinor(1999L);
        usaEntry.setEffectiveFrom(LocalDate.now());
        when(priceBookEntryRepository.findByPlan_Id(anyLong()))
                .thenReturn(List.of(indiaEntry, usaEntry));
        List<PlanDTO> result =
                customerService.getFeaturedPlans("INDIA");
        assertFalse(result.isEmpty());
        result.forEach(p ->
                assertEquals("INR", p.getDefaultCurrency()));
    }
    @Test
    void testGetAllActivePlans() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setPlan(plan);
        entry.setRegion("INDIA");
        entry.setCurrency("INR");
        entry.setPriceMinor(999L);
        entry.setEffectiveFrom(LocalDate.now());
        when(priceBookEntryRepository.findAll())
                .thenReturn(List.of(entry));
        List<PlanDTO> result =
                customerService.getAllActivePlans(null);
        assertEquals(1, result.size());
    }
    @Test
    void testGetAvailableAddOns_MapsFields() {
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));
        when(customerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(customer));
        Subscription sub = Subscription.builder()
                .id(1L)
                .plan(plan)
                .build();
        when(subscriptionRepository.findByCustomer_IdAndStatusIn(anyLong(), anyList()))
                .thenReturn(List.of(sub));
        AddOn addOn = AddOn.builder()
                .id(1L)
                .name("Storage")
                .priceMinor(500L)
                .currency("INR")
                .billingPeriod(BillingPeriod.MONTHLY)
                .status(Status.ACTIVE)
                .build();
        when(addOnRepository.findByStatus(Status.ACTIVE))
                .thenReturn(List.of(addOn));
        List<AddOnDTO> result =
                customerService.getAvailableAddOns("test@test.com");
        assertEquals(1, result.size());
        AddOnDTO dto = result.get(0);
        assertEquals("Storage", dto.getName());
        assertEquals(500L, dto.getPriceMinor());
        assertEquals("INR", dto.getCurrency());
    }
}