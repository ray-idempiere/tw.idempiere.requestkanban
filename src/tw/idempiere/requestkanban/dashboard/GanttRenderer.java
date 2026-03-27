/*
 * Copyright (C) 2026 Ray Lee / TopGiga
 * SPDX-License-Identifier: GPL-2.0-only
 */
package tw.idempiere.requestkanban.dashboard;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MStatus;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Builds the Gantt table HTML for the Request Kanban dashboard.
 * All rendering logic lives here; RequestKanbanDashboard owns ZK components and events.
 */
class GanttRenderer {

	private final Properties ctx;
	private final LocalDate  from;
	private final LocalDate  to;
	private final String     range;   // "week" | "month" | "quarter" | "custom"
	private final String     scope;   // currentScope from dashboard
	private final MStatus[]  statuses;

	GanttRenderer(Properties ctx, LocalDate from, LocalDate to,
	              String range, String scope, MStatus[] statuses) {
		this.ctx      = ctx;
		this.from     = from;
		this.to       = to;
		this.range    = range;
		this.scope    = scope;
		this.statuses = statuses;
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	/** Background + text color for a Gantt bar based on status. */
	private String[] barColors(int statusId) {
		if (statuses != null) {
			for (MStatus s : statuses) {
				if (s.getR_Status_ID() == statusId) {
					switch (s.getValue()) {
						case "Open":       return new String[]{"#3b82f6", "#ffffff"};
						case "Processing": return new String[]{"#f59e0b", "#ffffff"};
						case "Verify":     return new String[]{"#10b981", "#ffffff"};
						case "Problem":    return new String[]{"#ef4444", "#ffffff"};
						case "Closed":     return new String[]{"#9ca3af", "#ffffff"};
						default:           return new String[]{"#9ca3af", "#ffffff"};
					}
				}
			}
		}
		return new String[]{"#9ca3af", "#ffffff"};
	}

	/** Left-border color for priority. */
	private String priorityBorder(String priority) {
		if ("1".equals(priority)) return "#dc2626"; // Urgent
		if ("2".equals(priority)) return "#f97316"; // High
		if ("3".equals(priority)) return "#fbbf24"; // Medium
		return "#34d399";                           // Low / other
	}

	/** Display name for a status value key. */
	private String statusName(String value) {
		if (statuses != null) {
			for (MStatus s : statuses) {
				if (s.getValue().equals(value)) return s.getName();
			}
		}
		return value;
	}

	/** Escapes HTML special characters. */
	private static String esc(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;")
		        .replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * Builds the full Gantt HTML from an already-positioned ResultSet.
	 * Caller is responsible for closing the ResultSet.
	 */
	String build(ResultSet rs) throws SQLException {
		// placeholder — full implementation in Task 2
		return "<div style=\"padding:20px;color:#888;\">GanttRenderer connected</div>";
	}
}
