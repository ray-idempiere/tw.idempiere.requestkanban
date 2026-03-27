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
		// ── Determine column granularity ─────────────────────────────────────
		long spanDays = ChronoUnit.DAYS.between(from, to) + 1;
		String colUnit;
		if ("week".equals(range) || spanDays <= 14) {
			colUnit = "day";
		} else if ("quarter".equals(range) || spanDays > 60) {
			colUnit = "month";
		} else {
			colUnit = "week";
		}

		// ── Build list of column start dates ─────────────────────────────────
		List<LocalDate> cols = new ArrayList<>();
		if ("day".equals(colUnit)) {
			for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1))
				cols.add(d);
		} else if ("week".equals(colUnit)) {
			cols.add(from);
			LocalDate mon = from.with(DayOfWeek.MONDAY);
			if (!mon.isAfter(from)) mon = mon.plusWeeks(1);
			for (LocalDate d = mon; !d.isAfter(to); d = d.plusWeeks(1))
				cols.add(d);
		} else {
			cols.add(from);
			for (LocalDate d = from.withDayOfMonth(1).plusMonths(1);
				 !d.isAfter(to); d = d.plusMonths(1))
				cols.add(d);
		}

		int N         = cols.size();
		int totalCols = 1 + N;
		LocalDate today    = LocalDate.now();
		long      totalDays = ChronoUnit.DAYS.between(from, to) + 1;

		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"min-width:600px;\">");
		sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:11px;min-width:600px;\">");

		// -- Header row --
		sb.append("<thead><tr style=\"background:#f4f5f7;border-bottom:2px solid #ddd;\">");
		sb.append("<th style=\"width:160px;padding:6px 10px;text-align:left;color:#555;" +
		          "font-weight:600;border-right:1px solid #ddd;\">")
		  .append(Msg.getMsg(ctx, "DocumentNo"))
		  .append("</th>");
		for (LocalDate col : cols) {
			boolean isToday = "day".equals(colUnit)
				? col.equals(today)
				: (!col.isAfter(today) &&
				   (cols.indexOf(col) == cols.size() - 1 ||
				    cols.get(cols.indexOf(col) + 1).isAfter(today)));
			String thStyle = "padding:4px;text-align:center;color:#888;font-weight:500;" +
				"border-right:1px solid #eee;" +
				(isToday ? "background:#fff3cd;" : "");
			sb.append("<th style=\"").append(thStyle).append("\">");
			sb.append(col.getMonthValue()).append("/").append(col.getDayOfMonth());
			if (isToday) sb.append(" ◀今");
			sb.append("</th>");
		}
		sb.append("</tr></thead>");

		// -- Body rows --
		sb.append("<tbody>");
		boolean teamMode = !"Private".equals(scope);
		String lastResponsible = null;
		int lastProjectId = Integer.MIN_VALUE;

		do {
			int    requestId  = rs.getInt("R_Request_ID");
			String docNo      = rs.getString("DocumentNo");
			String summary    = rs.getString("Summary");
			String priority   = rs.getString("Priority");
			int    statusId   = rs.getInt("R_Status_ID");
			java.sql.Timestamp startTs   = rs.getTimestamp("StartTime");
			java.sql.Timestamp endTs     = rs.getTimestamp("EndTime");
			java.sql.Date      closeDate = rs.getDate("CloseDate");
			if (endTs == null && closeDate != null)
				endTs = new java.sql.Timestamp(closeDate.getTime());
			String responsible = rs.getString("Responsible");
			String customer    = rs.getString("Customer");
			int    projectId   = rs.getInt("C_Project_ID");
			boolean hasProject = !rs.wasNull();
			String projectName = rs.getString("ProjectName");

			// Project group header
			int groupKey = hasProject ? projectId : -1;
			if (groupKey != lastProjectId) {
				lastProjectId   = groupKey;
				lastResponsible = null;
				String groupLabel = hasProject
					? "📁 " + esc(projectName)
					: Msg.getMsg(ctx, "RK_Unassigned");
				String groupStyle = hasProject
					? "background:#f0f4ff;color:#1a3a6e;"
					: "background:#f5f5f5;color:#888;font-style:italic;";
				sb.append("<tr style=\"").append(groupStyle).append("\">")
				  .append("<td colspan=\"").append(totalCols).append("\"")
				  .append(" style=\"font-weight:600;padding:5px 10px;font-size:11px;")
				  .append("border-bottom:1px solid #c8d8ff;\">")
				  .append(groupLabel)
				  .append("</td></tr>");
			}

			// Person sub-header (team mode)
			if (teamMode) {
				String respKey = responsible != null ? responsible : "";
				if (!respKey.equals(lastResponsible)) {
					lastResponsible = respKey;
					String dispName = responsible != null && !responsible.isEmpty()
						? responsible : Msg.getMsg(ctx, "RK_Unassigned");
					sb.append("<tr style=\"background:#f0f4ff;\">")
					  .append("<td colspan=\"").append(totalCols).append("\"")
					  .append(" style=\"padding:5px 10px;font-size:11px;font-weight:700;color:#0052cc;\">")
					  .append("👤 ").append(esc(dispName))
					  .append("</td></tr>");
				}
			}

			// No-date row
			if (startTs == null && endTs == null) {
				String summaryTrunc = summary != null && summary.length() > 30
					? summary.substring(0, 30) + "…" : (summary != null ? summary : "");
				sb.append("<tr style=\"opacity:0.5;border-bottom:1px solid #f0f0f0;\">")
				  .append("<td draggable=\"true\"")
				  .append(" ondragstart=\"window._zkGanttDragging=").append(requestId)
				  .append(";event.dataTransfer.setData('text/plain','").append(requestId).append("');\"")
				  .append(" onclick=\"window._zkGanttClick(").append(requestId).append(")\"")
				  .append(" style=\"padding:6px 10px;border-right:1px solid #ddd;font-size:11px;color:#333;cursor:grab;\">")
				  .append("#").append(esc(docNo)).append(" — ").append(esc(summaryTrunc))
				  .append("</td>")
				  .append("<td colspan=\"").append(N).append("\"")
				  .append(" style=\"padding:4px 10px;color:#aaa;font-size:10px;font-style:italic;\">")
				  .append("— ").append(Msg.getMsg(ctx, "RK_NoDateSet"))
				  .append("</td></tr>");
				continue;
			}

			// Bar math
			LocalDate startDate = startTs != null
				? startTs.toLocalDateTime().toLocalDate() : from;
			LocalDate endDate = endTs != null
				? endTs.toLocalDateTime().toLocalDate() : to;

			double leftPct  = Math.max(0,
				ChronoUnit.DAYS.between(from, startDate)) * 100.0 / totalDays;
			double widthPct = Math.max(2,
				Math.min(100.0 - leftPct,
					(ChronoUnit.DAYS.between(startDate, endDate) + 1) * 100.0 / totalDays));

			String[] colors     = barColors(statusId);
			String   bg         = colors[0];
			String   textColor  = colors[1];
			String   borderColor = priorityBorder(priority);

			// Request name column
			sb.append("<tr style=\"border-bottom:1px solid #f0f0f0;\">")
			  .append("<td draggable=\"true\"")
			  .append(" ondragstart=\"window._zkGanttDragging=").append(requestId)
			  .append(";event.dataTransfer.setData('text/plain','").append(requestId).append("');\"")
			  .append(" onclick=\"window._zkGanttClick(").append(requestId).append(")\"")
			  .append(" style=\"padding:6px 10px;border-right:1px solid #ddd;cursor:grab;\">")
			  .append("<div style=\"font-weight:600;color:#333;font-size:11px;\">#")
			  .append(esc(docNo)).append("</div>")
			  .append("<div style=\"color:#888;font-size:10px;white-space:nowrap;overflow:hidden;" +
			          "text-overflow:ellipsis;max-width:140px;\">")
			  .append(esc(summary != null ? summary : ""))
			  .append("</div></td>");

			// Bar label
			java.time.format.DateTimeFormatter barFmt =
				java.time.format.DateTimeFormatter.ofPattern("M/d");
			StringBuilder barLabel = new StringBuilder();
			if (customer != null && !customer.isEmpty())
				barLabel.append("(").append(esc(customer)).append(") ");
			barLabel.append(startTs != null
				? startTs.toLocalDateTime().toLocalDate().format(barFmt) : "");
			barLabel.append("~");
			barLabel.append(endTs != null
				? endTs.toLocalDateTime().toLocalDate().format(barFmt) : "");

			// Bar column
			sb.append("<td colspan=\"").append(N).append("\"")
			  .append(" style=\"padding:3px 2px;position:relative;height:32px;\">")
			  .append("<div draggable=\"true\"")
			  .append(" ondragstart=\"window._zkGanttDragging=").append(requestId)
			  .append(";event.dataTransfer.setData('text/plain','").append(requestId).append("');\"")
			  .append(" onclick=\"window._zkGanttClick(").append(requestId).append(")\"")
			  .append(" title=\"#").append(esc(docNo)).append(": ")
			  .append(esc(summary != null ? summary : "")).append("\"")
			  .append(" style=\"position:absolute;")
			  .append("left:").append(String.format("%.2f", leftPct)).append("%;")
			  .append("width:").append(String.format("%.2f", widthPct)).append("%;")
			  .append("background:").append(bg).append(";")
			  .append("border-left:4px solid ").append(borderColor).append(";")
			  .append("height:22px;border-radius:0 3px 3px 0;")
			  .append("display:flex;align-items:center;padding:0 6px;")
			  .append("cursor:pointer;font-size:10px;color:").append(textColor).append(";")
			  .append("white-space:nowrap;overflow:hidden;\">")
			  .append(barLabel)
			  .append("</div></td></tr>");

		} while (rs.next());

		sb.append("</tbody></table>");

		// -- Legend --
		sb.append("<div style=\"padding:8px 12px;border-top:1px solid #eee;" +
		          "display:flex;gap:16px;flex-wrap:wrap;font-size:10px;color:#555;\">");
		sb.append("<strong style=\"color:#444;\">狀態底色：</strong>");
		String[][] statusLegend = {
			{"#deebff", "Open"}, {"#fffae6", "Processing"}, {"#e3fcef", "Verify"},
			{"#ffebe6", "Problem"}, {"#dfe1e6", "Closed"}
		};
		for (String[] sl : statusLegend) {
			sb.append("<span><span style=\"display:inline-block;width:12px;height:12px;" +
			          "background:").append(sl[0]).append(";vertical-align:middle;" +
			          "border-radius:2px;\"></span> ")
			  .append(esc(statusName(sl[1])))
			  .append("</span>");
		}
		sb.append("<span style=\"margin-left:12px;\">")
		  .append("<strong style=\"color:#444;\">左邊框 = 優先權</strong></span>");
		String[][] priorityLegend = {
			{"#bf2600", "Urgent"}, {"#ff8b00", "High"}, {"#ffe380", "Medium"}, {"#36b37e", "Low"}
		};
		for (String[] pl : priorityLegend) {
			sb.append("<span><span style=\"display:inline-block;width:4px;height:12px;" +
			          "background:").append(pl[0]).append(";vertical-align:middle;\"></span> ")
			  .append(pl[1]).append("</span>");
		}
		sb.append("</div></div>");

		return sb.toString();
	}
}
