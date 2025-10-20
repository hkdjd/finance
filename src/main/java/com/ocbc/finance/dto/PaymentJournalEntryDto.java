package com.ocbc.finance.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款会计分录DTO，包含支付时间戳
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PaymentJournalEntryDto extends JournalEntryDto{
    private LocalDateTime paymentTimestamp; // 支付操作时间戳
    private Integer entryOrder; // 分录顺序
    private String entryType; // 分录类型
    private String description; // 分录描述
    private LocalDateTime updatedAt;
    private String amortizationPeriod; // 摊销期间，用于分组显示
    private Long paymentId; // 付款ID，用于按付款分组

    // Custom constructor to match the calling signature
    public PaymentJournalEntryDto(LocalDate bookingDate, String account, BigDecimal dr, BigDecimal cr, 
                                  String memo, LocalDateTime createdAt, LocalDateTime paymentTimestamp, 
                                  Integer entryOrder, String entryType, String description, 
                                  LocalDateTime updatedAt, String amortizationPeriod, Long paymentId) {
        super(bookingDate, account, dr, cr, memo, createdAt);
        this.paymentTimestamp = paymentTimestamp;
        this.entryOrder = entryOrder;
        this.entryType = entryType;
        this.description = description;
        this.updatedAt = updatedAt;
        this.amortizationPeriod = amortizationPeriod;
        this.paymentId = paymentId;
    }

}
