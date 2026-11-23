package com.fairgig.controller;

import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The result of a controller call: either a value, or a list of messages ready to show to the user.
 * Controllers never let an exception escape to the screen, so a failed operation can never
 * crash the app (NFR-11) and the user always sees a clear message.
 *
 * <pre>
 *   Outcome&lt;User&gt; result = authController.login(email, password);
 *   if (result.isOk()) { ... result.value() ... } else { showError(result.message()); }
 * </pre>
 */
public final class Outcome<T> {

    private static final Logger LOG = Logger.getLogger("FairGig");

    private final T value;
    private final List<String> problems;

    private Outcome(T value, List<String> problems) {
        this.value = value;
        this.problems = problems;
    }

    public static <T> Outcome<T> ok(T value) {
        return new Outcome<>(value, List.of());
    }

    public static <T> Outcome<T> fail(String message) {
        return new Outcome<>(null, List.of(message));
    }

    public static <T> Outcome<T> fail(List<String> messages) {
        return new Outcome<>(null, List.copyOf(messages));
    }

    public boolean isOk() {
        return problems.isEmpty();
    }

    /** The result (null for operations that return nothing, or when failed). */
    public T value() {
        return value;
    }

    /** Every message, one per problem (empty when ok). */
    public List<String> problems() {
        return problems;
    }

    /** All messages joined line by line, for a single label or alert. */
    public String message() {
        return String.join("\n", problems);
    }

    /** Runs an action and turns any failure into a friendly Outcome. */
    public static <T> Outcome<T> attempt(Supplier<T> action) {
        try {
            return ok(action.get());
        } catch (ValidationException e) {
            return fail(e.getProblems());
        } catch (AccessDeniedException | NotFoundException e) {
            return fail(e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Database problem", e);
            return fail("Something went wrong with the database. Please try again. "
                + "If it keeps happening, check that PostgreSQL is running.");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected problem", e);
            return fail("Something unexpected went wrong. Please try again.");
        }
    }

    /** Same for actions that return nothing. */
    public static Outcome<Void> run(Runnable action) {
        return attempt(() -> {
            action.run();
            return null;
        });
    }
}
