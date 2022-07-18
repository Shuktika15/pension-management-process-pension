package dev.shuktika.processpension.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PensionDetail {
    private Integer pensionAmount;
    private Integer bankServiceCharge;
}
