package com.ruwanthi.pet_clinic.medical.service;

import com.ruwanthi.pet_clinic.medical.dto.CreateStockBatchRequest;
import com.ruwanthi.pet_clinic.medical.dto.SaleResponse;
import com.ruwanthi.pet_clinic.medical.dto.SellMedicineRequest;
import com.ruwanthi.pet_clinic.medical.entity.Medicine;
import com.ruwanthi.pet_clinic.medical.entity.StockBatch;
import com.ruwanthi.pet_clinic.medical.repo.MedicineRepository;
import com.ruwanthi.pet_clinic.medical.repo.StockBatchRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private User pharmacist;
    private Medicine medicine;

    @BeforeEach
    void setUp() {
        pharmacist = userWithRole("PHARMACIST");

        medicine = new Medicine();
        medicine.setId(7L);
        medicine.setName("Amoxicillin 250mg");
        medicine.setIsActive(true);
    }

    @Test
    void addStockBatchShouldRejectDuplicateBatchForSameMedicine() {
        CreateStockBatchRequest request = new CreateStockBatchRequest(
                7L,
                "BATCH-1001",
                LocalDate.now().plusDays(90),
                new BigDecimal("45.50"),
                new BigDecimal("60.00"),
                100,
                LocalDateTime.now()
        );

        when(medicineRepository.findById(7L)).thenReturn(Optional.of(medicine));
        when(stockBatchRepository.existsByMedicineIdAndBatchNoIgnoreCase(7L, "BATCH-1001")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.addStockBatch(request, pharmacist)
        );

        assertEquals("A batch with this number already exists for the selected medicine", exception.getMessage());
        verify(stockBatchRepository, never()).save(any(StockBatch.class));
    }

    @Test
    void addStockBatchShouldDefaultPurchasePriceToZeroWhenMissing() {
        CreateStockBatchRequest request = new CreateStockBatchRequest(
                7L,
                "BATCH-1002",
                LocalDate.now().plusDays(120),
                null,
                new BigDecimal("75.00"),
                40,
                LocalDateTime.now()
        );

        when(medicineRepository.findById(7L)).thenReturn(Optional.of(medicine));
        when(stockBatchRepository.existsByMedicineIdAndBatchNoIgnoreCase(7L, "BATCH-1002")).thenReturn(false);
        when(stockBatchRepository.save(any(StockBatch.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        inventoryService.addStockBatch(request, pharmacist);

        verify(stockBatchRepository).save(any(StockBatch.class));
    }

    @Test
    void sellMedicineShouldDeductAcrossMultipleBatchesUsingFefo() {
        SellMedicineRequest request = new SellMedicineRequest(7L, 70);

        StockBatch first = new StockBatch();
        first.setId(1L);
        first.setMedicine(medicine);
        first.setBatchNo("A-1");
        first.setExpiryDate(LocalDate.now().plusDays(7));
        first.setQuantityAvailable(30);

        StockBatch second = new StockBatch();
        second.setId(2L);
        second.setMedicine(medicine);
        second.setBatchNo("A-2");
        second.setExpiryDate(LocalDate.now().plusDays(20));
        second.setQuantityAvailable(50);

        when(medicineRepository.findById(7L)).thenReturn(Optional.of(medicine));
        when(stockBatchRepository.findSellableBatchesForUpdate(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(first, second));

        SaleResponse response = inventoryService.sellMedicine(request, pharmacist);

        assertEquals(70, response.soldQuantity());
        assertEquals(10, response.remainingQuantity());
        assertEquals(2, response.deductions().size());
        assertEquals(0, first.getQuantityAvailable());
        assertEquals(10, second.getQuantityAvailable());
    }

    @Test
    void sellMedicineShouldFailWhenInventoryIsInsufficient() {
        SellMedicineRequest request = new SellMedicineRequest(7L, 90);

        StockBatch first = new StockBatch();
        first.setMedicine(medicine);
        first.setQuantityAvailable(25);

        StockBatch second = new StockBatch();
        second.setMedicine(medicine);
        second.setQuantityAvailable(30);

        when(medicineRepository.findById(7L)).thenReturn(Optional.of(medicine));
        when(stockBatchRepository.findSellableBatchesForUpdate(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(first, second));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.sellMedicine(request, pharmacist)
        );

        assertEquals("Insufficient stock. Available quantity: 55", exception.getMessage());
        assertEquals(25, first.getQuantityAvailable());
        assertEquals(30, second.getQuantityAvailable());
    }

    private User userWithRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(100L);
        user.setRoles(Set.of(role));
        return user;
    }
}
