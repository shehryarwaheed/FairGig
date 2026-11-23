package com.fairgig.ui;

/**
 * A dashboard tile on a role's home page.
 *
 * @param icon  icon name
 * @param tint  mint | blue | coral | amber
 * @param label micro-label
 * @param kind  how the (placeholder) value is displayed
 * @param note  small caption under the meter
 */
public record TileSpec(String icon, String tint, String label, Kind kind, String note) {

    /** Value style: money follows the selected currency. */
    public enum Kind { MONEY, COUNT, PERCENT }
}
