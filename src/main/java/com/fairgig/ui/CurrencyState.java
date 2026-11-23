package com.fairgig.ui;

import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/** The currency the user picked in the header. Screens bind their money text to it. */
public final class CurrencyState {

    /** One selectable currency. */
    public record Option(String code, String symbol, String name) {}

    public static final List<Option> OPTIONS = List.of(
        new Option("PKR", "Rs", "Pakistani Rupee"),
        new Option("USD", "$", "US Dollar"),
        new Option("EUR", "€", "Euro"),
        new Option("GBP", "£", "British Pound"),
        new Option("AED", "AED", "UAE Dirham"),
        new Option("SAR", "SAR", "Saudi Riyal"),
        new Option("INR", "₹", "Indian Rupee")
    );

    private static final ObjectProperty<Option> CURRENT = new SimpleObjectProperty<>(OPTIONS.get(0));

    private CurrencyState() {}

    public static ObjectProperty<Option> currentProperty() {
        return CURRENT;
    }

    /** Just the symbol, e.g. "Rs". */
    public static StringBinding symbolBinding() {
        return Bindings.createStringBinding(() -> CURRENT.get().symbol(), CURRENT);
    }

    /** Symbol + amount text, e.g. "Rs 48,250". */
    public static StringBinding money(String amount) {
        return Bindings.createStringBinding(() -> CURRENT.get().symbol() + " " + amount, CURRENT);
    }
}
