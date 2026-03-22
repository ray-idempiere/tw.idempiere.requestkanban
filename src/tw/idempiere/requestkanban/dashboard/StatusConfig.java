/*
 * Copyright (C) 2026 Ray Lee / TopGiga
 * SPDX-License-Identifier: GPL-2.0-only
 */
package tw.idempiere.requestkanban.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure-Java status filter configuration.
 * No iDempiere imports — fully unit-testable.
 * Constructed by RequestKanbanDashboard.loadStatusConfig() from AD_SysConfig values.
 */
public class StatusConfig {

    private final Set<String> hiddenStatuses;
    private final String activeStatusesInClause;

    /**
     * @param hiddenCsv  comma-separated R_Status.value entries to hide (may be null or blank)
     * @param activeCsv  comma-separated R_Status.value entries that count as "active"
     *                   (may be null or blank; defaults to "Processing,Open")
     */
    public StatusConfig(String hiddenCsv, String activeCsv) {
        this.hiddenStatuses = parseCsv(hiddenCsv);
        Set<String> activeSet = parseCsv(activeCsv);
        if (activeSet.isEmpty()) activeSet = Set.of("Processing", "Open");
        this.activeStatusesInClause = activeSet.stream()
                .map(v -> "'" + v.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Returns true if this status value should be hidden from the Kanban/List view.
     * Exact-case match. null is never hidden.
     */
    public boolean isHidden(String statusValue) {
        if (statusValue == null) return false;
        return hiddenStatuses.contains(statusValue);
    }

    /**
     * Returns a SQL-safe IN clause fragment, e.g. "'Processing','Open'".
     * Values come from MSysConfig (admin-controlled); single quotes are escaped.
     */
    public String getActiveStatusesInClause() {
        return activeStatusesInClause;
    }
}
