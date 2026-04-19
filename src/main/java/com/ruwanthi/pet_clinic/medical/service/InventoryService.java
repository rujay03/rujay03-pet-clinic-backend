package com.ruwanthi.pet_clinic.medical.service;

import com.ruwanthi.pet_clinic.medical.dto.CreateStockBatchRequest;
import com.ruwanthi.pet_clinic.medical.dto.InventoryBatchResponse;
import com.ruwanthi.pet_clinic.medical.dto.InventoryMedicineResponse;
import com.ruwanthi.pet_clinic.medical.dto.SaleBatchDeductionResponse;
import com.ruwanthi.pet_clinic.medical.dto.SaleResponse;
import com.ruwanthi.pet_clinic.medical.dto.SellMedicineRequest;
import com.ruwanthi.pet_clinic.medical.entity.Medicine;
import com.ruwanthi.pet_clinic.medical.entity.StockBatch;
import com.ruwanthi.pet_clinic.medical.repo.MedicineRepository;
import com.ruwanthi.pet_clinic.medical.repo.StockBatchRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private final MedicineRepository medicineRepository;
    private final StockBatchRepository stockBatchRepository;

    public InventoryService(MedicineRepository medicineRepository, StockBatchRepository stockBatchRepository) {
        this.medicineRepository = medicineRepository;
        this.stockBatchRepository = stockBatchRepository;
    }

    @Transactional
    public InventoryBatchResponse addStockBatch(CreateStockBatchRequest request, User user) {
        requirePharmacyAccess(user);

        Medicine medicine = medicineRepository.findById(request.medicineId())
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        String batchNo = request.batchNo().trim();
        if (stockBatchRepository.existsByMedicineIdAndBatchNoIgnoreCase(medicine.getId(), batchNo)) {
            throw new IllegalArgumentException("A batch with this number already exists for the selected medicine");
        }

        if (request.expiryDate() != null && request.expiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot add inventory with a past expiry date");
        }

        StockBatch batch = new StockBatch();
        batch.setMedicine(medicine);
        batch.setBatchNo(batchNo);
        batch.setExpiryDate(request.expiryDate());
        batch.setPurchasePrice(request.purchasePrice() != null ? request.purchasePrice() : BigDecimal.ZERO);
        batch.setUnitSellPrice(request.unitSellPrice());
        batch.setQuantityAvailable(request.quantity());
        batch.setReceivedAt(request.receivedAt() != null ? request.receivedAt() : LocalDateTime.now());

        return toBatchResponse(stockBatchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public List<InventoryMedicineResponse> listInventory(User user) {
        requirePharmacyAccess(user);

        return medicineRepository.findAllByOrderByIdDesc()
                .stream()
                .map((medicine) -> new InventoryMedicineResponse(
                        medicine.getId(),
                        medicine.getName(),
                        medicine.getGenericName(),
                        medicine.getForm(),
                        medicine.getStrength(),
                        medicine.getIsActive(),
                        stockBatchRepository.totalQuantityByMedicineId(medicine.getId()),
                        stockBatchRepository.findCurrentSellPriceByMedicineId(medicine.getId(), LocalDate.now())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> listBatchesByMedicine(Long medicineId, User user) {
        requirePharmacyAccess(user);
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        return stockBatchRepository.findByMedicineIdOrderByFefo(medicineId)
                .stream()
                .map((batch) -> toBatchResponse(batch, medicine))
                .toList();
    }

    @Transactional
    public SaleResponse sellMedicine(SellMedicineRequest request, User user) {
        requirePharmacyAccess(user);

        Medicine medicine = medicineRepository.findById(request.medicineId())
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        int requestedQuantity = request.quantity();
        int remainingToDeduct = requestedQuantity;

        List<StockBatch> batches = stockBatchRepository.findSellableBatchesForUpdate(medicine.getId(), LocalDate.now());
        int sellableQuantity = batches.stream().mapToInt(StockBatch::getQuantityAvailable).sum();
        if (sellableQuantity < requestedQuantity) {
            throw new IllegalArgumentException("Insufficient stock. Available quantity: " + sellableQuantity);
        }

        List<SaleBatchDeductionResponse> deductions = new ArrayList<>();

        for (StockBatch batch : batches) {
            if (remainingToDeduct <= 0) {
                break;
            }

            int deduct = Math.min(batch.getQuantityAvailable(), remainingToDeduct);
            batch.setQuantityAvailable(batch.getQuantityAvailable() - deduct);
            remainingToDeduct -= deduct;

            deductions.add(new SaleBatchDeductionResponse(
                    batch.getId(),
                    batch.getBatchNo(),
                    batch.getExpiryDate(),
                    deduct,
                    batch.getQuantityAvailable()
            ));
        }

        int remainingQuantity = sellableQuantity - requestedQuantity;

        return new SaleResponse(
                medicine.getId(),
                medicine.getName(),
                requestedQuantity,
                remainingQuantity,
                deductions
        );
    }

    private InventoryBatchResponse toBatchResponse(StockBatch batch) {
        return toBatchResponse(batch, batch.getMedicine());
    }

    private InventoryBatchResponse toBatchResponse(StockBatch batch, Medicine medicine) {
        return new InventoryBatchResponse(
                batch.getId(),
                medicine.getId(),
                medicine.getName(),
                batch.getBatchNo(),
                batch.getExpiryDate(),
                batch.getPurchasePrice(),
                batch.getUnitSellPrice(),
                batch.getQuantityAvailable(),
                batch.getReceivedAt()
        );
    }

    private void requirePharmacyAccess(User user) {
        boolean allowed = user.getRoles().stream().anyMatch((role) ->
                "PHARMACIST".equalsIgnoreCase(role.getName()) ||
                        "ADMIN".equalsIgnoreCase(role.getName())
        );

        if (!allowed) {
            throw new SecurityException("Only pharmacists can manage inventory");
        }
    }
}
