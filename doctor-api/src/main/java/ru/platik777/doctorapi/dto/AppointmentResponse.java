package ru.platik777.doctorapi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientFirstName;
    private String patientLastName;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime appointmentDateTime;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}