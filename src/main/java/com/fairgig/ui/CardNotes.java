package com.fairgig.ui;

import com.fairgig.controller.Outcome;
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

    /**
     * The usual end of a card action: on success remember the note and reload the list, so the
     * rebuilt card shows it; on failure show the problems on the card itself.
     */
    public void finish(int id, Banner cardBanner, Outcome<?> result, String successNote, Runnable reload) {
        if (result.isOk()) {
            remember(id, successNote);
            reload.run();
        } else {
            cardBanner.showError(result.problems());
        }
    }
}
