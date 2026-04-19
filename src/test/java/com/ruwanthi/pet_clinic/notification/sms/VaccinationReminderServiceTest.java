package com.ruwanthi.pet_clinic.notification.sms;

import com.ruwanthi.pet_clinic.auth.service.EmailService;
import com.ruwanthi.pet_clinic.medical.repo.VaccinationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccinationReminderServiceTest {

    @Mock
    private VaccinationRepository vaccinationRepository;

    @Mock
    private VaccinationReminderDispatchRepository dispatchRepository;

    @Mock
    private EmailService emailService;

    private VaccinationReminderService service;

    @BeforeEach
    void setUp() {
        service = new VaccinationReminderService(vaccinationRepository, dispatchRepository, emailService, true);
    }

    @Test
    void sendsWeekBeforeReminderAndPersistsSentDispatch() {
        LocalDate runDate = LocalDate.of(2026, 4, 19);
        LocalDate weekTarget = runDate.plusDays(7);

        VaccinationRepository.VaccinationReminderCandidateProjection weekCandidate =
                candidate(10L, "owner@example.com", "Luna", "Rabies", weekTarget);

        when(vaccinationRepository.findReminderCandidatesByValidUntil(weekTarget)).thenReturn(List.of(weekCandidate));
        when(vaccinationRepository.findReminderCandidatesByValidUntil(runDate.plusDays(1))).thenReturn(List.of());
        when(dispatchRepository.existsByVaccinationIdAndReminderTypeAndStatus(
                10L,
                VaccinationReminderDispatch.ReminderType.WEEK_BEFORE,
                VaccinationReminderDispatch.DispatchStatus.SENT
        )).thenReturn(false);
        when(emailService.sendVaccinationValidityReminder(any(), any(), any(), any(), any())).thenReturn(true);

        VaccinationReminderService.ReminderRunResult result = service.sendDueReminders(runDate);

        assertFalse(result.disabled());
        assertEquals(1, result.sentCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.skippedCount());

        ArgumentCaptor<VaccinationReminderDispatch> captor = ArgumentCaptor.forClass(VaccinationReminderDispatch.class);
        verify(dispatchRepository).save(captor.capture());

        VaccinationReminderDispatch saved = captor.getValue();
        assertEquals(10L, saved.getVaccination().getId());
        assertEquals(VaccinationReminderDispatch.ReminderType.WEEK_BEFORE, saved.getReminderType());
        assertEquals(VaccinationReminderDispatch.DispatchStatus.SENT, saved.getStatus());
        assertEquals(runDate, saved.getReminderDate());
    }

    @Test
    void skipsWhenAlreadySent() {
        LocalDate runDate = LocalDate.of(2026, 4, 19);
        LocalDate dayTarget = runDate.plusDays(1);

        VaccinationRepository.VaccinationReminderCandidateProjection dayCandidate =
                candidate(20L, "owner@example.com", "Milo", "Parvo", dayTarget);

        when(vaccinationRepository.findReminderCandidatesByValidUntil(runDate.plusDays(7))).thenReturn(List.of());
        when(vaccinationRepository.findReminderCandidatesByValidUntil(dayTarget)).thenReturn(List.of(dayCandidate));
        when(dispatchRepository.existsByVaccinationIdAndReminderTypeAndStatus(
                20L,
                VaccinationReminderDispatch.ReminderType.DAY_BEFORE,
                VaccinationReminderDispatch.DispatchStatus.SENT
        )).thenReturn(true);

        VaccinationReminderService.ReminderRunResult result = service.sendDueReminders(runDate);

        assertEquals(0, result.sentCount());
        assertEquals(0, result.failedCount());
        assertEquals(1, result.skippedCount());

        verify(emailService, never()).sendVaccinationValidityReminder(any(), any(), any(), any(), any());
        verify(dispatchRepository, never()).save(any());
    }

    @Test
    void recordsFailedDispatchWhenEmailFails() {
        LocalDate runDate = LocalDate.of(2026, 4, 19);
        LocalDate dayTarget = runDate.plusDays(1);

        VaccinationRepository.VaccinationReminderCandidateProjection dayCandidate =
                candidate(30L, "owner@example.com", "Rocky", "Distemper", dayTarget);

        when(vaccinationRepository.findReminderCandidatesByValidUntil(runDate.plusDays(7))).thenReturn(List.of());
        when(vaccinationRepository.findReminderCandidatesByValidUntil(dayTarget)).thenReturn(List.of(dayCandidate));
        when(dispatchRepository.existsByVaccinationIdAndReminderTypeAndStatus(
                30L,
                VaccinationReminderDispatch.ReminderType.DAY_BEFORE,
                VaccinationReminderDispatch.DispatchStatus.SENT
        )).thenReturn(false);
        when(emailService.sendVaccinationValidityReminder(
                eq("owner@example.com"),
                eq("Rocky"),
                eq("Distemper"),
                eq(dayTarget),
                eq("1 day before")
        )).thenReturn(false);

        VaccinationReminderService.ReminderRunResult result = service.sendDueReminders(runDate);

        assertEquals(0, result.sentCount());
        assertEquals(1, result.failedCount());
        assertEquals(0, result.skippedCount());

        ArgumentCaptor<VaccinationReminderDispatch> captor = ArgumentCaptor.forClass(VaccinationReminderDispatch.class);
        verify(dispatchRepository).save(captor.capture());
        assertEquals(VaccinationReminderDispatch.DispatchStatus.FAILED, captor.getValue().getStatus());
    }

    private VaccinationRepository.VaccinationReminderCandidateProjection candidate(Long vaccinationId,
                                                                                   String ownerEmail,
                                                                                   String petName,
                                                                                   String vaccineName,
                                                                                   LocalDate validUntil) {
        return new VaccinationRepository.VaccinationReminderCandidateProjection() {
            @Override
            public Long getVaccinationId() {
                return vaccinationId;
            }

            @Override
            public String getOwnerEmail() {
                return ownerEmail;
            }

            @Override
            public String getPetName() {
                return petName;
            }

            @Override
            public String getVaccineName() {
                return vaccineName;
            }

            @Override
            public LocalDate getValidUntil() {
                return validUntil;
            }
        };
    }
}

