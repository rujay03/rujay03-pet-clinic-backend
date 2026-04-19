package com.ruwanthi.pet_clinic.notification.sms;

import com.ruwanthi.pet_clinic.auth.service.EmailService;
import com.ruwanthi.pet_clinic.medical.entity.Vaccination;
import com.ruwanthi.pet_clinic.medical.repo.VaccinationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VaccinationReminderService {

    private static final Logger logger = LoggerFactory.getLogger(VaccinationReminderService.class);

    private final VaccinationRepository vaccinationRepository;
    private final VaccinationReminderDispatchRepository dispatchRepository;
    private final EmailService emailService;
    private final boolean remindersEnabled;

    public VaccinationReminderService(VaccinationRepository vaccinationRepository,
                                      VaccinationReminderDispatchRepository dispatchRepository,
                                      EmailService emailService,
                                      @Value("${app.vaccination-reminders.enabled:true}") boolean remindersEnabled) {
        this.vaccinationRepository = vaccinationRepository;
        this.dispatchRepository = dispatchRepository;
        this.emailService = emailService;
        this.remindersEnabled = remindersEnabled;
    }

    @Transactional
    public ReminderRunResult sendDueReminders(LocalDate runDate) {
        if (!remindersEnabled) {
            return new ReminderRunResult(runDate, 0, 0, 0, true);
        }

        int sent = 0;
        int failed = 0;
        int skipped = 0;

        ReminderBatchResult weekBatch = processReminderWindow(runDate, 7, VaccinationReminderDispatch.ReminderType.WEEK_BEFORE, "1 week before");
        sent += weekBatch.sent();
        failed += weekBatch.failed();
        skipped += weekBatch.skipped();

        ReminderBatchResult dayBatch = processReminderWindow(runDate, 1, VaccinationReminderDispatch.ReminderType.DAY_BEFORE, "1 day before");
        sent += dayBatch.sent();
        failed += dayBatch.failed();
        skipped += dayBatch.skipped();

        logger.info("Vaccination reminder run completed for {}: sent={}, failed={}, skipped={}", runDate, sent, failed, skipped);
        return new ReminderRunResult(runDate, sent, failed, skipped, false);
    }

    private ReminderBatchResult processReminderWindow(LocalDate runDate,
                                                      int daysBefore,
                                                      VaccinationReminderDispatch.ReminderType reminderType,
                                                      String reminderLabel) {
        LocalDate targetValidUntil = runDate.plusDays(daysBefore);
        List<VaccinationRepository.VaccinationReminderCandidateProjection> candidates =
                vaccinationRepository.findReminderCandidatesByValidUntil(targetValidUntil);

        int sent = 0;
        int failed = 0;
        int skipped = 0;

        for (VaccinationRepository.VaccinationReminderCandidateProjection candidate : candidates) {
            Long vaccinationId = candidate.getVaccinationId();
            if (vaccinationId == null || candidate.getOwnerEmail() == null || candidate.getOwnerEmail().isBlank()) {
                skipped++;
                continue;
            }

            boolean alreadySent = dispatchRepository.existsByVaccinationIdAndReminderTypeAndStatus(
                    vaccinationId,
                    reminderType,
                    VaccinationReminderDispatch.DispatchStatus.SENT
            );

            if (alreadySent) {
                skipped++;
                continue;
            }

            boolean delivered = emailService.sendVaccinationValidityReminder(
                    candidate.getOwnerEmail(),
                    candidate.getPetName(),
                    candidate.getVaccineName(),
                    candidate.getValidUntil(),
                    reminderLabel
            );

            VaccinationReminderDispatch dispatch = VaccinationReminderDispatch.builder()
                    .vaccination(Vaccination.builder().id(vaccinationId).build())
                    .reminderDate(runDate)
                    .reminderType(reminderType)
                    .status(delivered
                            ? VaccinationReminderDispatch.DispatchStatus.SENT
                            : VaccinationReminderDispatch.DispatchStatus.FAILED)
                    .errorMessage(delivered ? null : "Email delivery failed")
                    .build();
            dispatchRepository.save(dispatch);

            if (delivered) {
                sent++;
            } else {
                failed++;
            }
        }

        return new ReminderBatchResult(sent, failed, skipped);
    }

    public record ReminderRunResult(LocalDate runDate, int sentCount, int failedCount, int skippedCount, boolean disabled) {
    }

    private record ReminderBatchResult(int sent, int failed, int skipped) {
    }
}

