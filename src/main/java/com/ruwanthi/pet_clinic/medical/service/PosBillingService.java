package com.ruwanthi.pet_clinic.medical.service;

import com.ruwanthi.pet_clinic.medical.dto.BillResponse;
import com.ruwanthi.pet_clinic.medical.dto.CreateBillItemRequest;
import com.ruwanthi.pet_clinic.medical.dto.CreateBillRequest;
import com.ruwanthi.pet_clinic.medical.dto.PosBillItemResponse;
import com.ruwanthi.pet_clinic.medical.entity.Bill;
import com.ruwanthi.pet_clinic.medical.entity.BillItem;
import com.ruwanthi.pet_clinic.medical.entity.BillItemType;
import com.ruwanthi.pet_clinic.medical.entity.Medicine;
import com.ruwanthi.pet_clinic.medical.entity.StockBatch;
import com.ruwanthi.pet_clinic.medical.repo.BillRepository;
import com.ruwanthi.pet_clinic.medical.repo.MedicineRepository;
import com.ruwanthi.pet_clinic.medical.repo.StockBatchRepository;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PosBillingService {

    private static final DateTimeFormatter BILL_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BillRepository billRepository;
    private final MedicineRepository medicineRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StaffRepository staffRepository;

    public PosBillingService(
            BillRepository billRepository,
            MedicineRepository medicineRepository,
            StockBatchRepository stockBatchRepository,
            StaffRepository staffRepository
    ) {
        this.billRepository = billRepository;
        this.medicineRepository = medicineRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.staffRepository = staffRepository;
    }

    @Transactional
    public BillResponse createBill(CreateBillRequest request, User user) {
        requirePharmacyAccess(user);
        Staff staff = staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Staff profile not found"));

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("At least one bill item is required");
        }

        Bill bill = new Bill();
        bill.setBillNo(generateBillNo());
        bill.setBilledAt(LocalDateTime.now());
        bill.setPaymentMethod(request.paymentMethod().trim());
        bill.setNotes(blankToNull(request.notes()));
        bill.setCreatedByStaff(staff);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<BillItem> items = new ArrayList<>();

        for (CreateBillItemRequest requestItem : request.items()) {
            if (requestItem.itemType() == BillItemType.PRODUCT) {
                if (requestItem.medicineId() == null) {
                    throw new IllegalArgumentException("Medicine is required for product line items");
                }

                Medicine medicine = medicineRepository.findById(requestItem.medicineId())
                        .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + requestItem.medicineId()));

                int requestedQuantity = requestItem.quantity();
                List<StockBatch> batches = stockBatchRepository.findSellableBatchesForUpdate(medicine.getId(), LocalDate.now());
                int availableQuantity = batches.stream().mapToInt(StockBatch::getQuantityAvailable).sum();
                if (availableQuantity < requestedQuantity) {
                    throw new IllegalArgumentException("Insufficient stock for medicine " + medicine.getId() + ". Available quantity: " + availableQuantity);
                }

                int remaining = requestedQuantity;
                for (StockBatch batch : batches) {
                    if (remaining <= 0) {
                        break;
                    }

                    int deduct = Math.min(batch.getQuantityAvailable(), remaining);
                    batch.setQuantityAvailable(batch.getQuantityAvailable() - deduct);
                    remaining -= deduct;

                    if (batch.getUnitSellPrice() == null) {
                        throw new IllegalArgumentException("Unit sell price is not configured for batch " + batch.getBatchNo());
                    }

                    BigDecimal unitPrice = scaleMoney(batch.getUnitSellPrice());
                    BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(deduct)).setScale(2, RoundingMode.HALF_UP);

                    BillItem billItem = new BillItem();
                    billItem.setBill(bill);
                    billItem.setMedicine(medicine);
                    billItem.setItemType(BillItemType.PRODUCT);
                    billItem.setMedicineName(medicine.getName());
                    billItem.setDescription(blankToNull(requestItem.description()));
                    billItem.setQuantity(deduct);
                    billItem.setUnitPrice(unitPrice);
                    billItem.setLineTotal(lineTotal);

                    items.add(billItem);
                    subtotal = subtotal.add(lineTotal);
                }
            } else if (requestItem.itemType() == BillItemType.SERVICE) {
                String itemName = blankToNull(requestItem.itemName());
                if (itemName == null) {
                    throw new IllegalArgumentException("Item name is required for service line items");
                }

                BigDecimal unitPrice = scaleMoney(requestItem.unitPrice());
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requestItem.quantity())).setScale(2, RoundingMode.HALF_UP);

                BillItem billItem = new BillItem();
                billItem.setBill(bill);
                billItem.setMedicine(null);
                billItem.setItemType(BillItemType.SERVICE);
                billItem.setMedicineName(itemName);
                billItem.setDescription(blankToNull(requestItem.description()));
                billItem.setQuantity(requestItem.quantity());
                billItem.setUnitPrice(unitPrice);
                billItem.setLineTotal(lineTotal);

                items.add(billItem);
                subtotal = subtotal.add(lineTotal);
            }
        }

        BigDecimal discount = scaleMoney(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        BigDecimal tax = scaleMoney(request.taxAmount() == null ? BigDecimal.ZERO : request.taxAmount());
        BigDecimal total = subtotal.subtract(discount).add(tax).setScale(2, RoundingMode.HALF_UP);

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }

        bill.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        bill.setDiscountAmount(discount);
        bill.setTaxAmount(tax);
        bill.setTotalAmount(total);
        bill.setItems(items);

        Bill saved = billRepository.save(bill);
        return toBillResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> listBills(User user) {
        requirePharmacyAccess(user);
        return billRepository.findAllWithItemsOrderByIdDesc()
                .stream()
                .map(this::toBillResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(Long billId, User user) {
        requirePharmacyAccess(user);

        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        return toBillResponse(bill);
    }

    private BillResponse toBillResponse(Bill bill) {
        List<PosBillItemResponse> items = bill.getItems().stream()
                .map((item) -> new PosBillItemResponse(
                        item.getId(),
                        item.getItemType(),
                        item.getMedicine() != null ? item.getMedicine().getId() : null,
                        item.getMedicineName(),
                        item.getDescription(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new BillResponse(
                bill.getId(),
                bill.getBillNo(),
                bill.getBilledAt(),
                bill.getSubtotal(),
                bill.getDiscountAmount(),
                bill.getTaxAmount(),
                bill.getTotalAmount(),
                bill.getPaymentMethod(),
                bill.getNotes(),
                bill.getCreatedByStaff().getId(),
                bill.getCreatedByStaff().getFullName(),
                bill.getCreatedAt(),
                items
        );
    }

    private void requirePharmacyAccess(User user) {
        boolean allowed = user.getRoles().stream().anyMatch((role) ->
                "PHARMACIST".equalsIgnoreCase(role.getName()) ||
                        "ADMIN".equalsIgnoreCase(role.getName())
        );

        if (!allowed) {
            throw new SecurityException("Only pharmacists can manage POS billing");
        }
    }

    private String generateBillNo() {
        String timePart = LocalDateTime.now().format(BILL_TIME_FORMAT);

        for (int attempt = 0; attempt < 10; attempt++) {
            int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);
            String billNo = "BILL-" + timePart + "-" + randomPart;
            if (!billRepository.existsByBillNo(billNo)) {
                return billNo;
            }
        }

        throw new IllegalStateException("Failed to generate unique bill number");
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
