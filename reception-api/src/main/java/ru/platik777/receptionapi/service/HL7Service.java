package ru.platik777.receptionapi.service;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ADT_A03;
import ca.uhn.hl7v2.parser.Parser;
import jakarta.annotation.PostConstruct;
import ru.platik777.receptionapi.entity.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class HL7Service {

    @Value("${hl7.server.host:localhost}")
    private String hl7ServerHost;

    @Value("${hl7.server.port:2575}")
    private int hl7ServerPort;

    private HapiContext context;
    private Parser parser;

    @PostConstruct
    public void init() {
        this.context = new DefaultHapiContext();
        this.parser = context.getPipeParser();
    }

    /**
     * Создаёт HL7 сообщение ADT^A01 (Patient Admission)
     */
    public void sendPatientAdmission(Patient patient) throws HL7Exception, IOException, LLPException {
        ADT_A01 message = new ADT_A01();

        // MSH Segment
        message.initQuickstart("ADT", "A01", "P");
        message.getMSH().getSendingApplication().getNamespaceID().setValue("RECEPTION");
        message.getMSH().getSendingFacility().getNamespaceID().setValue("HOSPITAL");
        message.getMSH().getReceivingApplication().getNamespaceID().setValue("HIS");
        message.getMSH().getReceivingFacility().getNamespaceID().setValue("HOSPITAL");

        // PID Segment
        message.getPID().getPatientIdentifierList(0).getIDNumber().setValue(patient.getId().toString());
        message.getPID().getPatientName(0).getFamilyName().getSurname().setValue(patient.getLastName());
        message.getPID().getPatientName(0).getGivenName().setValue(patient.getFirstName());
        message.getPID().getDateTimeOfBirth().getTime().setValue(
                patient.getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );

        // EVN Segment
        message.getEVN().getRecordedDateTime().getTime().setValue(
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        );

        sendMessage(message);
    }

    /**
     * Создаёт HL7 сообщение ADT^A03 (Patient Discharge)
     */
    public void sendPatientDischarge(Patient patient) throws HL7Exception, IOException, LLPException {
        ADT_A03 message = new ADT_A03();

        // MSH Segment
        message.initQuickstart("ADT", "A03", "P");
        message.getMSH().getSendingApplication().getNamespaceID().setValue("RECEPTION");
        message.getMSH().getSendingFacility().getNamespaceID().setValue("HOSPITAL");
        message.getMSH().getReceivingApplication().getNamespaceID().setValue("HIS");
        message.getMSH().getReceivingFacility().getNamespaceID().setValue("HOSPITAL");

        // PID Segment
        message.getPID().getPatientIdentifierList(0).getIDNumber().setValue(patient.getId().toString());
        message.getPID().getPatientName(0).getFamilyName().getSurname().setValue(patient.getLastName());
        message.getPID().getPatientName(0).getGivenName().setValue(patient.getFirstName());
        message.getPID().getDateTimeOfBirth().getTime().setValue(
                patient.getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );

        // EVN Segment
        message.getEVN().getRecordedDateTime().getTime().setValue(
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        );

        sendMessage(message);
    }

    /**
     * Отправляет HL7 сообщение на сервер HIS
     */
    private void sendMessage(Message message) throws HL7Exception, IOException, LLPException {
        String encodedMessage = parser.encode(message);

        // Логируем исходное HL7 сообщение (требование №7)
        log.info("=== Отправка HL7 сообщения ===");
        log.info("Исходный формат HL7:\n{}", encodedMessage);
        log.info("==============================");

        Connection connection = null;
        try {
            connection = context.newClient(hl7ServerHost, hl7ServerPort, false);
            Initiator initiator = connection.getInitiator();
            Message response = initiator.sendAndReceive(message);

            String responseString = parser.encode(response);
            log.info("=== Ответ от HIS ===");
            log.info("{}", responseString);
            log.info("===================");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("Ошибка при закрытии соединения", e);
                }
            }
        }
    }
}