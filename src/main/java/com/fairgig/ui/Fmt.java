package com.fairgig.ui;

import com.fairgig.model.VerificationStatus;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.scene.control.Label;

/** Display helpers: money in the chosen currency, numbers, times and status chips. */
public final class Fmt {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.ENGLISH);

    private Fmt() {}

    /** 5200 -> "5,200.00" (no currency symbol). */
    public static String number(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }

    /** Hours: 6.50 -> "6.5". */
    public static String hours(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    /** A label that shows the amount with the currency the user picked and updates when it changes. */
    public static Label money(BigDecimal amount) {
        Label label = new Label();
        label.textProperty().bind(CurrencyState.money(number(amount)));
        return label;
    }

    public static String when(LocalDateTime time) {
        return time == null ? "" : WHEN.format(time);
    }

    /** A coloured pill for a verification status. */
    public static Label statusChip(VerificationStatus status) {
        Label chip = new Label(status.label());
        chip.getStyleClass().add("status-chip");
        chip.getStyleClass().add(switch (status) {
            case PENDING -> "status-pending";
            case VERIFIED -> "status-verified";
            case DISPUTED_FLAGGED -> "status-flagged";
            case UNVERIFIABLE -> "status-unverifiable";
        });
        return chip;
    }
}
