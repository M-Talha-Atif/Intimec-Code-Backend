package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.MttrTrendEntry;

import java.util.List;

/**
 * Service contract for computing the 7-day MTTR trend.
 *
 * <p>Returns exactly 7 entries -- one per calendar day -- covering the
 * last 7 days in the caller's timezone. Days with no resolved entries
 * return mttr=0.0.
 *
 * <p>MTTR is only computed from entries that have a {@code resolvedAt}
 * field. Unresolved entries are excluded entirely.
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsMttrTrendService {

    /**
     * Computes the 7-day MTTR trend.
     *
     * @param timezone IANA timezone string for day boundary alignment;
     *                 null or blank defaults to UTC
     * @return list of 7 entries, oldest day first, newest day last
     */
    List<MttrTrendEntry> computeMttrTrend(String timezone);
}