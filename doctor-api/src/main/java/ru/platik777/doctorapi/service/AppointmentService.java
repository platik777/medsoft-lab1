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

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Получение FHIR Appointment ресурса и сохранение в БД
     */
    public void receiveFhirAppointment(String fhirJson) {
        log.info("=== Получен FHIR Appointment ресурс ===");
        log.info("Исходный FHIR JSON:\n{}", fhirJson);
        log.info("=========================================");

        try {
            IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
            org.hl7.fhir.r4.model.Appointment fhirAppointment =
                    parser.parseResource(org.hl7.fhir.r4.model.Appointment.class, fhirJson);

            Appointment appointment = convertFhirToEntity(fhirAppointment);
            appointment = appointmentRepository.save(appointment);

            log.info("Посещение сохранено в БД: ID={}, Пациент={} {}, Врач={}, Время={}",
                    appointment.getId(),
                    appointment.getPatientFirstName(),
                    appointment.getPatientLastName(),
                    appointment.getDoctorName(),
                    appointment.getAppointmentDateTime());

            // Broadcast через WebSocket для real-time обновления
            broadcastAppointments();

        } catch (Exception e) {
            log.error("Ошибка при обработке FHIR Appointment", e);
            throw new RuntimeException("Failed to process FHIR Appointment", e);
        }
    }

    /**
     * Конвертация FHIR Appointment в Entity
     */
    private Appointment convertFhirToEntity(org.hl7.fhir.r4.model.Appointment fhirAppointment) {
        Appointment appointment = new Appointment();

        // ID (если есть в FHIR)
        if (fhirAppointment.hasId()) {
            try {
                appointment.setId(Long.parseLong(fhirAppointment.getIdElement().getIdPart()));
            } catch (NumberFormatException e) {
                log.warn("Не удалось распарсить ID из FHIR, будет использован автогенерируемый");
            }
        }

        // Дата и время
        if (fhirAppointment.hasStart()) {
            Date start = fhirAppointment.getStart();
            appointment.setAppointmentDateTime(
                    LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault())
            );
        }

        // Статус
        if (fhirAppointment.hasStatus()) {
            appointment.setStatus(fhirAppointment.getStatus().toCode().toUpperCase());
        } else {
            appointment.setStatus("SCHEDULED");
        }

        // Причина (из расширений или description)
        if (fhirAppointment.hasDescription()) {
            appointment.setReason(fhirAppointment.getDescription());
        } else if (fhirAppointment.hasComment()) {
            appointment.setReason(fhirAppointment.getComment());
        } else {
            appointment.setReason("Не указано");
        }

        // Парсинг участников (пациент и врач)
        for (org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent participant :
                fhirAppointment.getParticipant()) {

            if (participant.hasActor() && participant.getActor().hasReference()) {
                String reference = participant.getActor().getReference();
                String display = participant.getActor().hasDisplay() ?
                        participant.getActor().getDisplay() : "Unknown";

                if (reference.startsWith("Patient/")) {
                    String patientIdStr = reference.replace("Patient/", "");
                    try {
                        appointment.setPatientId(Long.parseLong(patientIdStr));
                    } catch (NumberFormatException e) {
                        log.warn("Не удалось распарсить Patient ID: {}", patientIdStr);
                    }

                    // Парсинг имени пациента из display
                    String[] nameParts = display.split(" ");
                    if (nameParts.length >= 2) {
                        appointment.setPatientFirstName(nameParts[0]);
                        appointment.setPatientLastName(nameParts[1]);
                    } else {
                        appointment.setPatientFirstName(display);
                        appointment.setPatientLastName("");
                    }
                }

                if (reference.startsWith("Practitioner/")) {
                    String doctorIdStr = reference.replace("Practitioner/", "");
                    try {
                        appointment.setDoctorId(Long.parseLong(doctorIdStr));
                    } catch (NumberFormatException e) {
                        log.warn("Не удалось распарсить Practitioner ID: {}", doctorIdStr);
                    }
                    appointment.setDoctorName(display);
                }
            }
        }

        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());

        return appointment;
    }

    /**
     * Получить все посещения
     */
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    /**
     * Получить посещения конкретного врача
     */
    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    /**
     * Получить посещения конкретного пациента
     */
    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    /**
     * Обновить статус посещения
     */
    public Appointment updateAppointmentStatus(Long id, String newStatus) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));

        appointment.setStatus(newStatus);
        appointment.setUpdatedAt(LocalDateTime.now());

        appointment = appointmentRepository.save(appointment);

        log.info("Статус посещения обновлён: ID={}, новый статус={}", id, newStatus);

        // Broadcast для real-time обновления
        broadcastAppointments();

        return appointment;
    }

    /**
     * Broadcast всех посещений через WebSocket
     */
    private void broadcastAppointments() {
        List<Appointment> appointments = getAllAppointments();
        messagingTemplate.convertAndSend("/topic/appointments", appointments);
        log.info("Отправлено обновление списка посещений через WebSocket ({} посещений)",
                appointments.size());
    }

    /**
     * Конвертация Entity в FHIR Appointment (для отправки)
     */
    public String convertEntityToFhir(Appointment appointment) {
        org.hl7.fhir.r4.model.Appointment fhirAppointment =
                new org.hl7.fhir.r4.model.Appointment();

        // ID
        fhirAppointment.setId(appointment.getId().toString());

        // Статус
        fhirAppointment.setStatus(
                org.hl7.fhir.r4.model.Appointment.AppointmentStatus.fromCode(
                        appointment.getStatus().toLowerCase()
                )
        );

        // Дата и время
        Date start = Date.from(
                appointment.getAppointmentDateTime().atZone(ZoneId.systemDefault()).toInstant()
        );
        fhirAppointment.setStart(start);

        // Описание (причина)
        fhirAppointment.setDescription(appointment.getReason());

        // Пациент
        Reference patientRef = new Reference();
        patientRef.setReference("Patient/" + appointment.getPatientId());
        patientRef.setDisplay(appointment.getPatientFirstName() + " " +
                appointment.getPatientLastName());

        org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent patientParticipant =
                new org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent();
        patientParticipant.setActor(patientRef);
        patientParticipant.setStatus(
                org.hl7.fhir.r4.model.Appointment.ParticipationStatus.ACCEPTED
        );
        fhirAppointment.addParticipant(patientParticipant);

        // Врач
        Reference practitionerRef = new Reference();
        practitionerRef.setReference("Practitioner/" + appointment.getDoctorId());
        practitionerRef.setDisplay(appointment.getDoctorName());

        org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent doctorParticipant =
                new org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent();
        doctorParticipant.setActor(practitionerRef);
        doctorParticipant.setStatus(
                org.hl7.fhir.r4.model.Appointment.ParticipationStatus.ACCEPTED
        );
        fhirAppointment.addParticipant(doctorParticipant);

        // Конвертация в JSON
        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        String fhirJson = parser.encodeResourceToString(fhirAppointment);

        log.info("=== Сгенерирован FHIR Appointment ресурс ===");
        log.info("FHIR JSON:\n{}", fhirJson);
        log.info("============================================");

        return fhirJson;
    }
}