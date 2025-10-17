package ru.platik777.hisserver;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ADT_A03;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.platik777.hisserver.service.PatientService;

import java.util.Map;

@SpringBootApplication
@Slf4j
public class HisServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HisServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner startHL7Server(PatientService patientService) {
        return args -> {
            int port = 2575;

            try (HapiContext context = new DefaultHapiContext()) {

                HL7Service server = context.newServer(port, false);

                server.registerApplication("ADT", "*", new ReceivingApplication<>() {
                    @Override
                    public Message processMessage(Message message, Map<String, Object> metadata)
                            throws ReceivingApplicationException, HL7Exception {

                        Parser parser = context.getPipeParser();
                        String encodedMessage = parser.encode(message);

                        try {
                            String messageType = message.getName();

                            if (messageType.equals("ADT_A01")) {
                                log.info("=== Получено HL7 сообщение ADT^A01 ===");
                                log.info("Исходный формат: {}", encodedMessage);
                                log.info("======================================");

                                ADT_A01 adtMessage = (ADT_A01) message;

                                String patientId = adtMessage.getPID().getPatientIdentifierList(0)
                                        .getIDNumber().getValue();
                                String lastName = adtMessage.getPID().getPatientName(0)
                                        .getFamilyName().getSurname().getValue();
                                String firstName = adtMessage.getPID().getPatientName(0)
                                        .getGivenName().getValue();
                                String dob = adtMessage.getPID().getDateTimeOfBirth()
                                        .getTime().getValue();

                                patientService.addPatient(Long.parseLong(patientId), firstName, lastName, dob);

                                log.info("Пациент добавлен в HIS: {} {} (ID: {})", firstName, lastName, patientId);

                            } else if (messageType.equals("ADT_A03")) {
                                log.info("=== Получено HL7 сообщение ADT^A03 ===");
                                log.info("Исходный формат: {}", encodedMessage);
                                log.info("======================================");

                                ADT_A03 adtMessage = (ADT_A03) message;

                                String patientId = adtMessage.getPID().getPatientIdentifierList(0).getIDNumber().getValue();

                                patientService.removePatient(Long.parseLong(patientId));

                                log.info("Пациент удалён из HIS (ID: {})", patientId);
                            }

                        } catch (Exception e) {
                            log.error("Ошибка обработки HL7 сообщения", e);
                            throw new ReceivingApplicationException(e);
                        }

                        try {
                            return message.generateACK();
                        } catch (Exception e) {
                            log.error("Ошибка генерации ACK", e);
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public boolean canProcess(Message message) {
                        return true;
                    }
                });
                
                server.startAndWait();
                log.info("HL7 Server запущен на порту {}", port);
            }
        };
    }
}