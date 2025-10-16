package ru.platik777.receptionapi.controller;

import ru.platik777.receptionapi.dto.PatientRequest;
import ru.platik777.receptionapi.dto.PatientResponse;
import ru.platik777.receptionapi.entity.Patient;
import ru.platik777.receptionapi.repository.PatientRepository;
import ru.platik777.receptionapi.service.HL7Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientRepository patientRepository;
    private final HL7Service hl7Service;

    @PostMapping
    public ResponseEntity<PatientResponse> addPatient(@RequestBody PatientRequest request) {
        try {
            Patient patient = new Patient();
            patient.setFirstName(request.getFirstName());
            patient.setLastName(request.getLastName());
            patient.setDateOfBirth(request.getDateOfBirth());
            patient.setCreatedAt(LocalDate.now());

            patient = patientRepository.save(patient);

            // Отправляем HL7 сообщение
            hl7Service.sendPatientAdmission(patient);

            log.info("Пациент добавлен: {} {}", patient.getFirstName(), patient.getLastName());

            return ResponseEntity.ok(toResponse(patient));
        } catch (Exception e) {
            log.error("Ошибка при добавлении пациента", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        try {
            Patient patient = patientRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Пациент не найден"));

            // Отправляем HL7 сообщение о выписке
            hl7Service.sendPatientDischarge(patient);

            patientRepository.deleteById(id);
            log.info("Пациент удалён: {} {}", patient.getFirstName(), patient.getLastName());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка при удалении пациента", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return ResponseEntity.ok(
                patients.stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList())
        );
    }

    private PatientResponse toResponse(Patient patient) {
        PatientResponse response = new PatientResponse();
        response.setId(patient.getId());
        response.setFirstName(patient.getFirstName());
        response.setLastName(patient.getLastName());
        response.setDateOfBirth(patient.getDateOfBirth());
        return response;
    }
}
