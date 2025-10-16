package ru.platik777.receptionapi.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientRequest {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
}
