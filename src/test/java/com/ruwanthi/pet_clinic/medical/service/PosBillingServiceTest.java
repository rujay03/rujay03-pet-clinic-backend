package com.ruwanthi.pet_clinic.medical.service;

import com.ruwanthi.pet_clinic.medical.dto.BillResponse;
import com.ruwanthi.pet_clinic.medical.dto.CreateBillItemRequest;
import com.ruwanthi.pet_clinic.medical.dto.CreateBillRequest;
import com.ruwanthi.pet_clinic.medical.entity.BillItemType;
import com.ruwanthi.pet_clinic.medical.entity.Medicine;
import com.ruwanthi.pet_clinic.medical.entity.StockBatch;
import com.ruwanthi.pet_clinic.medical.repo.BillRepository;
import com.ruwanthi.pet_clinic.medical.repo.MedicineRepository;
import com.ruwanthi.pet_clinic.medical.repo.StockBatchRepository;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosBillingServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private PosBillingService posBillingService;

    private User pharmacist;
    private Staff staff;
    private Medicine medicine;

    @BeforeEach
    void setUp() {
        pharmacist = userWithRole("PHARMACIST");

        staff = new Staff();
        staff.setId(50L);
        staff.setFullName("Pharmacist One");

        medicine = new Medicine();
        medicine.setId(7L);
        medicine.setName("Amoxicillin 250mg");
        medicine.setIsActive(true);
    }

    @Test
    void createBillShouldPersistBillAndDeductStock() {
        CreateBillRequest request = new CreateBillRequest(
                List.of(new CreateBillItemRequest(7L, BillItemType.PRODUCT, null, null, 2, new BigDecimal("125.00"))),
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                "CASH",
                "Walk-in"
        );

        StockBatch batch = new StockBatch();
        batch.setMedicine(medicine);
        batch.setQuantityAvailable(5);
        batch.setExpiryDate(LocalDate.now().plusDays(30));
        batch.setUnitSellPrice(new BigDecimal("130.00"));

        when(staffRepository.findByUserId(pharmacist.getId())).thenReturn(Optional.of(staff));
        when(stockBatchRepository.findSellableBatchesForUpdate(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(batch));
        when(medicineRepository.findById(7L)).thenReturn(Optional.of(medicine));
        when(billRepository.existsByBillNo(anyString())).thenReturn(false);
        when(billRepository.save(any())).thenAnswer((invocation) -> invocation.getArgument(0));

        BillResponse response = posBillingService.createBill(request, pharmacist);

        assertEquals(new BigDecimal("260.00"), response.subtotal());
        assertEquals(new BigDecimal("265.00"), response.totalAmount());
        assertEquals(3, batch.getQuantityAvailable());
        assertEquals(1, response.items().size());
        assertEquals("CASH", response.paymentMethod());

        verify(billRepository).save(any());
    }

    @Test
    void createBillShouldFailWhenStockIsInsufficient() {
        CreateBillRequest request = new CreateBillRequest(
                List.of(new CreateBillItemRequest(7L, BillItemType.PRODUCT, null, null, 6, new BigDecimal("100.00"))),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "CARD",
                null
        );

        StockBatch batch = new StockBatch();
        batch.setMedicine(medicine);
        batch.setQuantityAvailable(5);

        when(staffRepository.findByUserId(pharmacist.getId())).thenReturn(Optional.of(staff));
        when(stockBatchRepository.findSellableBatchesForUpdate(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(batch));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> posBillingService.createBill(request, pharmacist)
        );

        assertEquals("Insufficient stock for medicine 7. Available quantity: 5", exception.getMessage());
    }

    @Test
    void createBillShouldSaveServiceLineWithoutInventoryDeduction() {
        CreateBillRequest request = new CreateBillRequest(
                List.of(new CreateBillItemRequest(null, BillItemType.SERVICE, "Treatment", "Post-checkin care", 1, new BigDecimal("1500.00"))),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "CASH",
                null
        );

        when(staffRepository.findByUserId(pharmacist.getId())).thenReturn(Optional.of(staff));
        when(billRepository.existsByBillNo(anyString())).thenReturn(false);
        when(billRepository.save(any())).thenAnswer((invocation) -> invocation.getArgument(0));

        BillResponse response = posBillingService.createBill(request, pharmacist);

        assertEquals(new BigDecimal("1500.00"), response.subtotal());
        assertEquals(new BigDecimal("1500.00"), response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals(BillItemType.SERVICE, response.items().get(0).itemType());
        assertEquals("Treatment", response.items().get(0).itemName());
    }

    private User userWithRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(200L);
        user.setRoles(Set.of(role));
        return user;
    }
}
