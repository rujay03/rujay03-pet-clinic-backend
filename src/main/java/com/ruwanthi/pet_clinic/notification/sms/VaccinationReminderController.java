package com.ruwanthi.pet_clinic.notification.sms;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications/vaccination-reminders")
public class VaccinationReminderController {

    private final VaccinationReminderService vaccinationReminderService;

    public VaccinationReminderController(VaccinationReminderService vaccinationReminderService) {
        this.vaccinationReminderService = vaccinationReminderService;
    }

    @PostMapping("/send-now")
    public ResponseEntity<VaccinationReminderService.ReminderRunResult> sendNow(
            @RequestParam(value = "runDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate
    ) {
        LocalDate effectiveRunDate = runDate != null ? runDate : LocalDate.now();
        VaccinationReminderService.ReminderRunResult result = vaccinationReminderService.sendDueReminders(effectiveRunDate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "feature", "vaccination-reminder-email",
                "message", "Use POST /send-now to trigger reminder emails"
        ));
    }
}

