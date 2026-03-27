/*
 * Copyright (C) 2026 Ray Lee / TopGiga
 * SPDX-License-Identifier: GPL-2.0-only
 */
package tw.idempiere.requestkanban.dashboard;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.logging.Level;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.dashboard.DashboardPanel;
import org.adempiere.webui.desktop.IDesktop;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.WAttachment;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ServerPushTemplate;
import org.compiere.model.MAttachment;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MQuery;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestUpdate;
import org.compiere.model.MStatus;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublespinner;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

/**
 * Dashboard item: Request Kanban 
 * @author Ray Lee
 * 
 */
public class RequestKanbanDashboard extends DashboardPanel implements EventListener<Event> ,ValueChangeListener, org.osgi.service.event.EventHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3787249181565314148L;

	@SuppressWarnings("unused")
	private static final CLogger logger = CLogger.getCLogger(RequestKanbanDashboard.class);
	private String currentScope = "Private";
	private Window dialog;
	private Window winNewRequest;
	private static final String VIEW_KANBAN = "Kanban";
	private static final String VIEW_LIST = "List";
	private static final String VIEW_GANTT = "gantt";
	private String currentView = VIEW_KANBAN;
	private String searchFilter = "";
	
	private Textbox txtSearch;
	private Button btnToggleView;
	private Tabbox listTabbox;
	private Hlayout kanbanLayout;
	private Vlayout root;
	private MStatus[] statuses;
	private MRequest currentRequest;
	private WSearchEditor fUser;
	private WSearchEditor fSalesRep;
	private WTableDirEditor fDoc;
	private WSearchEditor fRole;
	private WTableDirEditor fDepart;
	private Textbox summary;
	private WTableDirEditor fPriority;
	private WSearchEditor fProductSpent;
	private Doublespinner spinnerQuantity;
	private Textbox result;
	private Datebox fStartTime;
	private Datebox fEndTime;
	// Gantt view fields
	private Div ganttLayout;
	private boolean ganttControlsInitialized = false;
	private String ganttRange = "month";
	private boolean showFinalClose = false;
	private Checkbox chkFinalClose;
	private LocalDate ganttFrom;
	private LocalDate ganttTo;
	private Datebox ganttFromBox;
	private Datebox ganttToBox;
	private Html ganttHtml;

	// Project panel fields
	private Html projectPanelHtml;
	private List<int[]> cachedProjects = new ArrayList<>();
	private List<String> cachedProjectNames = new ArrayList<>();

	private ServiceRegistration<EventHandler> m_reg;
	private StatusConfig statusConfig = new StatusConfig("", "Processing,Open");

	private void loadStatusConfig() {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		String hidden = MSysConfig.getValue("RK_HiddenStatuses", "", clientId, 0);
		String active = MSysConfig.getValue("RK_ActiveStatuses", "Processing,Open", clientId, 0);
		statusConfig = new StatusConfig(hidden, active);
	}

	protected void initForm() {
		loadStatusConfig();
		this.setStyle("padding: 0; margin: 0; border: none;");
		
		root = new Vlayout();
		root.setHflex("1");
		root.setVflex("1");
		root.setSpacing("0");
		root.setStyle("padding: 0; margin: 0;");
		this.appendChild(root);

		// Top Bar
		Hlayout topBar = new Hlayout();
		topBar.setValign("middle");
		topBar.setSpacing("10px");
		topBar.setHflex("1");
		topBar.setStyle("padding: 10px; background-color: #f5f5f5; border-bottom: 1px solid #ddd;");
		root.appendChild(topBar);

		// View Toggle (leftmost)
		btnToggleView = new Button("📅 " + Msg.getMsg(Env.getCtx(), "RK_GanttView"));
		btnToggleView.addEventListener(Events.ON_CLICK, e -> toggleView());
		topBar.appendChild(btnToggleView);

		Button btnNew = new Button(Msg.getMsg(Env.getCtx(),"RK_NewRequest"));
		btnNew.setImage("/images/kanban/add-file-bw-24.png");
		btnNew.setHoverImage("/images/kanban/add-file-color-24.png");
		btnNew.setSclass("btn-primary rk-btn-sm-icon");
		btnNew.addEventListener(Events.ON_CLICK, e -> createNewRequestForm(e));
		topBar.appendChild(btnNew);

		Separator sep = new Separator("vertical");
		sep.setBar(true);
		topBar.appendChild(sep);

		Hlayout pillGroup = new Hlayout();
		pillGroup.setSpacing("4px");
		pillGroup.setValign("middle");
		String[] scopeLabels = {"RK_Private", "RK_Subordinates", "RK_Team", "RK_All"};
		String[] scopeValues = {"Private", "Subordinates", "Team", "All"};
		for (int i = 0; i < scopeLabels.length; i++) {
			if (i == 3 && !isSupervisor()) continue;
			final String scopeVal = scopeValues[i];
			Button pill = new Button(Msg.getMsg(Env.getCtx(), scopeLabels[i]));
			pill.setSclass(i == 0 ? "rk-pill rk-pill-active" : "rk-pill");
			pill.setAttribute("Scope", scopeVal);
			pill.addEventListener(Events.ON_CLICK, e -> {
				currentScope = scopeVal;
				for (Component sibling : pillGroup.getChildren()) {
					if (sibling instanceof Button) {
						String sibScope = (String) sibling.getAttribute("Scope");
						((Button) sibling).setSclass(
							scopeVal.equals(sibScope) ? "rk-pill rk-pill-active" : "rk-pill");
					}
				}
				refreshData();
			});
			pillGroup.appendChild(pill);
		}
		topBar.appendChild(pillGroup);

		Separator sep2 = new Separator("vertical");
		sep2.setBar(true);
		topBar.appendChild(sep2);

		// Search
		txtSearch = new Textbox();
		txtSearch.setPlaceholder(Msg.getMsg(Env.getCtx(), "RK_SearchPlaceholder"));
		txtSearch.setWidth("200px");
		txtSearch.addEventListener(Events.ON_OK, e -> {
			searchFilter = txtSearch.getValue().trim();
			refreshData();
		});
		topBar.appendChild(txtSearch);

		// Version info aligned to the right
		Space spring = new Space();
		spring.setHflex("1");
		topBar.appendChild(spring);

		// Display Final Close checkbox (right side, before version; only shown in Gantt view)
		chkFinalClose = new Checkbox(Msg.getMsg(Env.getCtx(), "RK_DisplayFinalClose"));
		chkFinalClose.setVisible(false);
		chkFinalClose.addEventListener(Events.ON_CHECK, e -> {
			showFinalClose = chkFinalClose.isChecked();
			refreshGantt();
		});
		topBar.appendChild(chkFinalClose);
		
		org.osgi.framework.Bundle hostBundle = FrameworkUtil.getBundle(getClass());
		String pluginVersion = "?.?.?";
		if (hostBundle != null && hostBundle.getBundleContext() != null) {
			for (org.osgi.framework.Bundle b : hostBundle.getBundleContext().getBundles()) {
				if ("tw.idempiere.requestkanban".equals(b.getSymbolicName())) {
					pluginVersion = b.getVersion().toString();
					break;
				}
			}
		}
		Label lblVersion = new Label("v" + pluginVersion);
		lblVersion.setStyle("color: #999; font-size: 11px; font-weight: bold; margin-right: 10px;");
		topBar.appendChild(lblVersion);

		// Kanban View Container
		kanbanLayout = new Hlayout();
		kanbanLayout.setHflex("1");
		kanbanLayout.setVflex("1");
		kanbanLayout.setSclass("rk-kanban-hlayout");
		kanbanLayout.setStyle("overflow-x: auto; padding: 10px;");
		root.appendChild(kanbanLayout);

		// List View Container (Hidden by default)
		listTabbox = new Tabbox();
		listTabbox.setHflex("1");
		listTabbox.setVflex("1");
		listTabbox.setVisible(false);
		root.appendChild(listTabbox);

		// Gantt View Container (hidden by default)
		ganttLayout = new Div();
		ganttLayout.setHflex("1");
		ganttLayout.setVflex("1");
		ganttLayout.setStyle("overflow: auto;");
		ganttLayout.setVisible(false);
		root.appendChild(ganttLayout);

		if(statuses == null)
			statuses = getRequestStatus(); 
			
		initKanbanColumns();
		initListTabs();
			 
		refreshData();
		
		// Register OSGi Event Handler for real-time updates
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(EventConstants.EVENT_TOPIC, new String[]{
				"org/compiere/model/R_Request/*", 
				"org/compiere/model/R_RequestUpdate/*",
				"org/compiere/model/AD_Attachment/*"
		});
		m_reg = FrameworkUtil.getBundle(RequestKanbanDashboard.class).getBundleContext().registerService(EventHandler.class, this, props);
	}

	@Override
	public void handleEvent(org.osgi.service.event.Event event) {
		Desktop desktop = this.getDesktop();
		if (desktop != null) {
			if (!desktop.isServerPushEnabled()) {
				desktop.enableServerPush(true);
			}
			Executions.schedule(desktop, e -> refreshData(), new Event("onServerPushRefresh"));
		}
	}

	public void onDetach() {
		if (m_reg != null) {
			m_reg.unregister();
			m_reg = null;
		}
		//super.onDetach();
	}

	private void initKanbanColumns() {
		kanbanLayout.getChildren().clear();
		int columnIndex = 0;
		for (MStatus mStatus : statuses) {
			String val = mStatus.getValue();
			if (statusConfig.isHidden(val)) continue;

			Vlayout col = new Vlayout();
			col.setHflex("1");
			col.setStyle("background-color: #ebedf0; border-radius: 5px; margin-right: 15px; padding: 5px; min-width: 280px; overflow-y: auto;");
			col.setDroppable("true");
			col.addEventListener(Events.ON_DROP, e -> onDrop((DropEvent)e));
			
			Hlayout header = new Hlayout();
			header.setHflex("1");
			header.setValign("middle");
			header.setStyle("padding: 5px; border-bottom: 2px solid #ddd;");
			header.setDroppable("true");
			header.addEventListener(Events.ON_DROP, e -> onDrop((DropEvent)e));
			
			Image statusIcon = getStatusIconImage(mStatus);
			if (statusIcon != null) header.appendChild(statusIcon);

			Label lblTitle = new Label(mStatus.getName());
			lblTitle.setStyle("font-weight: bold; font-size: 14px;");
			header.appendChild(lblTitle);
			col.appendChild(header);

			Listbox listbox = new Listbox();
			listbox.setId("listbox_" + mStatus.getValue());
			listbox.setHflex("1");
			listbox.setDroppable("true");
			listbox.setOddRowSclass("non-odd");
			listbox.setStyle("background: transparent; border: none;");
			listbox.setAttribute("R_Status_ID", mStatus.getR_Status_ID());
			listbox.addEventListener(Events.ON_DROP, e -> onDrop((DropEvent)e));
			
			col.appendChild(listbox);
			if (columnIndex == 0) {
				Button btnAddInCol = new Button(Msg.getMsg(Env.getCtx(), "RK_NewRequest"));
				btnAddInCol.setSclass("rk-add-card-btn");
				btnAddInCol.addEventListener(Events.ON_CLICK, e -> createNewRequestForm(e));
				col.appendChild(btnAddInCol);
			}
			kanbanLayout.appendChild(col);
			columnIndex++;
		}
	}

	private void initListTabs() {
		listTabbox.getChildren().clear();
		Tabs tabs = new Tabs();
		Tabpanels panels = new Tabpanels();
		listTabbox.appendChild(tabs);
		listTabbox.appendChild(panels);

		for (MStatus mStatus : statuses) {
			String val = mStatus.getValue();
			if (statusConfig.isHidden(val)) continue;

			Tab tab = new Tab(mStatus.getName());
			tabs.appendChild(tab);

			Tabpanel panel = new Tabpanel();
			panel.setVflex("1");
			Listbox listbox = new Listbox();
			listbox.setId("listbox_list_" + mStatus.getValue());
			listbox.setHflex("1");
			listbox.setVflex("1");
			listbox.setAttribute("R_Status_ID", mStatus.getR_Status_ID());
			
			Listhead head = new Listhead();
			head.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "DocumentNo"), null, "120px"));
			head.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "Summary")));
			head.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "RK_SalesRep"), null, "120px"));
			head.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "RK_StartTime"), null, "130px"));
			head.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "RK_EndTime"), null, "130px"));
			head.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "RK_Priority"), null, "80px"));
			listbox.appendChild(head);
			
			panel.appendChild(listbox);
			panels.appendChild(panel);
		}
	}

	private void toggleView() {
		if (currentView.equals(VIEW_KANBAN)) {
			// Kanban → Gantt
			currentView = VIEW_GANTT;
			btnToggleView.setLabel("≡ " + Msg.getMsg(Env.getCtx(), "RK_ListView"));
			kanbanLayout.setVisible(false);
			listTabbox.setVisible(false);
			ganttLayout.setVisible(true);
			chkFinalClose.setVisible(true);
			if (!ganttControlsInitialized) {
				initGanttControls();
			}
			resolveGanttRange();
			refreshGantt();
			refreshProjectPanel();
		} else if (currentView.equals(VIEW_GANTT)) {
			// Gantt → List
			currentView = VIEW_LIST;
			btnToggleView.setLabel("⊞ " + Msg.getMsg(Env.getCtx(), "RK_KanbanView"));
			ganttLayout.setVisible(false);
			chkFinalClose.setVisible(false);
			listTabbox.setVisible(true);
			refreshData();
		} else {
			// List → Kanban
			currentView = VIEW_KANBAN;
			btnToggleView.setLabel("📅 " + Msg.getMsg(Env.getCtx(), "RK_GanttView"));
			listTabbox.setVisible(false);
			kanbanLayout.setVisible(true);
			refreshData();
		}
	}

	private void resolveGanttRange() {
		LocalDate today = LocalDate.now();
		switch (ganttRange) {
			case "week":
				ganttFrom = today.with(DayOfWeek.MONDAY);
				ganttTo   = ganttFrom.plusDays(6);
				break;
			case "quarter":
				ganttFrom = today.withDayOfMonth(1)
				                 .withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1);
				ganttTo   = ganttFrom.plusMonths(3).minusDays(1);
				break;
			default: // "month" (also handles "custom" — does NOT overwrite dates)
				if (!"custom".equals(ganttRange)) {
					ganttFrom = today.withDayOfMonth(1);
					ganttTo   = today.withDayOfMonth(today.lengthOfMonth());
				}
				break;
		}
		if (ganttFromBox != null) ganttFromBox.setValue(java.sql.Date.valueOf(ganttFrom));
		if (ganttToBox   != null) ganttToBox.setValue(java.sql.Date.valueOf(ganttTo));
	}

	private void initGanttControls() {
		// ── Time range pills + Datebox row ──────────────────────────
		Hlayout rangeBar = new Hlayout();
		rangeBar.setValign("middle");
		rangeBar.setSpacing("6px");
		rangeBar.setStyle("padding: 8px 12px; background: #f8f9fa; border-bottom: 1px solid #ddd; flex-wrap: wrap;");

		String[][] rangeDefs = {
			{"week",    "RK_ThisWeek"},
			{"month",   "RK_ThisMonth"},
			{"quarter", "RK_ThisQuarter"}
		};
		for (String[] rd : rangeDefs) {
			final String rangeKey = rd[0];
			Button pill = new Button(Msg.getMsg(Env.getCtx(), rd[1]));
			pill.setSclass("month".equals(rangeKey) ? "rk-pill rk-pill-active" : "rk-pill");
			pill.setAttribute("ganttRange", rangeKey);
			pill.addEventListener(Events.ON_CLICK, e -> {
				ganttRange = rangeKey;
				// Update active style on all range pills
				for (Component sib : rangeBar.getChildren()) {
					if (sib instanceof Button && sib.getAttribute("ganttRange") != null) {
						((Button) sib).setSclass(
							rangeKey.equals(sib.getAttribute("ganttRange"))
							? "rk-pill rk-pill-active" : "rk-pill");
					}
				}
				resolveGanttRange();
				refreshGantt();
			});
			rangeBar.appendChild(pill);
		}

		// From Datebox
		ganttFromBox = new Datebox();
		ganttFromBox.setFormat("yyyy-MM-dd");
		ganttFromBox.setWidth("100px");
		ganttFromBox.addEventListener(Events.ON_CHANGE, e -> onGanttDateChange(rangeBar));

		// Separator "~"
		Label tilde = new Label("~");
		tilde.setStyle("color: #888; font-size: 11px; margin: 0 2px;");

		// To Datebox
		ganttToBox = new Datebox();
		ganttToBox.setFormat("yyyy-MM-dd");
		ganttToBox.setWidth("100px");
		ganttToBox.addEventListener(Events.ON_CHANGE, e -> onGanttDateChange(rangeBar));

		rangeBar.appendChild(tilde);
		rangeBar.appendChild(ganttFromBox);
		Label tilde2 = new Label("~");
		tilde2.setStyle("color: #888; font-size: 11px; margin: 0 2px;");
		rangeBar.appendChild(tilde2);
		rangeBar.appendChild(ganttToBox);

		ganttLayout.appendChild(rangeBar);

		// ── Content area: [project panel | gantt chart] ──────────────
		Hlayout contentArea = new Hlayout();
		contentArea.setHflex("1");
		contentArea.setVflex("1");
		contentArea.setSpacing("0");
		contentArea.setStyle("overflow: hidden;");

		// -- Left: project panel --
		Vlayout projectPanel = new Vlayout();
		projectPanel.setSpacing("4px");
		projectPanel.setWidth("200px");
		projectPanel.setStyle("padding: 8px; border-right: 1px solid #ddd; " +
		                      "background: #fafafa; overflow-y: auto; flex-shrink: 0;");

		Label lblProjects = new Label(Msg.getMsg(Env.getCtx(), "RK_Projects"));
		lblProjects.setStyle("font-size: 11px; font-weight: 700; color: #555; " +
		                     "text-transform: uppercase; letter-spacing: 0.5px;");
		projectPanel.appendChild(lblProjects);

		projectPanelHtml = new Html();
		projectPanelHtml.setHflex("1");
		projectPanel.appendChild(projectPanelHtml);

		Button btnNewProject = new Button(Msg.getMsg(Env.getCtx(), "RK_NewProject"));
		btnNewProject.setStyle("width: 100%; font-size: 11px; margin-top: 4px;");
		btnNewProject.addEventListener(Events.ON_CLICK, e -> openNewProjectDialog());
		projectPanel.appendChild(btnNewProject);

		contentArea.appendChild(projectPanel);

		// -- Right: gantt html --
		ganttHtml = new Html();
		ganttHtml.setHflex("1");
		ganttHtml.setVflex("1");
		contentArea.appendChild(ganttHtml);

		ganttLayout.appendChild(contentArea);

		// ── JS bridges (inject once) ──────────────────────────────────
		final String uuid = ganttLayout.getUuid();
		Clients.evalJavaScript(
			"window._zkGanttClick = function(id) {" +
			"  zAu.send(new zk.Event(zk.Widget.$('" + uuid + "'), 'onGanttClick', id));" +
			"};" +
			"window._zkGanttDrop = function(e, projectId) {" +
			"  var reqId = window._zkGanttDragging || '';" +
			"  if (!reqId) return;" +
			"  window._zkGanttDragging = null;" +
			"  zAu.send(new zk.Event(zk.Widget.$('" + uuid + "'), 'onGanttDrop'," +
			"    {requestId: reqId, projectId: projectId}));" +
			"};"
		);
		ganttLayout.addEventListener("onGanttClick",
			e -> openRequestUpdate((Integer) e.getData()));
		ganttLayout.addEventListener("onGanttDrop", e -> onGanttDropHandler(e));

		ganttControlsInitialized = true;
	}

	private static LocalDate dateToLocalDate(java.util.Date d) {
		if (d instanceof java.sql.Date)
			return ((java.sql.Date) d).toLocalDate();
		return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
	}

	private void onGanttDateChange(Hlayout rangeBar) {
		java.util.Date fv = ganttFromBox.getValue();
		java.util.Date tv = ganttToBox.getValue();
		if (fv == null || tv == null) return;
		LocalDate from = dateToLocalDate(fv);
		LocalDate to   = dateToLocalDate(tv);
		if (from.isAfter(to)) {
			Clients.showNotification(
				Msg.getMsg(Env.getCtx(), "RK_InvalidDateRange"),
				"error", ganttFromBox, "end_center", 3000);
			return;
		}
		ganttFrom = from;
		ganttTo   = to;
		ganttRange = "custom";
		// Deactivate all range pills
		for (Component sib : rangeBar.getChildren()) {
			if (sib instanceof Button && sib.getAttribute("ganttRange") != null) {
				((Button) sib).setSclass("rk-pill");
			}
		}
		refreshGantt();
	}

	private void refreshGantt() {
		if (!ganttControlsInitialized) return;
		// Ensure dates are set
		if (ganttFrom == null || ganttTo == null) resolveGanttRange();

		PreparedStatement[] pstmtHolder = new PreparedStatement[1];
		ResultSet rs = null;
		try {
			ResultSet[] result = loadGanttData(pstmtHolder);
			if (result == null) {
				// Scope returned no results (e.g., Subordinates with no subs)
				ganttHtml.setContent(emptyStateHtml());
				return;
			}
			rs = result[0];
			if (!rs.next()) {
				ganttHtml.setContent(emptyStateHtml());
				return;
			}
			// rs is now positioned at first row — pass to buildGanttHtmlFromFirstRow
			ganttHtml.setContent(buildGanttHtmlFromFirstRow(rs));
		} catch (Exception ex) {
			ganttHtml.setContent("<div style=\"color:red;padding:20px;\">Error: "
				+ escHtml(ex.getMessage()) + "</div>");
		} finally {
			DB.close(rs, pstmtHolder[0]);
		}
	}

	private void refreshData() {
		clearBoxes();
		addOpenItem();
		updateBoxStatus();
		if (VIEW_GANTT.equals(currentView) && ganttControlsInitialized) {
			refreshGantt();
		}
	}

	private void clearBoxes() {
		for (Component col : kanbanLayout.getChildren()) {
			if (col instanceof Vlayout) {
				Listbox lb = getColumnListbox(col);
				if (lb != null) lb.getItems().clear();
			}
		}
		if (listTabbox.getTabpanels() != null) {
			for (Component panel : listTabbox.getTabpanels().getChildren()) {
				Listbox lb = (Listbox) panel.getFirstChild();
				lb.getItems().clear();
			}
		}
	}

	private void updateBoxStatus() {
		for (Component col : kanbanLayout.getChildren()) {
			if (col instanceof Vlayout) {
				Hlayout header = (Hlayout) col.getFirstChild();
				Label lbl = null;
				for (Component c : header.getChildren()) {
					if (c instanceof Label) {
						lbl = (Label) c;
						break;
					}
				}
				if (lbl != null) {
					Listbox lb = getColumnListbox(col);
					if (lb == null) continue;
					String name = lb.getId().substring(8);
					lbl.setValue(getStatusName(name) + " (" + lb.getItemCount() + ")");
					if (lb.getItemCount() == 0) {
						Listitem empty = new Listitem();
						empty.setStyle("min-height: 150px;");
						Listcell cell = new Listcell();
						cell.setStyle("display: flex; align-items: center; justify-content: center; min-height: 150px;");
						Label lbl2 = new Label(Msg.getMsg(Env.getCtx(), "RK_NoRequests"));
						lbl2.setStyle("color: #aaa; font-size: 12px; text-align: center;");
						cell.appendChild(lbl2);
						empty.appendChild(cell);
						lb.appendChild(empty);
					}
				}
			}
		}
		if (listTabbox.getTabs() != null) {
			int i = 0;
			for (Component tabObj : listTabbox.getTabs().getChildren()) {
				Tab tab = (Tab) tabObj;
				Tabpanel panel = (Tabpanel) listTabbox.getTabpanels().getChildren().get(i);
				Listbox lb = (Listbox) panel.getFirstChild();
				String name = lb.getId().substring(13); 
				tab.setLabel(getStatusName(name) + " (" + lb.getItemCount() + ")");
				i++;
			}
		}
	}

	private Listbox getColumnListbox(Component col) {
		for (Component c : col.getChildren()) {
			if (c instanceof Listbox) return (Listbox) c;
		}
		return null;
	}

	private String getStatusName(String value) {
		for (MStatus s : statuses) {
			if (s.getValue().equals(value)) return s.getName();
		}
		return value;
	}

	/**
	 * Returns a 16×16 icon Image for the given R_Status.
	 * Priority 1: first image-type attachment on the R_Status record.
	 * Priority 2: Help field containing an image path (legacy).
	 */
	private Image getStatusIconImage(MStatus mStatus) {
		// Priority 1: attachment on R_Status record
		try {
			MAttachment att = MAttachment.get(Env.getCtx(), MStatus.Table_ID, mStatus.getR_Status_ID());
			if (att != null && att.getEntryCount() > 0) {
				for (int i = 0; i < att.getEntryCount(); i++) {
					org.compiere.model.MAttachmentEntry entry = att.getEntry(i);
					String n = entry.getName().toLowerCase();
					if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
							|| n.endsWith(".gif") || n.endsWith(".webp")) {
						byte[] data = entry.getData();
						if (data != null && data.length > 0) {
							org.zkoss.image.AImage aimg = new org.zkoss.image.AImage(entry.getName(), data);
							Image icon = new Image();
							icon.setContent(aimg);
							icon.setWidth("16px");
							icon.setHeight("16px");
							icon.setStyle("margin-right: 5px;");
							return icon;
						}
					}
				}
			}
		} catch (Exception ex) {
			logger.log(Level.WARNING, "Status icon attachment load failed: " + mStatus.getValue(), ex);
		}
		// Priority 2: Help field image path
		if (mStatus.getHelp() != null && mStatus.getHelp().toLowerCase().endsWith(".png")) {
			String iconSrc = mStatus.getHelp();
			if (!iconSrc.startsWith("/") && !iconSrc.startsWith("http")) iconSrc = "/" + iconSrc;
			Image icon = new Image(iconSrc);
			icon.setWidth("16px");
			icon.setHeight("16px");
			icon.setStyle("margin-right: 5px;");
			return icon;
		}
		return null;
	}

	private String getInitials(String name) {
		if (name == null || name.isEmpty()) return "?";
		String[] parts = name.trim().split("\\s+");
		if (parts.length >= 2 && !parts[0].isEmpty() && !parts[parts.length - 1].isEmpty()) {
			return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase();
		}
		// Single word (common for Chinese names) — take first 1–2 chars
		return name.length() >= 2 ? name.substring(0, 2) : name.substring(0, 1).toUpperCase();
	}

	private void addOpenItem() {
		StringBuilder sql = new StringBuilder("select StartTime, EndTime\n")
				.append(",(select name from ad_user where ad_user_id = R_Request.salesrep_id ) Responsible ")
				.append(", (select name from ad_user where ad_user_id = R_Request.ad_user_id ) Customer ")
				.append(",Summary ")
				.append(",DocumentNo ")
				.append(",StartDate ")
				.append(",R_Status_ID ")
				.append(",R_Request_ID ")
				.append(",Priority ")
				.append(", (select count(*) from ad_attachment where ad_table_id = 417 and record_id = R_Request.R_Request_ID) AttachmentCount ")
				.append(" from R_Request ")
				.append(" where exists (select 1 from R_Status where R_Status_ID = R_Request.R_Status_ID and IsFinalClose != 'Y') ")
				.append(" and StartDate is not null ");
		
		Properties ctx = Env.getCtx();
		String ss = currentScope;

		// #AD_User_ID is always set for any authenticated iDempiere session
		int userId = Integer.parseInt(ctx.getProperty("#AD_User_ID"));

		switch(ss) {
			case "Private":
				sql.append(" and ( AD_User_ID = ? OR SalesRep_ID = ? )");
				break;
			case "Subordinates":
				List<Integer> subs = getSubordinateIds(userId);
				if (subs.isEmpty()) return; // user has no subordinates — nothing to show for this scope
				String inClause = subs.stream().map(String::valueOf).collect(Collectors.joining(","));
				// inClause contains Java int values from a typed ResultSet — not user input
				sql.append(" and ( AD_User_ID IN (" + inClause + ") OR SalesRep_ID IN (" + inClause + ") )");
				break;
			case "Team":
				sql.append(" and ( AD_User_ID = ? OR SalesRep_ID = ? ")
				   .append(" OR exists (select 1 from AD_User_Roles where AD_Role_ID = R_Request.AD_Role_ID AND AD_User_ID = ").append(ctx.getProperty("#AD_User_ID")).append(" ) )");
				break;
			case "All":
				sql.append(" and ? = ? "); 
				break;
		}

		if (searchFilter != null && !searchFilter.isEmpty()) {
			sql.append(" and (UPPER(Summary) LIKE ? OR UPPER(DocumentNo) LIKE ?)");
		}
		
		sql.append(" order by Priority ASC, StartDate DESC");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), null);
			int idx = 1;
			if (ss.equals("All")) {
				pstmt.setInt(idx++, 1);
				pstmt.setInt(idx++, 1);
			} else if (!ss.equals("Subordinates")) {
				// Subordinates case has no bind params — IDs are inlined in the WHERE clause
				pstmt.setInt(idx++, userId);
				pstmt.setInt(idx++, userId);
			}
			if (searchFilter != null && !searchFilter.isEmpty()) {
				String filter = "%" + searchFilter.toUpperCase() + "%";
				pstmt.setString(idx++, filter);
				pstmt.setString(idx++, filter);
			}

			rs = pstmt.executeQuery();
			while (rs.next()) {
				int statusId = rs.getInt("R_Status_ID");
				
				Listbox kanbanLb = findListbox(kanbanLayout, statusId);
				if (kanbanLb != null) {
					kanbanLb.appendChild(createKanbanItem(rs));
				}

				Listbox listLb = findListbox(listTabbox, statusId);
				if (listLb != null) {
					listLb.appendChild(createListItem(rs));
				}
			}
		} catch (SQLException ex) {
			throw new AdempiereException("Unable to load request items", ex);
		} finally {
			DB.close(rs, pstmt);
		}
		updateBoxStatus();
	}

	/**
	 * Loads gantt data. Caller is responsible for closing rs and pstmt via DB.close().
	 * Returns null if scope is "Subordinates" and user has no subordinates.
	 */
	private ResultSet[] loadGanttData(PreparedStatement[] pstmtOut) throws SQLException {
		Properties ctx = Env.getCtx();
		int userId = Integer.parseInt(ctx.getProperty("#AD_User_ID"));
		String ss = currentScope;

		StringBuilder sql = new StringBuilder(
			"SELECT r.R_Request_ID, r.DocumentNo, r.Summary, r.Priority," +
			" r.StartTime, r.EndTime, r.CloseDate, r.R_Status_ID," +
			" (SELECT Name FROM AD_User WHERE AD_User_ID = r.SalesRep_ID) AS Responsible," +
			" (SELECT Name FROM AD_User WHERE AD_User_ID = r.AD_User_ID)  AS Customer," +
			" r.C_Project_ID, p.Name AS ProjectName" +
			" FROM R_Request r" +
			" LEFT JOIN C_Project p ON p.C_Project_ID = r.C_Project_ID" +
			" WHERE r.StartDate IS NOT NULL" +
			"   AND r.IsActive = 'Y'"
		);
		if (!showFinalClose) {
			sql.append("   AND EXISTS (SELECT 1 FROM R_Status WHERE R_Status_ID = r.R_Status_ID" +
			           "               AND IsFinalClose != 'Y')");
		}

		// Scope filter (same patterns as addOpenItem)
		switch (ss) {
			case "Private":
				sql.append(" AND (r.AD_User_ID = ? OR r.SalesRep_ID = ?)");
				break;
			case "Subordinates":
				List<Integer> subs = getSubordinateIds(userId);
				if (subs.isEmpty()) return null; // nothing to show
				String inClause = subs.stream().map(String::valueOf)
				                     .collect(Collectors.joining(","));
				sql.append(" AND (r.AD_User_ID IN (").append(inClause)
				   .append(") OR r.SalesRep_ID IN (").append(inClause).append("))");
				break;
			case "Team":
				sql.append(" AND (r.AD_User_ID = ? OR r.SalesRep_ID = ?")
				   .append(" OR EXISTS (SELECT 1 FROM AD_User_Roles")
				   .append(" WHERE AD_Role_ID = r.AD_Role_ID AND AD_User_ID = ")
				   .append(userId).append("))");
				break;
			case "All":
			default:
				sql.append(" AND ? = ?");
				break;
		}

		// Search filter
		if (searchFilter != null && !searchFilter.isEmpty()) {
			sql.append(" AND (UPPER(r.Summary) LIKE ? OR UPPER(r.DocumentNo) LIKE ?)");
		}

		// Date-range overlap filter
		sql.append(" AND (r.StartTime IS NULL OR r.StartTime <= ?)")
		   .append(" AND (r.EndTime   IS NULL OR r.EndTime   >= ?)")
		   .append(" ORDER BY r.C_Project_ID NULLS LAST," +
		           " r.SalesRep_ID NULLS LAST," +
		           " COALESCE(r.StartTime, '9999-01-01'::timestamp)");

		PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);
		pstmtOut[0] = pstmt;

		int idx = 1;
		if ("All".equals(ss)) {
			pstmt.setInt(idx++, 1);
			pstmt.setInt(idx++, 1);
		} else if (!"Subordinates".equals(ss)) {
			pstmt.setInt(idx++, userId);
			pstmt.setInt(idx++, userId);
		}
		if (searchFilter != null && !searchFilter.isEmpty()) {
			String f = "%" + searchFilter.toUpperCase() + "%";
			pstmt.setString(idx++, f);
			pstmt.setString(idx++, f);
		}
		// ganttTo as end-of-day, ganttFrom as start-of-day
		pstmt.setTimestamp(idx++,
			java.sql.Timestamp.valueOf(ganttTo.atTime(23, 59, 59)));
		pstmt.setTimestamp(idx,
			java.sql.Timestamp.valueOf(ganttFrom.atStartOfDay()));

		return new ResultSet[]{ pstmt.executeQuery() };
	}

	/** Returns [backgroundHex, textHex] for a given R_Status_ID. */
	private String[] getGanttBarColors(int statusId) {
		for (MStatus s : statuses) {
			if (s.getR_Status_ID() == statusId) {
				switch (s.getValue()) {
					case "Open":       return new String[]{"#deebff", "#0747a6"};
					case "Processing": return new String[]{"#fffae6", "#7a4500"};
					case "Verify":     return new String[]{"#e3fcef", "#006644"};
					case "Problem":    return new String[]{"#ffebe6", "#bf2600"};
					case "Closed":     return new String[]{"#dfe1e6", "#555"};
					default:           return new String[]{"#dfe1e6", "#555"};
				}
			}
		}
		// Status not in the non-final-close list — treat as final closed (dark gray)
		return new String[]{"#c4c9d4", "#333"};
	}

	/** Returns the left-border color hex for the given priority string. */
	private String getGanttPriorityBorder(String priority) {
		if ("1".equals(priority)) return "#bf2600"; // Urgent
		if ("2".equals(priority)) return "#ff8b00"; // High
		if ("3".equals(priority)) return "#ffe380"; // Medium
		return "#36b37e";                           // Low / other
	}

	private String emptyStateHtml() {
		return "<div style=\"display:flex;align-items:center;justify-content:center;" +
			   "min-height:120px;color:#aaa;font-size:13px;\">" +
			   Msg.getMsg(Env.getCtx(), "RK_NoRequests") +
			   "</div>";
	}

	/** Escapes HTML special characters to prevent XSS in generated HTML. */
	private String escHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;")
				 .replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** Loads active C_Project records for the current client into cachedProjects / cachedProjectNames. */
	private void loadProjectList() {
		cachedProjects.clear();
		cachedProjectNames.clear();
		String sql = "SELECT C_Project_ID, Name FROM C_Project " +
		             "WHERE AD_Client_ID = ? AND IsActive = 'Y' ORDER BY Name";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				cachedProjects.add(new int[]{ rs.getInt(1) });
				cachedProjectNames.add(rs.getString(2));
			}
		} catch (Exception ex) {
			logger.log(Level.WARNING, "loadProjectList failed", ex);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/** Renders the left project list as an HTML fragment for projectPanelHtml. */
	private String buildProjectPanelHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"padding:4px 0;\">");
		if (cachedProjects.isEmpty()) {
			sb.append("<div style=\"color:#aaa;font-size:11px;padding:8px 10px;\">")
			  .append(Msg.getMsg(Env.getCtx(), "RK_NoProjects"))
			  .append("</div>");
		} else {
			for (int i = 0; i < cachedProjects.size(); i++) {
				int projectId = cachedProjects.get(i)[0];
				String name   = cachedProjectNames.get(i);
				sb.append("<div")
				  .append(" id=\"proj-").append(projectId).append("\"")
				  .append(" ondragover=\"event.preventDefault();this.style.background='#dbeafe';\"")
				  .append(" ondragleave=\"this.style.background='';\"")
				  .append(" ondrop=\"event.preventDefault();this.style.background='';window._zkGanttDrop(event,").append(projectId).append(");\"")
				  .append(" style=\"padding:6px 10px;font-size:12px;cursor:default;border-radius:4px;")
				  .append("border:1px solid #e0e0e0;margin-bottom:4px;background:#fff;")
				  .append("white-space:nowrap;overflow:hidden;text-overflow:ellipsis;\"")
				  .append(" title=\"").append(escHtml(name)).append("\">")
				  .append("📁 ").append(escHtml(name))
				  .append("</div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	/** Reloads project list from DB and re-renders the left panel. */
	private void refreshProjectPanel() {
		if (projectPanelHtml == null) return;
		loadProjectList();
		projectPanelHtml.setContent(buildProjectPanelHtml());
	}

	private String buildGanttHtmlFromFirstRow(java.sql.ResultSet rs) throws java.sql.SQLException {
		// ── Determine column granularity ──────────────────────────────
		long spanDays = ChronoUnit.DAYS.between(ganttFrom, ganttTo) + 1;
		String colUnit; // "day" | "week" | "month"
		if ("week".equals(ganttRange) || spanDays <= 14) {
			colUnit = "day";
		} else if ("quarter".equals(ganttRange) || spanDays > 60) {
			colUnit = "month";
		} else {
			colUnit = "week"; // each Monday
		}

		// ── Build list of column start dates ─────────────────────────
		List<LocalDate> cols = new ArrayList<>();
		if ("day".equals(colUnit)) {
			for (LocalDate d = ganttFrom; !d.isAfter(ganttTo); d = d.plusDays(1))
				cols.add(d);
		} else if ("week".equals(colUnit)) {
			// Always include ganttFrom as first label
			cols.add(ganttFrom);
			// Then add each Monday after ganttFrom
			LocalDate mon = ganttFrom.with(DayOfWeek.MONDAY);
			if (!mon.isAfter(ganttFrom)) mon = mon.plusWeeks(1);
			for (LocalDate d = mon; !d.isAfter(ganttTo); d = d.plusWeeks(1))
				cols.add(d);
		} else { // "month"
			// Start with ganttFrom, then add 1st of each subsequent month
			cols.add(ganttFrom);
			for (LocalDate d = ganttFrom.withDayOfMonth(1).plusMonths(1);
				 !d.isAfter(ganttTo); d = d.plusMonths(1))
				cols.add(d);
		}

		int N = cols.size(); // number of date columns
		int totalCols = 1 + N; // +1 for the request-name column
		LocalDate today = LocalDate.now();
		long totalDays = ChronoUnit.DAYS.between(ganttFrom, ganttTo) + 1;

		// ── HTML assembly ─────────────────────────────────────────────
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"overflow-x:auto;\">");
		sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:11px;min-width:600px;\">");

		// -- Header row --
		sb.append("<thead><tr style=\"background:#f4f5f7;border-bottom:2px solid #ddd;\">");
		sb.append("<th style=\"width:160px;padding:6px 10px;text-align:left;color:#555;" +
				  "font-weight:600;border-right:1px solid #ddd;\">")
		  .append(Msg.getMsg(Env.getCtx(), "DocumentNo"))
		  .append("</th>");
		for (LocalDate col : cols) {
			boolean isToday = "day".equals(colUnit)
				? col.equals(today)
				: (!col.isAfter(today) &&
				   (cols.indexOf(col) == cols.size() - 1 || cols.get(cols.indexOf(col) + 1).isAfter(today)));
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
		boolean teamMode = !"Private".equals(currentScope);
		String lastResponsible = null;

		do {
			int requestId   = rs.getInt("R_Request_ID");
			String docNo    = rs.getString("DocumentNo");
			String summary  = rs.getString("Summary");
			String priority = rs.getString("Priority");
			int statusId    = rs.getInt("R_Status_ID");
			java.sql.Timestamp startTs = rs.getTimestamp("StartTime");
			java.sql.Timestamp endTs   = rs.getTimestamp("EndTime");
			java.sql.Date closeDate    = rs.getDate("CloseDate");
			// Use CloseDate as fallback when EndTime is null
			if (endTs == null && closeDate != null)
				endTs = new java.sql.Timestamp(closeDate.getTime());
			String responsible = rs.getString("Responsible");
			String customer    = rs.getString("Customer");

			// Group header (team mode only)
			if (teamMode) {
				String respKey = responsible != null ? responsible : "";
				if (!respKey.equals(lastResponsible)) {
					lastResponsible = respKey;
					String dispName = responsible != null && !responsible.isEmpty()
						? responsible
						: Msg.getMsg(Env.getCtx(), "RK_Unassigned");
					sb.append("<tr style=\"background:#f0f4ff;\">")
					  .append("<td colspan=\"").append(totalCols).append("\"")
					  .append(" style=\"padding:5px 10px;font-size:11px;font-weight:700;color:#0052cc;\">")
					  .append("👤 ").append(escHtml(dispName))
					  .append("</td></tr>");
				}
			}

			// No-date row (both StartTime and EndTime null, with no CloseDate fallback)
			if (startTs == null && endTs == null) {
				String summaryTrunc = summary != null && summary.length() > 30
					? summary.substring(0, 30) + "…" : (summary != null ? summary : "");
				sb.append("<tr style=\"opacity:0.5;border-bottom:1px solid #f0f0f0;\">")
				  .append("<td onclick=\"window._zkGanttClick(").append(requestId).append(")\"")
				  .append(" style=\"padding:6px 10px;border-right:1px solid #ddd;" +
						  "font-size:11px;color:#333;cursor:pointer;\">")
				  .append("#").append(escHtml(docNo)).append(" — ").append(escHtml(summaryTrunc))
				  .append("</td>")
				  .append("<td colspan=\"").append(N).append("\"")
				  .append(" style=\"padding:4px 10px;color:#aaa;font-size:10px;font-style:italic;\">")
				  .append("— ").append(Msg.getMsg(Env.getCtx(), "RK_NoDateSet"))
				  .append("</td></tr>");
				continue;
			}

			// Bar math
			LocalDate startDate = startTs != null
				? startTs.toLocalDateTime().toLocalDate() : ganttFrom;
			LocalDate endDate   = endTs   != null
				? endTs.toLocalDateTime().toLocalDate()   : ganttTo;

			double leftPct  = Math.max(0,
				ChronoUnit.DAYS.between(ganttFrom, startDate)) * 100.0 / totalDays;
			double widthPct = Math.max(2,
				Math.min(100.0 - leftPct,
					(ChronoUnit.DAYS.between(startDate, endDate) + 1) * 100.0 / totalDays));

			String[] colors    = getGanttBarColors(statusId);
			String bg          = colors[0];
			String textColor   = colors[1];
			String borderColor = getGanttPriorityBorder(priority);

			// Request name column
			sb.append("<tr style=\"border-bottom:1px solid #f0f0f0;\">")
			  .append("<td onclick=\"window._zkGanttClick(").append(requestId).append(")\"")
			  .append(" style=\"padding:6px 10px;border-right:1px solid #ddd;cursor:pointer;\">")
			  .append("<div style=\"font-weight:600;color:#333;font-size:11px;\">#")
			  .append(escHtml(docNo)).append("</div>")
			  .append("<div style=\"color:#888;font-size:10px;white-space:nowrap;overflow:hidden;" +
					  "text-overflow:ellipsis;max-width:140px;\">")
			  .append(escHtml(summary != null ? summary : ""))
			  .append("</div></td>");

			// Bar label: (customer/applicant) start~end
			java.time.format.DateTimeFormatter barFmt = java.time.format.DateTimeFormatter.ofPattern("M/d");
			StringBuilder barLabel = new StringBuilder();
			if (customer != null && !customer.isEmpty())
				barLabel.append("(").append(escHtml(customer)).append(") ");
			barLabel.append(startTs != null ? startTs.toLocalDateTime().toLocalDate().format(barFmt) : "");
			barLabel.append("~");
			barLabel.append(endTs   != null ? endTs.toLocalDateTime().toLocalDate().format(barFmt)   : "");

			// Bar column (spans all date columns)
			sb.append("<td colspan=\"").append(N).append("\"")
			  .append(" style=\"padding:3px 2px;position:relative;height:32px;\">")
			  .append("<div onclick=\"window._zkGanttClick(").append(requestId).append(")\"")
			  .append(" title=\"#").append(escHtml(docNo)).append(": ")
			  .append(escHtml(summary != null ? summary : "")).append("\"")
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
			  .append(escHtml(getStatusName(sl[1])))
			  .append("</span>");
		}
		sb.append("<span style=\"margin-left:12px;\"><strong style=\"color:#444;\">左邊框 = 優先權</strong></span>");
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

	private Listbox findListbox(Component parent, int statusId) {
		if (parent instanceof Listbox) {
			Object attr = parent.getAttribute("R_Status_ID");
			if (attr != null && (int)attr == statusId) return (Listbox) parent;
		}
		for (Component child : parent.getChildren()) {
			Listbox found = findListbox(child, statusId);
			if (found != null) return found;
		}
		return null;
	}

	private Listitem createKanbanItem(ResultSet rs) throws SQLException {
		int requestId = rs.getInt("R_Request_ID");
		String summaryTxt = rs.getString("Summary");
		int priority = rs.getInt("Priority");
		int attachmentCount = rs.getInt("AttachmentCount");

		Listitem item = new Listitem();
		item.setAttribute("R_Request_ID", requestId);
		item.setDraggable("true");

		Listcell cell = new Listcell();
		item.appendChild(cell);

		Vlayout card = new Vlayout();
		card.setSpacing("5px");
		card.setStyle("padding: 10px; border: 1px solid #ddd; border-radius: 8px; background-color: " + getPriorityColor(priority) + "; cursor: pointer; margin-bottom: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);");

		Label lblSummary = new Label(summaryTxt);
		lblSummary.setMultiline(true);
		lblSummary.setMaxlength(100);
		lblSummary.setStyle("font-weight: 500; font-size: 13px; color: #333;");
		card.appendChild(lblSummary);

		String customer = rs.getString("Customer");
		String responsible = rs.getString("Responsible");

		Hlayout peopleRow = new Hlayout();
		peopleRow.setHflex("1");
		peopleRow.setValign("middle");
		if (customer != null) {
			Label lblCustomer = new Label("👤 " + customer);
			lblCustomer.setStyle("font-size: 11px; color: #777;");
			peopleRow.appendChild(lblCustomer);
		}
		if (responsible != null) {
			Label lblSalesRep = new Label("🔧 " + responsible);
			lblSalesRep.setStyle("font-size: 11px; color: #555; margin-left: 10px;");
			peopleRow.appendChild(lblSalesRep);
		}
		card.appendChild(peopleRow);

		java.sql.Timestamp endTs = rs.getTimestamp("EndTime");

		Hlayout lastRow = new Hlayout();
		lastRow.setHflex("1");
		lastRow.setValign("middle");

		java.sql.Date startDate = rs.getDate("StartDate");
		if (startDate != null) {
			LocalDate ld = startDate.toLocalDate();
			Label lblStart = new Label("📅 " + ld.getYear() + "/" + ld.getMonthValue() + "/" + ld.getDayOfMonth());
			lblStart.setStyle("font-size: 10px; color: #888;");
			lastRow.appendChild(lblStart);
		}

		if (endTs != null) {
		    LocalDate endDate = endTs.toLocalDateTime().toLocalDate();
		    LocalDate today = LocalDate.now();
		    long daysLeft = ChronoUnit.DAYS.between(today, endDate);
		    String chipStyle;
		    String chipText;
		    if (daysLeft <= 0) {
		        chipStyle = "background:#ffebe6;color:#bf2600;padding:1px 5px;border-radius:3px;font-size:10px;font-weight:600;margin-left:6px;";
		        chipText = "🔴 逾期";
		    } else if (daysLeft <= 3) {
		        chipStyle = "background:#fffae6;color:#7a6200;padding:1px 5px;border-radius:3px;font-size:10px;font-weight:600;margin-left:6px;";
		        chipText = "⏰ " + endDate.getMonthValue() + "/" + endDate.getDayOfMonth();
		    } else {
		        chipStyle = "background:#e3fcef;color:#006644;padding:1px 5px;border-radius:3px;font-size:10px;font-weight:600;margin-left:6px;";
		        chipText = "⏰ " + endDate.getMonthValue() + "/" + endDate.getDayOfMonth();
		    }
		    Label lblDue = new Label(chipText);
		    lblDue.setStyle(chipStyle);
		    lastRow.appendChild(lblDue);
		}

		Space lastSpacer = new Space();
		lastSpacer.setHflex("1");
		lastRow.appendChild(lastSpacer);

		if (attachmentCount > 0) {
			Image imgAttach = new Image("/images/kanban/Attachment24.png");
			imgAttach.setWidth("16px");
			imgAttach.setHeight("16px");
			lastRow.appendChild(imgAttach);
		}

		card.appendChild(lastRow);

		cell.appendChild(card);
		item.addEventListener(Events.ON_CLICK, e -> openRequestUpdate(requestId));
		
		return item;
	}

	private Listitem createListItem(ResultSet rs) throws SQLException {
		int requestId = rs.getInt("R_Request_ID");
		int attachmentCount = rs.getInt("AttachmentCount");
		Listitem item = new Listitem();
		item.setAttribute("R_Request_ID", requestId);
		
		Listcell docCell = new Listcell();
		Hlayout h = new Hlayout();
		h.setValign("middle");
		h.appendChild(new Label(rs.getString("DocumentNo")));
		if (attachmentCount > 0) {
			Image img = new Image("/images/kanban/Attachment24.png");
			img.setWidth("14px");
			h.appendChild(img);
		}
		docCell.appendChild(h);
		item.appendChild(docCell);
		
		item.appendChild(new Listcell(rs.getString("Summary")));
		item.appendChild(new Listcell(rs.getString("Responsible")));
		
		SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Timestamp startTime = rs.getTimestamp("StartTime");
		Timestamp endTime = rs.getTimestamp("EndTime");
		item.appendChild(new Listcell(startTime != null ? sdfTime.format(startTime) : ""));
		item.appendChild(new Listcell(endTime != null ? sdfTime.format(endTime) : ""));
		
		Listcell priorityCell = new Listcell(rs.getString("Priority"));
		priorityCell.setStyle("background-color: " + getPriorityColor(rs.getInt("Priority")));
		item.appendChild(priorityCell);
		
		item.addEventListener(Events.ON_CLICK, e -> openRequestUpdate(requestId));
		return item;
	}

	private String getPriorityColor(int priority) {
		if (priority <= 1) return "#F8BBD0";      // Urgent - Pink
		if (priority <= 3) return "#FFE0B2";      // High - Orange
		if (priority <= 5) return "#FFF9C4";      // Medium - Yellow
		if (priority <= 7) return "#DCEDC8";      // Low - Light Green
		return "#F5F5F5";                         // Minor - Grey
	}

	private String getPriorityDotColor(int priority) {
		if (priority <= 1) return "#E91E63";
		if (priority <= 3) return "#FF9800";
		if (priority <= 5) return "#FBC02D";
		if (priority <= 7) return "#8BC34A";
		return "#9E9E9E";
	}

	private void openRequestUpdate(int requestId) {
		currentRequest = new MRequest(Env.getCtx(), requestId, null);
		MRequestUpdate[] updates = currentRequest.getUpdates(null);
		
		dialog = (Window)Executions.createComponents("/zul/request-update.zul", this, null);
		dialog.setTitle(Msg.getMsg(Env.getCtx(), "RK_RequestFormTitle"));
		
		setupUpdateDialog(dialog, currentRequest, updates);
		dialog.doModal();
	}

	private void setupUpdateDialog(Window dialog, MRequest request, MRequestUpdate[] updates) {
		Div requestDoc = (Div) dialog.getFellow("requestDoc");
		requestDoc.getChildren().clear();

		boolean canEdit = canEditRequest(request);

		// Update dialog title to include DocumentNo
		dialog.setTitle(Msg.getMsg(Env.getCtx(), "RK_RequestFormTitle") + " — #" + request.getDocumentNo());

		// ── 📋 基本資訊 ───────────────────────────────────────
		Label sec1 = new Label("📋 " + Msg.getMsg(Env.getCtx(), "RK_BasicInfo"));
		sec1.setStyle("font-size: 11px; font-weight: 700; color: #888; letter-spacing: 0.5px; margin: 10px 0 6px;");
		requestDoc.appendChild(sec1);

		Grid grid = new Grid();
		grid.setHflex("1");
		Columns cols = new Columns();
		Column labelCol = new Column(); labelCol.setWidth("80px");
		Column fieldCol = new Column(); fieldCol.setHflex("1");
		cols.appendChild(labelCol); cols.appendChild(fieldCol);
		grid.appendChild(cols);
		Rows rows = new Rows();
		grid.appendChild(rows);

		// Priority
		MLookup userPriorityL = MLookupFactory.get(Env.getCtx(), 0, 0, 5426, DisplayType.List);
		fPriority = new WTableDirEditor("Priority", false, false, true, userPriorityL);
		fPriority.setMandatory(true);
		fPriority.setValue(request.getPriority());
		fPriority.setReadWrite(canEdit);
		addDialogRow(rows, Msg.getMsg(Env.getCtx(), "RK_Priority"), fPriority.getComponent());

		// SalesRep
		MLookup srL = MLookupFactory.get(Env.getCtx(), 0, 0, 5432, DisplayType.Search);
		fSalesRep = new WSearchEditor("SalesRep_ID", false, false, true, srL);
		fSalesRep.setValue(request.getSalesRep_ID());
		fSalesRep.setReadWrite(canEdit);
		addDialogRow(rows, Msg.getMsg(Env.getCtx(), "RK_SalesRep"), fSalesRep.getComponent());

		// StartTime + EndTime on one row
		fStartTime = new Datebox();
		fStartTime.setHflex("1");
		fStartTime.setFormat("yyyy-MM-dd HH:mm");
		if (request.getStartTime() != null) fStartTime.setValue(request.getStartTime());
		fStartTime.setDisabled(!canEdit);

		fEndTime = new Datebox();
		fEndTime.setHflex("1");
		fEndTime.setFormat("yyyy-MM-dd HH:mm");
		if (request.getEndTime() != null) fEndTime.setValue(request.getEndTime());
		fEndTime.setDisabled(!canEdit);

		Hlayout timeRow = new Hlayout();
		timeRow.setHflex("1");
		timeRow.setSpacing("6px");
		timeRow.appendChild(fStartTime);
		Label lblTo = new Label("→");
		lblTo.setStyle("color: #888;");
		timeRow.appendChild(lblTo);
		timeRow.appendChild(fEndTime);
		addDialogRow(rows,
			Msg.getMsg(Env.getCtx(), "RK_StartTime") + " / " + Msg.getMsg(Env.getCtx(), "RK_EndTime"),
			timeRow);

		// Attachment
		Hlayout attachRow = new Hlayout();
		attachRow.setValign("middle");
		attachRow.setSpacing("8px");
		Button btnManageAttach = new Button(Msg.getMsg(Env.getCtx(), "Attachment"));
		btnManageAttach.setImage("/images/kanban/Attachment24.png");
		btnManageAttach.addEventListener(Events.ON_CLICK, e -> {
			WAttachment wa = new WAttachment(0, 0, MRequest.Table_ID, request.getR_Request_ID(), null, ev -> refreshData());
			wa.setWidth("800px");
			wa.setHeight("600px");
			wa.setSclass("attachment-all");
			wa.setShadow(true);
			wa.setBorder("normal");
			wa.setClosable(true);
			wa.setSizable(true);
			wa.setMaximizable(true);
			wa.doModal();
		});
		attachRow.appendChild(btnManageAttach);
		int attachmentCount = DB.getSQLValue(null,
			"select count(*) from ad_attachment where ad_table_id = ? and record_id = ?",
			MRequest.Table_ID, request.getR_Request_ID());
		if (attachmentCount > 0) {
			Label lblCount = new Label("(" + attachmentCount + ")");
			lblCount.setStyle("font-weight: bold; color: #336699;");
			attachRow.appendChild(lblCount);
		}
		addDialogRow(rows, Msg.getMsg(Env.getCtx(), "Attachment"), attachRow);

		requestDoc.appendChild(grid);

		// ── 📝 摘要 ───────────────────────────────────────────
		Label sec2 = new Label("📝 " + Msg.getMsg(Env.getCtx(), "RK_Summary"));
		sec2.setStyle("font-size: 11px; font-weight: 700; color: #888; letter-spacing: 0.5px; margin: 10px 0 4px;");
		requestDoc.appendChild(sec2);

		Textbox summaryTxt = new Textbox(request.getSummary());
		summaryTxt.setMultiline(true);
		summaryTxt.setHeight("80px");
		summaryTxt.setHflex("1");
		summaryTxt.setReadonly(true);
		summaryTxt.setStyle("background-color: rgba(255, 255, 128, .5);");
		requestDoc.appendChild(summaryTxt);

		// ── 💬 更新歷程 ───────────────────────────────────────
		Label sec3 = new Label("💬 " + Msg.getMsg(Env.getCtx(), "RK_UpdateHistory"));
		sec3.setStyle("font-size: 11px; font-weight: 700; color: #888; letter-spacing: 0.5px; margin: 10px 0 4px;");
		requestDoc.appendChild(sec3);

		Listbox boxUpdates = (Listbox) dialog.getFellow("boxUpdates");
		boxUpdates.getItems().clear();
		Listheader listHeaderUpdate = (Listheader) dialog.getFellow("listHeaderUpdate");
		listHeaderUpdate.setLabel(Msg.getMsg(Env.getCtx(), "RK_UpdateHistory"));

		if (updates.length == 0) {
			boxUpdates.appendChild(new Listitem(Msg.getMsg(Env.getCtx(), "RK_NoUpdates")));
		} else {
			SimpleDateFormat sdfTime = new SimpleDateFormat("MM-dd HH:mm");
			for (MRequestUpdate mRequestUpdate : updates) {
				Listitem item = new Listitem();
				Listcell cell = new Listcell();

				MUser user = new MUser(Env.getCtx(), mRequestUpdate.getUpdatedBy(), null);
				String userName = user.getName() != null ? user.getName() : "?";
				String initials = getInitials(userName);

				Hlayout h = new Hlayout();
				h.setValign("top");
				h.setSpacing("8px");

				// Avatar circle with initials
				Div avatar = new Div();
				avatar.setWidth("28px");
				avatar.setHeight("28px");
				avatar.setStyle("border-radius:50%;background:#1976d2;color:#fff;"
					+ "display:flex;align-items:center;justify-content:center;"
					+ "font-size:11px;font-weight:700;flex-shrink:0;");
				avatar.appendChild(new Label(initials));

				// Meta + message
				Vlayout content = new Vlayout();
				content.setSpacing("2px");
				Label lblMeta = new Label(userName + " · " + sdfTime.format(mRequestUpdate.getUpdated()));
				lblMeta.setStyle("color:#888;font-size:11px;");
				content.appendChild(lblMeta);
				content.appendChild(new Label(mRequestUpdate.getResult()));
				if (mRequestUpdate.getQtySpent() != null && mRequestUpdate.getQtySpent().floatValue() != 0) {
					String unit = mRequestUpdate.getQtySpent().floatValue() > 1 ? "Hours" : "Hour";
					Label lblQty = new Label("[" + mRequestUpdate.getM_ProductSpent().getValue()
						+ ": " + mRequestUpdate.getQtySpent()
						+ " " + Msg.getMsg(Env.getCtx(), unit) + "]");
					lblQty.setStyle("color:#555;font-size:11px;");
					content.appendChild(lblQty);
				}

				h.appendChild(avatar);
				h.appendChild(content);
				cell.appendChild(h);
				item.appendChild(cell);
				boxUpdates.appendChild(item);
			}
		}

		// New message textbox (readonly for non-editors)
		result = new Textbox();
		result.setRows(3);
		result.setMultiline(true);
		result.setHflex("1");
		result.setId("result");
		result.setReadonly(!canEdit);
		requestDoc.appendChild(result);

		// Product / Quantity — only shown when canEdit
		if (canEdit) {
			MLookup productL = MLookupFactory.get(Env.getCtx(), 0, 0, 13497, DisplayType.Search);
			fProductSpent = new WSearchEditor("M_ProductSpent_ID", false, false, true, productL);
			fProductSpent.setValue(MColumn.get(Env.getCtx(), 13497).getDefaultValue());
			Hlayout hProd = new Hlayout();
			hProd.setSpacing("6px");
			hProd.appendChild(fProductSpent.getComponent());
			hProd.appendChild(new Label(Msg.getMsg(Env.getCtx(), "RK_Quantity")));
			spinnerQuantity = new Doublespinner();
			spinnerQuantity.setStep(0.5);
			hProd.appendChild(spinnerQuantity);
			requestDoc.appendChild(hProd);
		}

		// ── Save button (disabled for read-only users) ─────────
		Button btnSave = (Button) dialog.getFellow("closeBtn");
		btnSave.setLabel(Msg.getMsg(Env.getCtx(), "RK_SaveAndClose"));
		btnSave.setDisabled(!canEdit);
		btnSave.addEventListener(Events.ON_CLICK, e -> {
			if (fPriority.getValue() == null) {
				Clients.showNotification(Msg.getMsg(Env.getCtx(), "RK_PriorityMandatory"));
				return;
			}
			boolean anyChange = false;
			// Message / priority (only when there's a new message or priority changed)
			if (result.getText().length() > 0 || !fPriority.getValue().equals(request.getPriority())) {
				int counter = getHighPriorityCounter(request);
				if (request.getPriority().equals("1") && counter > 0) {
					Clients.showNotification(Msg.getMsg(Env.getCtx(), "RK_HighPriorityLimit", new Object[]{counter}));
					return;
				}
				request.setPriorityUser((String) fPriority.getValue());
				request.setPriority((String) fPriority.getValue());
				if (spinnerQuantity != null && spinnerQuantity.getValue() != null && spinnerQuantity.getValue() > 0) {
					request.setM_ProductSpent_ID(Integer.valueOf(fProductSpent.getValue().toString()));
					request.setQtySpent(new BigDecimal(spinnerQuantity.getValue()));
				}
				request.addToResult(result.getText());
				anyChange = true;
			}
			// StartTime / EndTime
			Timestamp newStartTime = fStartTime.getValue() != null ? new Timestamp(fStartTime.getValue().getTime()) : null;
			Timestamp newEndTime = fEndTime.getValue() != null ? new Timestamp(fEndTime.getValue().getTime()) : null;
			if (!Objects.equals(request.getStartTime(), newStartTime) || !Objects.equals(request.getEndTime(), newEndTime)) {
				request.setStartTime(newStartTime);
				request.setEndTime(newEndTime);
				anyChange = true;
			}
			// SalesRep
			if (fSalesRep.getValue() != null && request.getSalesRep_ID() != (Integer) fSalesRep.getValue()) {
				request.setSalesRep_ID((Integer) fSalesRep.getValue());
				anyChange = true;
			}
			boolean isUpdated = anyChange && request.save();
			dialog.detach();
			if (isUpdated) refreshData();
		});

		Button btnZoom = (Button) dialog.getFellow("zoomBtn");
		btnZoom.setLabel(Msg.getMsg(Env.getCtx(), "RK_Zoom"));
		btnZoom.addEventListener(Events.ON_CLICK, e -> {
			IDesktop desk = SessionManager.getAppDesktop();
			MQuery query = new MQuery(MRequest.Table_ID);
			query.addRestriction("R_Request_ID=" + request.getR_Request_ID());
			desk.showZoomWindow(201, query);
			dialog.detach();
		});
	}

	public void onDrop(DropEvent evt) {
		Listitem item = (Listitem) evt.getDragged();
		int requestId = (int) item.getAttribute("R_Request_ID");
		MRequest request = new MRequest(Env.getCtx(), requestId, null);

		if (!canEditRequest(request)) {
			refreshData(); // restore visual state
			Clients.showNotification(Msg.getMsg(Env.getCtx(), "ActionNotAllowed"),
				Clients.NOTIFICATION_TYPE_WARNING, null, null, 3000);
			return;
		}

		if (evt.getTarget() instanceof Listbox) {
			evt.getTarget().appendChild(item);
		} else if (evt.getTarget() instanceof Hlayout) {
			Listbox targetListbox = (Listbox) evt.getTarget().getNextSibling();
			targetListbox.appendChild(item);
		} else if (evt.getTarget() instanceof Vlayout) {
			Listbox targetListbox = (Listbox) evt.getTarget().getChildren().get(1);
			targetListbox.appendChild(item);
		}
		request.setR_Status_ID((int) item.getListbox().getAttribute("R_Status_ID"));
		request.save();
		refreshData();
	}

	private void createNewRequestForm(Event event) {
		MLookup bpL = MLookupFactory.get(Env.getCtx(), 0, 0, 5434, DisplayType.Search);
		fUser = new WSearchEditor("AD_User_ID", false, false, true, bpL);
		fUser.setValue(Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")));
		fUser.setMandatory(true);

		MLookup userPriorityL = MLookupFactory.get(Env.getCtx(), 0, 0, 5426, DisplayType.List);
		fPriority = new WTableDirEditor("Priority", false, false, true, userPriorityL);
		fPriority.setMandatory(true);
		fPriority.setValue("5");
		if (!isSupervisor()) fPriority.setReadWrite(false);

		MLookup docL = MLookupFactory.get(Env.getCtx(), 0, 0, 7791, DisplayType.TableDir);
		fDoc = new WTableDirEditor("R_RequestType_ID", false, false, true, docL);
		fDoc.setMandatory(true);
		fDoc.addValueChangeListener(this);
		
		MLookup departL = MLookupFactory.get(Env.getCtx(), 0, 0, 54792, DisplayType.TableDir);
		fDepart = new WTableDirEditor("HR_Department_ID", false, false, true, departL);
		fDepart.setMandatory(true);

		MLookup srL = MLookupFactory.get(Env.getCtx(), 0, 0, 5432, DisplayType.Search);
		fSalesRep = new WSearchEditor("SalesRep_ID", false, false, true, srL);
		fSalesRep.setMandatory(true);

		MLookup roleL = MLookupFactory.get(Env.getCtx(), 0, 0, 13488, DisplayType.Search);
		fRole = new WSearchEditor("AD_Role_ID", false, false, true, roleL);
		fRole.setValue(1000088);

		summary = new Textbox();
		summary.setRows(5);
		summary.setMultiline(true);
		summary.setHflex("1");
		summary.setId("summary");

		currentRequest = new MRequest(Env.getCtx(), 0, null);

		winNewRequest = (Window) Executions.createComponents("/zul/request-new.zul", root, null);
		winNewRequest.setTitle(Msg.getMsg(Env.getCtx(), "RK_RequestFormTitle"));

		Div requestDoc = (Div) winNewRequest.getFellow("requestDoc");
		Grid grid = new Grid();
		Rows rows = new Rows();
		
		addRow(rows, "RK_Requester", fUser.getComponent());
		addRow(rows, "RK_Priority", fPriority.getComponent());
		addRow(rows, "RK_Department", fDepart.getComponent());
		addRow(rows, "RK_Type", fDoc.getComponent());
		addRow(rows, "RK_SalesRep", fSalesRep.getComponent());
		addRow(rows, "RK_ResponsibleRole", fRole.getComponent());
		
		Row rowSumLabel = new Row();
		Cell sumLabelCell = new Cell();
		sumLabelCell.setColspan(2);
		sumLabelCell.appendChild(new Label(Msg.getMsg(Env.getCtx(), "RK_SummaryLabel")));
		rowSumLabel.appendChild(sumLabelCell);
		rows.appendChild(rowSumLabel);

		Row rowSum = new Row();
		Cell sumCell = new Cell();
		sumCell.setColspan(2);
		sumCell.appendChild(summary);
		rowSum.appendChild(sumCell);
		rows.appendChild(rowSum);

		Row rowHint = new Row();
		Cell hintCell = new Cell();
		hintCell.setColspan(2);
		hintCell.appendChild(new Label(Msg.getMsg(Env.getCtx(), "RK_AttachmentHint")));
		rowHint.appendChild(hintCell);
		rows.appendChild(rowHint);

		grid.appendChild(rows);
		requestDoc.appendChild(grid);
		winNewRequest.doModal();

		Button btnSave = (Button) winNewRequest.getFellow("btnSave");
		btnSave.setLabel(Msg.getMsg(Env.getCtx(), "RK_SaveAndClose"));
		btnSave.addEventListener(Events.ON_CLICK, e -> {
			if (fUser.getValue() == null) {
				Clients.showNotification(Msg.getMsg(Env.getCtx(), "RK_RequesterMandatory"));
				return;
			}
			if (fPriority.getValue() == null) {
				Clients.showNotification(Msg.getMsg(Env.getCtx(), "RK_PriorityMandatory"));
				return;
			}
			if (fDoc.getValue() == null) {
				Clients.showNotification(Msg.getMsg(Env.getCtx(), "RK_TypeMandatory"));
				return;
			}
			if (summary.getText() == null || summary.getText().length() < 5) {
				Clients.showNotification(Msg.getMsg(Env.getCtx(), "RK_SummaryMandatory"));
				return;
			}

			currentRequest.setAD_User_ID((int) fUser.getValue());
			currentRequest.setPriorityUser((String) fPriority.getValue());
			currentRequest.setPriority((String) fPriority.getValue());
			currentRequest.setR_RequestType_ID((int) fDoc.getValue());
			currentRequest.setAD_Role_ID((int) fRole.getValue());
			currentRequest.setSalesRep_ID((int) fSalesRep.getValue());
			currentRequest.set_ValueOfColumn("HR_Department_ID", fDepart.getValue());
			currentRequest.setSummary(summary.getText());

			if (currentRequest.save()) {
				winNewRequest.detach();
				refreshData();
			} else {
				Clients.showNotification(Msg.getMsg(Env.getCtx(), "RK_SaveError"));
			}
		});

		Button btnCancel = (Button) winNewRequest.getFellow("btnCancel");
		btnCancel.setLabel(Msg.getMsg(Env.getCtx(), "RK_Cancel"));
		btnCancel.addEventListener(Events.ON_CLICK, e -> winNewRequest.detach());
	}

	private void addRow(Rows rows, String msgKey, Component comp) {
		Row row = new Row();
		row.appendChild(new Label(Msg.getMsg(Env.getCtx(), msgKey)));
		row.appendChild(comp);
		rows.appendChild(row);
	}

	private void addDialogRow(Rows rows, String labelText, Component field) {
		Row row = new Row();
		Label lbl = new Label(labelText);
		lbl.setStyle("font-weight: 600; color: #555; font-size: 12px;");
		row.appendChild(lbl);
		row.appendChild(field);
		rows.appendChild(row);
	}

	private MStatus[] getRequestStatus() {
		StringBuilder whereClauseFinal = new StringBuilder(MStatus.COLUMNNAME_IsFinalClose +"='N'");
		whereClauseFinal.append(" AND AD_Client_ID = ?");
		whereClauseFinal.append(" AND IsActive = 'Y' ");

		List<MStatus> list = new Query(Env.getCtx(), MStatus.Table_Name, whereClauseFinal.toString(), null)
										.setParameters( Integer.valueOf( (String)Env.getCtx().getProperty("#AD_Client_ID")))
										.setOrderBy(MStatus.COLUMNNAME_SeqNo)
										.list();
		return list.toArray(new MStatus[list.size()]);	
	}

	private boolean isSupervisor() {
		String sql = "select count(*) from ad_user where supervisor_id = ? ";
		int counter = DB.getSQLValue(null, sql, Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")));
		return counter > 0;
	}

	private List<Integer> getSubordinateIds(int supervisorId) {
		List<Integer> result = new ArrayList<>();
		String sql = "WITH RECURSIVE sub AS (" +
			"  SELECT ad_user_id FROM ad_user WHERE supervisor_id = ? " +
			"  UNION " +
			"  SELECT e.ad_user_id FROM ad_user e JOIN sub s ON s.ad_user_id = e.supervisor_id" +
			") SELECT ad_user_id FROM sub";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, supervisorId);
			rs = pstmt.executeQuery();
			while (rs.next()) result.add(rs.getInt(1));
		} catch (Exception e) {
			logger.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
		}
		return result;
	}

	private boolean isMySubordinate(int ad_User_ID) {
		int myId = Integer.parseInt(Env.getCtx().getProperty("#AD_User_ID"));
		return getSubordinateIds(myId).contains(ad_User_ID);
	}

	private boolean canEditRequest(MRequest request) {
		int userId = Integer.parseInt(Env.getCtx().getProperty("#AD_User_ID"));
		return request.getSalesRep_ID() == userId
			|| request.getAD_User_ID() == userId;
	}

	protected int getHighPriorityCounter(MRequest currentRequest2) {
		int myId = Integer.parseInt(Env.getCtx().getProperty("#AD_User_ID"));
		List<Integer> subs = getSubordinateIds(myId);
		// Always include the supervisor (myId) even when they have no subordinates
		String userFilter;
		if (subs.isEmpty()) {
			userFilter = "ad_user_id = " + myId;
		} else {
			String inClause = subs.stream().map(String::valueOf).collect(Collectors.joining(","));
			// inClause: Java int values from typed ResultSet; myId: Integer.parseInt result — neither is user input
			userFilter = "ad_user_id IN (" + inClause + ") OR ad_user_id = " + myId;
		}
		String sql = "SELECT count(*) FROM r_request " +
			"WHERE (" + userFilter + ") " +
			"AND EXISTS (SELECT 1 FROM R_Status WHERE R_Status_ID = r_request.R_Status_ID AND value IN (" + statusConfig.getActiveStatusesInClause() + ")) " +
			"AND priority = '1' AND r_request_id != ?";
		return DB.getSQLValue(null, sql, currentRequest2.getR_Request_ID());
	}

	@Override
	public void valueChange(ValueChangeEvent event) {
		if(event.getPropertyName().equals("R_RequestType_ID"))
		{
			int salesrep_id = getSalesRepByRequestType((int)event.getNewValue());
			if(salesrep_id > 0)
			fSalesRep.setValue(salesrep_id);
		}
	}

	private int getSalesRepByRequestType(int R_RequestType_ID) {
		String sql = "select ad_user_id From R_RequestTypeUpdates Where R_RequestType_ID = ?";
		return DB.getSQLValue(null, sql, R_RequestType_ID);
	}

	protected boolean isAttachment(int r_Request_ID) {
		String sql = "select count(*) from ad_attachment where ad_table_id = 417 and record_id = ?";
		return DB.getSQLValue(null, sql, r_Request_ID) > 0;
	}

	@Override
	public void refresh(ServerPushTemplate template) {}

	@Override
	public void updateUI() {}

	@Override
	public boolean isPooling() { return true; }

	@Override
	public void onEvent(Event event) throws Exception {}
	
	public RequestKanbanDashboard()
	{
		super();
		initForm();
	}
}
