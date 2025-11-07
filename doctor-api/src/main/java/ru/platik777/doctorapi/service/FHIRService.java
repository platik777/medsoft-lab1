package ru.platik777.doctorapi.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.platik777.doctorapi.entity.Appointment;
import ru.platik777.doctorapi.repository.AppointmentRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FHIRService {

    private final AppointmentRepository appointmentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FhirContext fhirContext = FhirContext.forR4();

    public Appointment createAppointmentFromFHIR(String fhirJson) {
        log.info("=== Получен FHIR Appointment ===");
        log.info("Исходный JSON:\n{}", fhirJson);
        log.info("================================");

        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        org.hl7.fhir.r4.model.Appointment fhirAppointment = parser.parseResource(
                org.hl7.fhir.r4.model.Appointment.class, fhirJson
        );

        Appointment appointment = new Appointment();
        appointment.setId(fhirAppointment.hasId() ?
                fhirAppointment.getIdElement().getIdPart() : UUID.randomUUID().toString());

        // Extract patient info
        if (fhirAppointment.hasParticipant()) {
            for (org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent participant :
                    fhirAppointment.getParticipant()) {
                if (participant.hasActor() && participant.getActor().hasReference()) {
                    String ref = participant.getActor().getReference();
                    if (ref.startsWith("Patient/")) {
                        String patientId = ref.substring(8);
                        appointment.setPatientId(Long.parseLong(patientId));
                    }
                    if (participant.getActor().hasDisplay()) {
                        appointment.setPatientName(participant.getActor().getDisplay());
                    }
                }
            }
        }

        appointment.setStatus(fhirAppointment.hasStatus() ?
                fhirAppointment.getStatus().toCode() : "pending");

        if (fhirAppointment.hasStart()) {
            LocalDateTime startTime = fhirAppointment.getStart()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            appointment.setAppointmentTime(startTime);
        } else {
            appointment.setAppointmentTime(LocalDateTime.now());
        }

        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());

        if (fhirAppointment.hasComment()) {
            appointment.setNotes(fhirAppointment.getComment());
        }

        appointmentRepository.save(appointment);
        log.info("Приём сохранён: {} для пациента {}", appointment.getId(), appointment.getPatientName());

        broadcastAppointments();
        return appointment;
    }

    public String sendEncounterToHIS(Appointment appointment) {
        Encounter encounter = new Encounter();
        encounter.setId(appointment.getId());

        // Status
        Encounter.EncounterStatus status;
        switch (appointment.getStatus()) {
            case "in-progress":
                status = Encounter.EncounterStatus.INPROGRESS;
                break;
            case "completed":
                status = Encounter.EncounterStatus.FINISHED;
                break;
            case "cancelled":
                status = Encounter.EncounterStatus.CANCELLED;
                break;
            default:
                status = Encounter.EncounterStatus.PLANNED;
        }
        encounter.setStatus(status);

        // Patient reference
        Reference patientRef = new Reference();
        patientRef.setReference("Patient/" + appointment.getPatientId());
        patientRef.setDisplay(appointment.getPatientName());
        encounter.setSubject(patientRef);

        // Period
        Period period = new Period();
        period.setStart(Date.from(appointment.getAppointmentTime()
                .atZone(ZoneId.systemDefault()).toInstant()));
        if ("completed".equals(appointment.getStatus())) {
            period.setEnd(Date.from(appointment.getUpdatedAt()
                    .atZone(ZoneId.systemDefault()).toInstant()));
        }
        encounter.setPeriod(period);

        // Class
        Coding classCoding = new Coding();
        classCoding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
        classCoding.setCode("AMB");
        classCoding.setDisplay("ambulatory");
        encounter.setClass_(classCoding);

        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        String encounterJson = parser.encodeResourceToString(encounter);

        log.info("=== Отправка FHIR Encounter в HIS ===");
        log.info("Исходный JSON:\n{}", encounterJson);
        log.info("====================================");

        return encounterJson;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentTimeDesc();
    }

    public Appointment updateAppointmentStatus(String id, String status, String notes) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Приём не найден"));

        appointment.setStatus(status);
        appointment.setUpdatedAt(LocalDateTime.now());
        if (notes != null && !notes.isEmpty()) {
            appointment.setNotes(notes);
        }

        appointmentRepository.save(appointment);

        // Send Encounter to HIS
        sendEncounterToHIS(appointment);

        broadcastAppointments();
        return appointment;
    }

    private void broadcastAppointments() {
        List<Appointment> appointments = getAllAppointments();
        messagingTemplate.convertAndSend("/topic/appointments", appointments);
        log.info("Отправлено обновление приёмов через WebSocket ({} приёмов)", appointments.size());
    }
}