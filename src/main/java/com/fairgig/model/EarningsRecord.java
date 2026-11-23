package com.fairgig.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One logged earning (table "earnings_records"). Money is always BigDecimal, never double. */
public class EarningsRecord {

    private Integer recordId;
    private Integer workerId;
    private Integer platformId;
    private Integer importId;                 // only for CSV rows
    private LocalDate shiftDate;
    private BigDecimal hoursWorked;
    private BigDecimal grossEarnings;
    private BigDecimal deductions;
    private BigDecimal netReceived;
    private VerificationStatus status = VerificationStatus.PENDING;   // FR-11
    private EntryMethod entryMethod = EntryMethod.MANUAL;
    private LocalDateTime createdAt;

    public EarningsRecord() {}

    /** Net received per hour worked, rounded to 2 decimals (0 when no hours). */
    public BigDecimal calculateEffectiveRate() {
        if (netReceived == null || hoursWorked == null || hoursWorked.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return netReceived.divide(hoursWorked, 2, RoundingMode.HALF_UP);
    }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public Integer getWorkerId() { return workerId; }
    public void setWorkerId(Integer workerId) { this.workerId = workerId; }

    public Integer getPlatformId() { return platformId; }
    public void setPlatformId(Integer platformId) { this.platformId = platformId; }

    public Integer getImportId() { return importId; }
    public void setImportId(Integer importId) { this.importId = importId; }

    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }

    public BigDecimal getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(BigDecimal hoursWorked) { this.hoursWorked = hoursWorked; }

    public BigDecimal getGrossEarnings() { return grossEarnings; }
    public void setGrossEarnings(BigDecimal grossEarnings) { this.grossEarnings = grossEarnings; }

    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }

    public BigDecimal getNetReceived() { return netReceived; }
    public void setNetReceived(BigDecimal netReceived) { this.netReceived = netReceived; }

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public EntryMethod getEntryMethod() { return entryMethod; }
    public void setEntryMethod(EntryMethod entryMethod) { this.entryMethod = entryMethod; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof EarningsRecord that && recordId != null && recordId.equals(that.recordId));
    }

    @Override
    public int hashCode() {
        return recordId == null ? System.identityHashCode(this) : recordId.hashCode();
    }

    @Override
    public String toString() {
        return "EarningsRecord{id=" + recordId + ", date=" + shiftDate + ", net=" + netReceived
            + ", status=" + status + "}";
    }
}
