package com.fairgig.ui;

/**
 * One tab of the app (sidebar entry or header page).
 *
 * @param title    tab name
 * @param subtitle one-line description shown under the page title
 * @param icon     icon name from {@link Icons}
 * @param home     true for the role's home / dashboard page
 */
public record NavItem(String title, String subtitle, String icon, boolean home) {

    /** Shared pages opened from the header (bell and avatar). */
    public static final NavItem NOTIFICATIONS = new NavItem(
        "Notifications", "Updates about reviews, complaints and evidence.", "bell", false);
    public static final NavItem PROFILE = new NavItem(
        "My Profile", "View and update your account details.", "person", false);
}
