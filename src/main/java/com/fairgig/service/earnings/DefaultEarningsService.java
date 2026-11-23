package com.fairgig.service.earnings;

import com.fairgig.model.EarningInput;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.EntryMethod;
import com.fairgig.model.Platform;
import com.fairgig.model.VerificationStatus;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.WorkerPlatformRepository;
import com.fairgig.strategy.validation.EarningValidation;
import com.fairgig.strategy.validation.FieldProblem;
import com.fairgig.strategy.validation.ValidationResult;
import com.fairgig.strategy.validation.ValidationStrategy;
import com.fairgig.util.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Real implementation of {@link EarningsService}. The validation rules are a STRATEGY that is
 * handed in, so they can be swapped (for example in tests) without changing this class.
 */
public class DefaultEarningsService implements EarningsService {

    private final EarningsRepository earnings;
    private final PlatformRepository platforms;
    private final WorkerPlatformRepository workerPlatforms;
    private final ValidationStrategy<EarningInput> validation;

    /** Uses the standard rules. */
    public DefaultEarningsService(EarningsRepository earnings, PlatformRepository platforms,
                                  WorkerPlatformRepository workerPlatforms) {
        this(earnings, platforms, workerPlatforms, EarningValidation.standard());
    }

    public DefaultEarningsService(EarningsRepository earnings, PlatformRepository platforms,
                                  WorkerPlatformRepository workerPlatforms,
                                  ValidationStrategy<EarningInput> validation) {
        this.earnings = earnings;
        this.platforms = platforms;
        this.workerPlatforms = workerPlatforms;
        this.validation = validation;
    }

    @Override
    public ValidationResult check(int workerId, EarningInput input) {
        ValidationResult result = validation.validate(input);
        List<FieldProblem> platformProblems = new ArrayList<>();
        // Only look at the platform when one was chosen (a missing one is already reported)
        if (input.platformId() != null) {
            Optional<Platform> platform = platforms.findById(input.platformId());
            if (platform.isEmpty() || !platform.get().isActive()) {
                platformProblems.add(new FieldProblem("platform",
                    "This platform is not available. Please choose one from the list."));
            } else if (!workerPlatforms.exists(workerId, input.platformId())) {
                platformProblems.add(new FieldProblem("platform",
                    "You are not registered on " + platform.get().getName()
                        + ". Please add it to your platforms first."));
            }
        }
        return result.and(ValidationResult.of(platformProblems));
    }

    @Override
    public EarningsRecord logEarning(int workerId, EarningInput input) {
        ValidationResult result = check(workerId, input);
        if (!result.isValid()) {
            throw new ValidationException(result.messages());
        }
        EarningsRecord record = new EarningsRecord();
        record.setWorkerId(workerId);
        record.setPlatformId(input.platformId());
        record.setShiftDate(input.shiftDate());
        record.setHoursWorked(input.hoursWorked());
        record.setGrossEarnings(input.grossEarnings());
        record.setDeductions(input.deductions());
        record.setNetReceived(input.netReceived());
        record.setStatus(VerificationStatus.PENDING);      // FR-11
        record.setEntryMethod(EntryMethod.MANUAL);
        return earnings.save(record);
    }

    @Override
    public List<EarningsRecord> recentEarnings(int workerId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return earnings.findByWorker(workerId, com.fairgig.repository.EarningsFilter.none())
            .stream().limit(safeLimit).toList();
    }
}
