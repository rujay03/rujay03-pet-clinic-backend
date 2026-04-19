package com.ruwanthi.pet_clinic.notification.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class VaccinationReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VaccinationReminderScheduler.class);

    private final VaccinationReminderService vaccinationReminderService;

    public VaccinationReminderScheduler(VaccinationReminderService vaccinationReminderService) {
        this.vaccinationReminderService = vaccinationReminderService;
    }

    @Scheduled(cron = "${app.vaccination-reminders.cron:0 0 9 * * *}")
    public void sendDailyVaccinationReminders() {
        LocalDate runDate = LocalDate.now();
        VaccinationReminderService.ReminderRunResult result = vaccinationReminderService.sendDueReminders(runDate);
        logger.info("Scheduled vaccination reminder run for {} complete. disabled={}, sent={}, failed={}, skipped={}",
                runDate,
                result.disabled(),
                result.sentCount(),
                result.failedCount(),
                result.skippedCount());
    }
}

