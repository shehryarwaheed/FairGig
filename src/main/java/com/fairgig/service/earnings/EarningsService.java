package com.fairgig.service.earnings;

import com.fairgig.model.EarningInput;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import com.fairgig.strategy.validation.ValidationResult;

/**
 * Logging earnings by hand (FR-09, FR-10, FR-11). Only workers log earnings, and only their own.
 * Screens must use the proxied instance:
 * {@code AccessProxy.wrap(EarningsService.class, new DefaultEarningsService(...))}.
 * <p>
 * Viewing history and verification status is part of the verification feature (FR-15, FR-16).
 */
public interface EarningsService {

    /**
     * Checks the input WITHOUT saving, so a form can show every problem next to its field
     * while the worker types. Includes the platform checks.
     */
    @Access(roles = Role.WORKER)
    ValidationResult check(@Self int workerId, EarningInput input);

    /**
     * Validates and saves a new earning with status PENDING (FR-11) and entry method MANUAL.
     *
     * @throws com.fairgig.util.ValidationException listing every problem, if the input is not acceptable
     */
    @Access(roles = Role.WORKER)
    EarningsRecord logEarning(@Self int workerId, EarningInput input);
}
