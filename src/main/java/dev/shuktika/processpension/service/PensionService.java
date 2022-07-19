package dev.shuktika.processpension.service;

import dev.shuktika.processpension.client.PensionerClient;
import dev.shuktika.processpension.exception.AadharMismatchException;
import dev.shuktika.processpension.exception.PensionerDetailServiceException;
import dev.shuktika.processpension.model.PensionDetail;
import dev.shuktika.processpension.model.Pensioner;
import dev.shuktika.processpension.model.ProcessPensionInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PensionService {
    private final PensionerClient pensionerClient;

    private PensionDetail calculatePension(Pensioner pensioner) {
        Double amount = Double.valueOf(pensioner.getAllowances());
        if (pensioner.getPensionType().equals("self")) {
            amount += pensioner.getSalaryEarned() * 0.8;
        } else {
            amount += pensioner.getSalaryEarned() * 0.5;
        }
        Integer bankServiceCharge = pensioner.getBankDetails().getBankType().equals("public") ? 500 : 550;
        return PensionDetail.builder()
                .pensionAmount(amount)
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
