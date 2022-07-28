package dev.shuktika.processpension.service;

import dev.shuktika.processpension.client.PensionerClient;
import dev.shuktika.processpension.configuration.PropertyValueConfiguration;
import dev.shuktika.processpension.exception.AadharMismatchException;
import dev.shuktika.processpension.exception.PensionerDetailServiceException;
import dev.shuktika.processpension.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PensionService {
    private final PensionerClient pensionerClient;
    private final PropertyValueConfiguration propertyValueConfiguration;

    private PensionDetail calculatePension(Pensioner pensioner) {
        PensionType pensionType = PensionType.getPensionType(pensioner.getPensionType());
        BankType bankType = BankType.getBankType(pensioner.getBankDetails().getBankType());
        Double salary = Double.valueOf(pensioner.getSalaryEarned());
        Double allowances = Double.valueOf(pensioner.getAllowances());
        Double pensionPercentage = propertyValueConfiguration.getPension(pensionType);
        Integer bankServiceCharge = propertyValueConfiguration.getBankCharges(bankType);
        double pensionAmount = salary * pensionPercentage + allowances;

        return PensionDetail.builder()
                .pensionAmount(pensionAmount)
                .bankServiceCharge(bankServiceCharge)
                .build();
    }

    public PensionDetail processPension(ProcessPensionInput processPensionInput) {
        Long aadharNumber = processPensionInput.getAadharNumber();
        ResponseEntity<Pensioner> pensionerResponseEntity = pensionerClient.getPensioner(aadharNumber);
        PensionDetail pensionDetail = null;

        if (pensionerResponseEntity.getStatusCode().is2xxSuccessful() && pensionerResponseEntity.hasBody()) {
            Pensioner pensioner = pensionerResponseEntity.getBody();
            pensionDetail = calculatePension(Objects.requireNonNull(pensioner));
        } else if (pensionerResponseEntity.getStatusCode().is4xxClientError()) {
            throw new AadharMismatchException(String.format("Aadhar number %s not found", aadharNumber));
        } else if (pensionerResponseEntity.getStatusCode().is5xxServerError()) {
            throw new PensionerDetailServiceException("Pensioner Detail Service internal server error");
        }

        return pensionDetail;
    }
}
