package com.fairgig.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Lets a list of cards show "Saved!" on the card that was just changed, even though the whole
 * list is rebuilt after the change: remember the message, and the rebuilt card shows it once.
 */
public final class CardNotes {

    private final Map<Integer, String> notes = new HashMap<>();

    /** Call before reloading the list. */
    public void remember(int id, String message) {
        notes.put(id, message);
    }

    /** A banner for the card with this id. It already shows the remembered message (once), if there is one. */
    public Banner bannerFor(int id) {
        Banner banner = new Banner();
        String message = notes.remove(id);
        if (message != null) {
            banner.showSuccess(message);
        }
        return banner;
    }
}
