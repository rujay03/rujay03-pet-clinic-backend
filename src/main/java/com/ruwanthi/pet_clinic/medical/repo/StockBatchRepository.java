package com.ruwanthi.pet_clinic.medical.repo;

import com.ruwanthi.pet_clinic.medical.entity.StockBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    boolean existsByMedicineIdAndBatchNoIgnoreCase(Long medicineId, String batchNo);

    @Query("""
            select coalesce(sum(sb.quantityAvailable), 0)
            from StockBatch sb
            where sb.medicine.id = :medicineId
            """)
    Integer totalQuantityByMedicineId(@Param("medicineId") Long medicineId);

    @Query(value = """
            select sb.unit_sell_price
            from stock_batch sb
            where sb.medicine_id = :medicineId
              and sb.quantity_available > 0
              and (sb.expiry_date is null or sb.expiry_date >= :today)
            order by
              case when sb.expiry_date is null then 1 else 0 end,
              sb.expiry_date asc,
              sb.received_at asc,
              sb.stock_batch_id asc
            limit 1
            """, nativeQuery = true)
    BigDecimal findCurrentSellPriceByMedicineId(@Param("medicineId") Long medicineId, @Param("today") LocalDate today);

    @Query("""
            select sb
            from StockBatch sb
            where sb.medicine.id = :medicineId
            order by
              case when sb.expiryDate is null then 1 else 0 end,
              sb.expiryDate asc,
              sb.receivedAt asc,
              sb.id asc
            """)
    List<StockBatch> findByMedicineIdOrderByFefo(@Param("medicineId") Long medicineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sb
            from StockBatch sb
            where sb.medicine.id = :medicineId
              and sb.quantityAvailable > 0
              and (sb.expiryDate is null or sb.expiryDate >= :today)
            order by
              case when sb.expiryDate is null then 1 else 0 end,
              sb.expiryDate asc,
              sb.receivedAt asc,
              sb.id asc
            """)
    List<StockBatch> findSellableBatchesForUpdate(@Param("medicineId") Long medicineId, @Param("today") LocalDate today);
}
