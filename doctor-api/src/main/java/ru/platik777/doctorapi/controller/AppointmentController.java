package ru.platik777.doctorapi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.doctorapi.dto.AppointmentResponse;
import ru.platik777.doctorapi.entity.Appointment;
import ru.platik777.doctorapi.service.AppointmentService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Получить все посещения
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        List<AppointmentResponse> responses = appointments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.info("Возвращено {} посещений", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Получить посещения конкретного врача
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByDoctor(
            @PathVariable Long doctorId) {

        List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
        List<AppointmentResponse> responses = appointments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.info("Возвращено {} посещений для врача ID={}", responses.size(), doctorId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Получить посещения конкретного пациента
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByPatient(
            @PathVariable Long patientId) {

        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);
        List<AppointmentResponse> responses = appointments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.info("Возвращено {} посещений для пациента ID={}", responses.size(), patientId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Обновить статус посещения
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {

        Appointment appointment = appointmentService.updateAppointmentStatus(id, request.status());

        log.info("Статус посещения ID={} обновлён на {}", id, request.status());
        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Получить посещение по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        // Можно добавить метод в сервис, если нужно
        List<Appointment> all = appointmentService.getAllAppointments();
        Appointment appointment = all.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));

        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Конвертация Entity в Response DTO
     */
    private AppointmentResponse toResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setPatientId(appointment.getPatientId());
        response.setPatientFirstName(appointment.getPatientFirstName());
        response.setPatientLastName(appointment.getPatientLastName());
        response.setDoctorId(appointment.getDoctorId());
        response.setDoctorName(appointment.getDoctorName());
        response.setAppointmentDateTime(appointment.getAppointmentDateTime());
        response.setReason(appointment.getReason());
        response.setStatus(appointment.getStatus());
        response.setCreatedAt(appointment.getCreatedAt());
        response.setUpdatedAt(appointment.getUpdatedAt());
        return response;
    }

    /**
     * DTO для обновления статуса
     */
    public record StatusUpdateRequest(String status) {}
}