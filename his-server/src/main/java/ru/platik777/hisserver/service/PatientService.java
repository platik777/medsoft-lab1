package ru.platik777.hisserver.service;

import ru.platik777.hisserver.entity.Patient;
import ru.platik777.hisserver.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void addPatient(Long id, String firstName, String lastName, String dobString) {
        LocalDate dob = LocalDate.parse(dobString, DateTimeFormatter.ofPattern("yyyyMMdd"));

        Patient patient = new Patient();
        patient.setId(id);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(dob);
        patient.setCreatedAt(LocalDate.now());

        patientRepository.save(patient);

        // Отправляем обновление через WebSocket
        broadcastPatients();
    }

    public void removePatient(Long id) {
        patientRepository.deleteById(id);

        // Отправляем обновление через WebSocket
        broadcastPatients();
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    private void broadcastPatients() {
        List<Patient> patients = getAllPatients();
        messagingTemplate.convertAndSend("/topic/patients", patients);
        log.info("Отправлено обновление списка пациентов через WebSocket ({} пациентов)",
                patients.size());
    }
}
