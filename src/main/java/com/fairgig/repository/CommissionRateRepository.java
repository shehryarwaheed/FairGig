package com.fairgig.repository;

import com.fairgig.model.CommissionRate;
import java.util.List;

/** Data access for platform commission rates (table "commission_rates"). */
public interface CommissionRateRepository {

    /** Inserts the rate and fills in its id. */
    CommissionRate save(CommissionRate rate);

    /** Every rate of one platform, OLDEST effective date first (so each one can be compared with the one before). */
    List<CommissionRate> findByPlatform(int platformId);
}
