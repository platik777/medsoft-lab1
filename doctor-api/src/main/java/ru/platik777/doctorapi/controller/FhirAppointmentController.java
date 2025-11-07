package ru.platik777.doctorapi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.doctorapi.service.AppointmentService;

@RestController
@RequestMapping("/fhir/appointments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class FhirAppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Эндпоинт для приема FHIR Appointment ресурсов от HIS
     */
    @PostMapping(consumes = "application/fhir+json")
    public ResponseEntity<String> receiveFhirAppointment(@RequestBody String fhirJson) {
        log.info("=== Получен запрос на /fhir/appointments ===");

        try {
            appointmentService.receiveFhirAppointment(fhirJson);
            return ResponseEntity.ok("Appointment received and processed successfully");
        } catch (Exception e) {
            log.error("Ошибка при обработке FHIR Appointment", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to process FHIR Appointment: " + e.getMessage());
        }
    }
}