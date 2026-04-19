package com.ruwanthi.pet_clinic.medical.repo;

import com.ruwanthi.pet_clinic.medical.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    boolean existsByBillNo(String billNo);

    @Query("""
            select distinct b
            from Bill b
            left join fetch b.items i
            left join fetch i.medicine
            left join fetch b.createdByStaff
            where b.id = :billId
            """)
    Optional<Bill> findByIdWithItems(@Param("billId") Long billId);

    @Query("""
            select distinct b
            from Bill b
            left join fetch b.items i
            left join fetch i.medicine
            left join fetch b.createdByStaff
            order by b.id desc
            """)
    List<Bill> findAllWithItemsOrderByIdDesc();
}

