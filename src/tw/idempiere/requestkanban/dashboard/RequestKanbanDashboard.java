/******************************************************************************
 * Copyright (C) 2008 Elaine Tan                                              *
 * Copyright (C) 2008 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package tw.idempiere.requestkanban.dashboard;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.dashboard.DPActivitiesModel;
import org.adempiere.webui.dashboard.DashboardPanel;
import org.adempiere.webui.desktop.IDesktop;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ServerPushTemplate;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MQuery;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestUpdate;
import org.compiere.model.MStatus;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.model.X_AD_Column;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublespinner;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Script;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Space;
import org.zkoss.zul.Spinner;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

/**
 * Dashboard item: Workflow activities, notices and requests
 * @author Elaine
 * @date November 20, 2008
 * 
 * Contributors:
 * CarlosRuiz - globalqss - Add unprocessed button to iDempiere
 * 
 * 
 * Contributors: 
 * Deepak Pansheriya - showing only notes message
 */
public class RequestKanbanDashboard extends DashboardPanel implements EventListener<Event> ,ValueChangeListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3787249181565314148L;

	@SuppressWarnings("unused")
	private static final CLogger logger = CLogger.getCLogger(RequestKanbanDashboard.class);
	Radiogroup scope;
	Hlayout boxes;
	Hbox hbox;
	Window dialog;
	Window winNewRequest;
	MRequest currentRequest;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");    
	Textbox result;
	MStatus[] statuses;
	Button btnAddRequest;
	 WSearchEditor fUser;
	 WSearchEditor fSalesRep;
	WTableDirEditor fDoc;
	 WSearchEditor fRole;
	 WTableDirEditor fDepart;
	Textbox summary;

	private WTableDirEditor fPriority;

	protected WSearchEditor fProductSpent;

	protected Doublespinner spinnerQuantity;
	protected void initForm() {

		hbox = new Hbox();
		Grid grid = new Grid();
		Rows rows = new Rows();
		Row row = new Row();
		Button btnNew = new Button(Msg.getMsg(Env.getCtx(),"New Request"));
		btnNew.setImage("/theme/zen/images/add-file-bw-24.png");
		btnNew.setHoverImage("/theme/zen/images/add-file-color-24.png");
		btnNew.setWidth("124px");
		btnNew.setId("btnAddRequest");
		btnNew.addEventListener(Events.ON_CLICK,new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				//Clients.showNotification(Msg.getMsg(Env.getCtx(), "Come soon..."));
//				IDesktop desk = SessionManager.getAppDesktop();
				//desk.openTask((int)event.getTarget().getAttribute("R_Request_ID"));
				
//				MQuery query = new MQuery(417) ;
//				String whereClause = "R_Request_ID= 0" ;
//				query.addRestriction(whereClause.toString());
//				desk.showZoomWindow( 201 , query);
				//desk.showWindow(201, null);
				
				createNewRequestForm( event);
			}
		});
		
		row.appendChild(btnNew);
		scope = new Radiogroup();
		scope.setId("scope");
		Radio ra1 = new Radio(Msg.getMsg(Env.getCtx(),  "Private"));
		ra1.setAttribute("Scope",  "Private");
		ra1.setRadiogroup(scope);
		ra1.setSelected(true);
		row.appendChild(ra1);
		
		Radio ra2 = new Radio(Msg.getMsg(Env.getCtx(),"Subordinates"));
		ra2.setAttribute("Scope",  "Subordinates");
		ra2.setRadiogroup(scope);
		row.appendChild(ra2);
		
		Radio ra3 = new Radio(Msg.getMsg(Env.getCtx(),"Team"));
		ra3.setAttribute("Scope",  "Team");
		ra3.setRadiogroup(scope);
		row.appendChild(ra3);
		
		if(isSupervisor())
		{
			Radio ra4 = new Radio(Msg.getMsg(Env.getCtx(),"All"));
			ra4.setAttribute("Scope",  "All");
			ra4.setRadiogroup(scope);
			row.appendChild(ra4);
		}

		//row.appendChild(scope);
		rows.appendChild(row);
		grid.appendChild(rows);
		scope.appendChild(grid);
		this.appendChild(scope);
		this.appendChild(new Separator());
		boxes = new Hlayout();
		this.appendChild(boxes);
		if(statuses == null)
			statuses = getRequestStatus(); 
			
			for (MStatus mStatus : statuses) {
				
				Listbox listbox = new Listbox();
				listbox.setId("listbox" + mStatus.getValue());
				listbox.setWidth("250px");
				listbox.setHeight("600px");
				listbox.setDroppable("true");
				listbox.setOddRowSclass("non-odd");
				listbox.setStyle("margin-right:10px");
				Listhead head = new Listhead();
				Listheader header = new Listheader(mStatus.getName());	
				header.setAttribute("Name",mStatus.getName() );
				if(mStatus.getHelp() != null && mStatus.getHelp().toLowerCase().indexOf("png")>0)
				{
					//header.setSrc(mStatus.getHelp());
				}
				head.appendChild(header);
		        listbox.appendChild(head);
		        listbox.setAttribute("R_Status_ID", mStatus.getR_Status_ID());
		        listbox.addEventListener(Events.ON_DROP, new EventListener<DropEvent>() {

						@Override
						public void onEvent(DropEvent event) throws Exception {
							onDrop(event);				
						}});
				boxes.appendChild(listbox);


			}
			scope.addEventListener(Events.ON_CHECK, new EventListener<Event>() {
				@Override
				public void onEvent(Event event) throws Exception {
					// TODO Auto-generated method stub
					clearBoxes();
			        updateBoxStatus();
					addOpenItem();
				}
			});
			 
			 addOpenItem();
			 updateBoxStatus();
	}
	private void clearBoxes() {
		List<Listbox> list = boxes.getChildren();
		for (Listbox listbox : list) {
			
			while(listbox.getItemCount() > 0) {
				listbox.removeItemAt(0);
			}
			
		}
	}
	private boolean isSupervisor() {
		String sql = "select count(*) from ad_user where supervisor_id = ? ";
		
		int counter = DB.getSQLValue(null, sql, Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")) );
		
		if(counter > 0)
			return true;
		return false;
	}
	private boolean isMySubordinate(int ad_User_ID) {
	
		String sql = " select  ?  IN  (select get_subordinates_id(?)) ";
			
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, ad_User_ID);
				pstmt.setInt(2, Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")));
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					boolean	boa = rs.getBoolean(1);
					return boa;
				}
			}
			catch (SQLException ex)
			{
				throw new AdempiereException("Unable to load production lines", ex);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}

				
		return false;
	}
	protected void initFormxx() {

		boxes = (Hlayout) getFellow("requestkanban").getFellow("boxes");
		scope = (Radiogroup) super.getFellow("scope");
		btnAddRequest = (Button ) super.getFellow("btnAddRequest");
		btnAddRequest.addEventListener(Events.ON_CLICK,new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				//Clients.showNotification(Msg.getMsg(Env.getCtx(), "Come soon..."));
				IDesktop desk = SessionManager.getAppDesktop();
				//desk.openTask((int)event.getTarget().getAttribute("R_Request_ID"));
				
				MQuery query = new MQuery(417) ;
				String whereClause = "R_Request_ID= 0" ;
				query.addRestriction(whereClause.toString());
				desk.showZoomWindow( 201 , query);
				
			}
		});
		scope.addEventListener(Events.ON_CHECK, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				clearBoxes();
		        updateBoxStatus();
				addOpenItem();
			}

			private void clearBoxes() {
				List<Listbox> list = boxes.getChildren();
				for (Listbox listbox : list) {
					
					while(listbox.getItemCount() > 0) {
						listbox.removeItemAt(0);
					}
					
				}
			}
		});
		if(statuses == null)
		statuses = getRequestStatus(); 
		
		for (MStatus mStatus : statuses) {
			
			Listbox listbox = new Listbox();
			listbox.setId("listbox" + mStatus.getValue());
			listbox.setWidth("250px");
			listbox.setHeight("600px");
			listbox.setDroppable("true");
			listbox.setOddRowSclass("non-odd");
			listbox.setStyle("margin-right:10px");
			Listhead head = new Listhead();
			Listheader header = new Listheader(mStatus.getName());	
			header.setAttribute("Name",mStatus.getName() );
			if(mStatus.getHelp() != null && mStatus.getHelp().toLowerCase().indexOf("png")>0)
			{
				//header.setSrc(mStatus.getHelp());
			}
			head.appendChild(header);
	        listbox.appendChild(head);
	        listbox.setAttribute("R_Status_ID", mStatus.getR_Status_ID());
	        listbox.addEventListener(Events.ON_DROP, new EventListener<DropEvent>() {

					@Override
					public void onEvent(DropEvent event) throws Exception {
						onDrop(event);				
					}});
			boxes.appendChild(listbox);


		}
		
		 
		 addOpenItem();
		 updateBoxStatus();
		//appendChild(alert);
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

	public void onDrop(DropEvent evt) {
		//Set selected = ((Listitem)evt.getDragged()).getListbox().getSelectedItems();
		

		
		//if(evt.getDragged() instanceof Listitem )
		Listitem item = (Listitem) evt.getDragged();
		if(evt.getTarget() instanceof Listbox)
		{
			evt.getTarget().appendChild(item); 
		}		

		int  R_Request_ID = (int) item.getAttribute("R_Request_ID");
		
		MRequest request = new MRequest(Env.getCtx(), R_Request_ID, null);
		request.setR_Status_ID((int)item.getListbox().getAttribute("R_Status_ID"));
		request.save();


        updateBoxStatus();

//		System.out.println(R_Request_ID);
//		
//		System.out.println(evt.getDragged());
//		System.out.println(  ((Listitem)evt.getDragged()).getListbox());
//		System.out.println(  ((Listitem)evt.getDragged()).getListbox().getId());
//		System.out.println("onDrop");
//		
		//then, you can handle the whole set at once
	}

	private void updateBoxStatus() {
	
		List<Listbox> list = boxes.getChildren();
		for (Listbox listbox : list) {
			
			Listheader listheader = (Listheader)  listbox.getListhead().getFirstChild();
			listheader.setLabel(listheader.getAttribute("Name") + " (" + listbox.getItemCount() + ")");
			
		}
	}

	private void addOpenItem() {
		String sql = "select StartTime,coalesce(EndTime,closedate,now()) EndTime\n"
				+ ",(select name from ad_user where ad_user_id = R_Request.salesrep_id ) Responsible "
				+ ", (select name from ad_user where ad_user_id = R_Request.ad_user_id ) Customer "
				+ ",Summary "
				+ ",DocumentNo "
				+ ",StartDate "
				+ ",R_Status_ID "
				+ ",R_Request_ID "
				+ ",Priority "
				+ " from R_Request "
				+ " where  exists (select 1 from R_Status where R_Status_ID = R_Request.R_Status_ID  and  IsFinalClose != 'Y') "
				+ "and StartDate is not null ";
		Properties	 ctx = Env.getCtx();
				String ss = "Private";
				try{
					
					ss = scope.getSelectedItem().getAttribute("Scope").toString();
				}
				catch (Exception e) {
					System.out.println(e.getMessage());
				}
		
				switch(ss){
					case "Private":
						
						sql += " and ( AD_User_ID = ? " 
								+ " OR SalesRep_ID = ? " 
								+ "  )";
						
						break;
						
					case "Subordinates":
						sql += " and ( AD_User_ID IN  (select get_subordinates_id(?))"
								+ " or SalesRep_ID in  (select get_subordinates_id(?)) ) ";
								
	
						
						break;
						
					case "Team":
						sql += " and ( AD_User_ID = ? "
						+ " OR SalesRep_ID = ? "; 			
						sql += " OR exists (select 1 from AD_User_Roles where AD_Role_ID = R_Request.AD_Role_ID  AND AD_User_ID = " + ctx.getProperty("#AD_User_ID")+" ) ";								
						sql 	+= ")";
						
						break;
						
					default:	
						sql += "and ? = ?";
				}
				
				sql += "order by  Priority ASC ,StartDate DESC";
		
		
		      
		
		


		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1,Integer.valueOf(ctx.getProperty("#AD_User_ID")) );
			pstmt.setInt(2,Integer.valueOf(ctx.getProperty("#AD_User_ID")) );
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				
				String label =  rs.getString("Summary");
				label += "\n" +  " 【" + rs.getString("Customer") + "】";
				label +=  "〖" +  rs.getString("DocumentNo")  + "〗";
				
				Listitem item = new Listitem();
				Listcell cell = new Listcell();
				Textbox tb = new Textbox(label);
				tb.setMultiline(true);
				tb.setHeight("100px");
				tb.setWidth("100%");
				tb.setReadonly(true);
				tb.setStyle("background-color: rgba(255, 255, 128, .5);");
				Label lHeader = new Label("「To: " + rs.getString("Responsible") + "」 " + sdf.format(rs.getTimestamp("StartDate")));
				
				cell.appendChild(new Label("「To: " + rs.getString("Responsible") + "」 " + sdf.format(rs.getTimestamp("StartDate"))));
				cell.appendChild(tb);
				item.appendChild(cell);
				//item.appendChild(new Textbox(label)); // not support
				item.setAttribute("R_Request_ID",rs.getInt("R_Request_ID"));
				item.setTooltip(rs.getString("Summary"));
				item.setDraggable("true");
				item.setDroppable("true");
			    boolean isApproved = isRequestApproved(rs.getInt("R_Request_ID"));
				
				if(rs.getInt("Priority") < 5)
				{
					if(isApproved)
						   item.setImage("/theme/zen/images/urgent-ok-32.png");
					else
							item.setImage("/theme/zen/images/urgent-32.png");
				}else {
					
					if(isApproved)
						item.setImage("/theme/zen/images/ask-question-ok-32.png");
					else
						item.setImage("/theme/zen/images/ask-question-32.png");
				}
				
				item.addEventListener(Events.ON_CLICK, new EventListener<MouseEvent>() {

					@Override
					public void onEvent(MouseEvent event) throws Exception {
						
						SimpleDateFormat sdfTime = new SimpleDateFormat(" yyyy-MM-dd HH:mm");    

						currentRequest = new MRequest(Env.getCtx(), (int)event.getTarget().getAttribute("R_Request_ID"),null);
						MRequestUpdate[] updates = currentRequest.getUpdates(null);
						
						 dialog = (Window)Executions.createComponents( "/theme/zen/zul/kanban/request-update.zul", event.getTarget().getParent().getParent(), null);
						Div requestDoc =  (Div)  dialog.getFellow("requestDoc");
						
						String headerString = sdf.format(currentRequest.getStartDate()); 
						headerString += " From: ";
						headerString += currentRequest.getAD_User().getName();
						headerString += " To: ";						
						headerString += currentRequest.getSalesRepName();
						Label header = new Label(headerString);
						requestDoc.appendChild(header);
						 requestDoc.appendChild(new Separator()); 
						MLookup userPriorityL  = MLookupFactory.get (Env.getCtx(), 0, 0, 5426, DisplayType.List);
				    	fPriority = new WTableDirEditor ("Priority", false, false, true, userPriorityL);
				    	fPriority.setMandatory(true);
				    	fPriority.setValue(currentRequest.getPriority());
				    	if(!isMySubordinate(currentRequest.getAD_User_ID()))
				    		fPriority.setReadWrite(false);
				    	
				    	requestDoc.appendChild(new Label("優先權: "));
				    	
				    	requestDoc.appendChild(fPriority.getComponent());
				    	

						 requestDoc.appendChild(new Separator());

						 //attachment 
						 if(isAttachment(currentRequest.getR_Request_ID()))
						 {
							 requestDoc.appendChild(new Image("/theme/zen/images/Attachment24.png"));
						 }
						 requestDoc.appendChild(new Label("Summary:"));

						 
						 requestDoc.appendChild(new Separator());
						
						 Textbox summary = new Textbox(currentRequest.getSummary());
						 summary.setMultiline(true);
						 summary.setHeight("100px");
						 summary.setWidth("100%");
						 summary.setReadonly(true);
						 summary.setStyle("background-color: rgba(255, 255, 128, .5);");
					

						 requestDoc.appendChild(summary);
						 requestDoc.appendChild(new Separator());
						 requestDoc.appendChild(new Label("Message:"));
						 result = new Textbox();
						 result.setRows(3);
						 result.setMultiline(true);
						 //result.setWidth("100%");
						 //result.setHeight("50px");
						 result.setId("result");
						 requestDoc.appendChild(result);
						// 估計時間
						 if(currentRequest.getSalesRep_ID() ==  Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")))
						 {
//								MLookup bpL = MLookupFactory.get (Env.getCtx(), form.getWindowNo(), 0, 2762, DisplayType.Search);

								MLookup productL  = MLookupFactory.get (Env.getCtx(), 0, 0, 13497, DisplayType.Search);
						    	fProductSpent = new WSearchEditor ("M_ProductSpent_ID", false, false, true, productL);
						    	System.out.print(MColumn.get(Env.getCtx(), 13497).getDefaultValue());
						    	fProductSpent.setValue(MColumn.get(Env.getCtx(), 13497).getDefaultValue());
						    	requestDoc.appendChild(new Separator());
						    	Hlayout h = new Hlayout();
						    	h.appendChild(fProductSpent.getComponent());
						    	h.appendChild(new Label("Quantity:"));
						    	spinnerQuantity = new Doublespinner();
						    	spinnerQuantity.setStep(0.5);
						    	h.appendChild(spinnerQuantity);
						   	 requestDoc.appendChild(h);
						 }
						 Listbox boxUpdates =  (Listbox)  dialog.getFellow("boxUpdates");
						
						if(updates.length == 0) {
							Listitem item = new Listitem( "沒有更新");
							boxUpdates.appendChild(item);

						}
						
						for (MRequestUpdate mRequestUpdate : updates) {
							
							Listitem item = new Listitem( );
							Listcell cell = new Listcell();
							mRequestUpdate.getUpdated();
							
							

							Image imgTalk = new Image("/theme/zen/images/talk-32.png");
							cell.appendChild(imgTalk);

							
							Label lDate = new Label(sdfTime.format(mRequestUpdate.getUpdated()));
							cell.appendChild(lDate);
							MUser user = new MUser(Env.getCtx(), mRequestUpdate.getUpdatedBy(),null);
							
//							Image imgUser = new Image("/theme/zen/images/user-32.png");
//							cell.appendChild(imgUser);
							Label lQtySpent;
							
							Label lUser = new Label( "「"+  user.getName() + "」 ");
							cell.appendChild(lUser);
							
							Label lResult = new Label(mRequestUpdate.getResult());
							cell.appendChild(lResult);
							if(mRequestUpdate.getQtySpent()!=null && mRequestUpdate.getQtySpent().floatValue()!= 0)
							{
								Label lProduct = new Label( " [" + mRequestUpdate.getM_ProductSpent().getValue() + ":");
								cell.appendChild(lProduct);
								if(mRequestUpdate.getQtySpent().floatValue() > 1)
									 lQtySpent = new Label(mRequestUpdate.getQtySpent() + " " + Msg.getMsg(Env.getCtx(), "Hours]"));
								else 
									 lQtySpent = new Label(mRequestUpdate.getQtySpent() + " " + Msg.getMsg(Env.getCtx(), "Hour]"));

								if(mRequestUpdate.getQtySpent().floatValue()!= 0)
								cell.appendChild(lQtySpent);

								
							}
								
							item.appendChild(cell);
							boxUpdates.appendChild(item);
						}
						
						 dialog.doModal();
	
						 Button close = (Button)dialog.getFellow("closeBtn");
						 close.addEventListener(Events.ON_CLICK,new  EventListener<Event>() {

							private boolean isUpdated = false;

							@Override
							public void onEvent(Event event) throws Exception {
								if(fPriority.getValue()==null)
								{	
									Clients.showNotification("優先權不能空白");
									return;
								}

								if(result.getText().length() > 0 || !fPriority.getValue().equals(currentRequest.getPriority()))
								{				
									int counter = getHighPriorityCounter(currentRequest);
									if(currentRequest.getPriority().equals("1") && counter > 0)
									{
										Clients.showNotification("已有" + counter + "筆緊急Ticket , 目前限定每位主管上限為1筆");
										return;
									}
									
									currentRequest.setPriorityUser((String) fPriority.getValue());
									currentRequest.setPriority((String) fPriority.getValue());
								
									if(spinnerQuantity != null && spinnerQuantity.getValue() != null && spinnerQuantity.getValue() > 0)
									{
										currentRequest.setM_ProductSpent_ID(Integer.valueOf(fProductSpent.getValue().toString()));
										currentRequest.setQtySpent(new BigDecimal(spinnerQuantity.getValue()));
									}
									
									currentRequest.addToResult(result.getText());
									if(currentRequest.save())
									{
										isUpdated = true;
									}
									
								}
								
								dialog.detach();
								if(isUpdated) {
									clearBoxes();
									addOpenItem();
									updateBoxStatus();
								}

							}
						});
						 Button zoom = (Button)dialog.getFellow("zoomBtn");
						 zoom.addEventListener(Events.ON_CLICK,new  EventListener<Event>() {

							@Override
							public void onEvent(Event event) throws Exception {
							
								IDesktop desk = SessionManager.getAppDesktop();
								//desk.openTask((int)event.getTarget().getAttribute("R_Request_ID"));
								
								MQuery query = new MQuery(MRequest.Table_ID) ;
								String whereClause = "R_Request_ID=" + currentRequest.getR_Request_ID() ;
								query.addRestriction(whereClause.toString());
								desk.showZoomWindow( 201 , query);	
							dialog.detach();

							}
						});

						 
					}


				});		
				item.addEventListener(Events.ON_RIGHT_CLICK, new EventListener<MouseEvent>() {

					@Override
					public void onEvent(MouseEvent event) throws Exception {

						String url = "/index.zul?Action=Zoom&AD_Table_ID=417&Record_ID=" + event.getTarget().getAttribute("R_Request_ID");
						//Executions.sendRedirect(url);
						//Window winRequest = new Window();
						IDesktop desk = SessionManager.getAppDesktop();
						//desk.openTask((int)event.getTarget().getAttribute("R_Request_ID"));
						
						MQuery query = new MQuery(417) ;
						String whereClause = "R_Request_ID=" + event.getTarget().getAttribute("R_Request_ID") ;
						query.addRestriction(whereClause.toString());
						desk.showZoomWindow( 201 , query);
						//desk.showURL(url, true);
					}
				});
				item.addEventListener(Events.ON_DROP, new EventListener<DropEvent>() {
				
					@Override
					public void onEvent(DropEvent event) throws Exception {
						/**
						 * target is Listitem
						 */
						Listitem item = (Listitem) event.getDragged();
						int  R_Request_ID = (int) item.getAttribute("R_Request_ID");
						MRequest request = new MRequest(Env.getCtx(), R_Request_ID, null);
						request.setR_Status_ID((int)event.getTarget().getParent().getAttribute("R_Status_ID"));
						request.save();
						
						
			            if (event.getTarget() instanceof Listitem) {
			            	event.getTarget().getParent().insertBefore(event.getDragged(), event.getTarget());
			            } else {
			            	event.getTarget().appendChild(event.getDragged());
			            }
			            updateBoxStatus();
			        
					}
				});
				
				
				List <Listbox> list = boxes.getChildren();
				
				for (Listbox listbox : list) {
					
				 if(	(int)listbox.getAttribute("R_Status_ID") == rs.getInt("R_Status_ID"))
					 listbox.appendChild(item);
				}				
			}
		}
		catch (SQLException ex)
		{
			throw new AdempiereException("Unable to load request items", ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		updateBoxStatus();
	}
	protected boolean isAttachment(int r_Request_ID) {
		String sql = "select count(*) from ad_attachment where ad_table_id = 417 and  record_id = ?";
		return DB.getSQLValue(null, sql, r_Request_ID) > 0;

	}
	private boolean isRequestApproved(int R_Request_ID) {
		
		String sql = "select get_request_approved_counter(?)";
		return DB.getSQLValue(null, sql, R_Request_ID) > 0;
	}
	public RequestKanbanDashboard()
	{
		super();
		initForm();
	}

	@Override
    public void refresh(ServerPushTemplate template)
	{
//		int notice = DPActivitiesModel.getNoticeCount();
//		int request = DPActivitiesModel.getRequestCount();
//		int workflow = DPActivitiesModel.getWorkflowCount();
//		int unprocessed = DPActivitiesModel.getUnprocessedCount();
//		if (noOfNotice != notice || noOfRequest != request 
//			|| noOfWorkflow != workflow || noOfUnprocessed != unprocessed )
//		{
//			noOfNotice = notice;
//			noOfRequest = request;
//			noOfWorkflow = workflow;
//			noOfUnprocessed = unprocessed;
//			template.executeAsync(this);
//		}
	}

    @Override
	public void updateUI() {
//    	btnNotice.setLabel(labelN + " : " + noOfNotice);
//		btnRequest.setLabel(labelR + " : " + noOfRequest);
//		btnWorkflow.setLabel(labelW + " : " + noOfWorkflow);
//		if (DPActivitiesModel.isShowUnprocessed()) 
//			btnUnprocessed.setLabel(labelU + " : " + noOfUnprocessed);
//		
//		EventQueue<Event> queue = EventQueues.lookup(IDesktop.ACTIVITIES_EVENT_QUEUE, true);
//		Map<String, Object> map = new HashMap<String, Object>();
//		map.put("notice", noOfNotice);
//		map.put("request", noOfRequest);
//		map.put("workflow", noOfWorkflow);
//		map.put("unprocessed", noOfUnprocessed);
//		Event event = new Event(IDesktop.ON_ACTIVITIES_CHANGED_EVENT, null, map);
//		queue.publish(event);
	}

	@Override
	public boolean isPooling() {
		return true;
	}

	public void onEvent(Event event)
    {

	}
	private void createNewRequestForm(Event event) {
		

    	
		
		
		MLookup bpL = MLookupFactory.get (Env.getCtx(), 0, 0, 5434, DisplayType.Search);
		fUser = new WSearchEditor ("AD_User_ID", false, false, true, bpL);
		fUser.setValue(Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")));
		fUser.setMandatory(true);
		
		
		MLookup userPriorityL  = MLookupFactory.get (Env.getCtx(), 0, 0, 5426, DisplayType.List);
    	fPriority = new WTableDirEditor ("Priority", false, false, true, userPriorityL);
    	fPriority.setMandatory(true);
    	fPriority.setValue("5");
	    if(!isSupervisor())
			fPriority.setReadWrite(false);
		
    	
    	
		//fUser.addValueChangeListener(this);
    	MLookup docL = MLookupFactory.get (Env.getCtx(), 0, 0, 7791, DisplayType.TableDir);
    	fDoc = new WTableDirEditor ("R_RequestType_ID", false, false, true, docL);
    	fDoc.setMandatory(true);
    	fDoc.addValueChangeListener(this);
    	MLookup departL = MLookupFactory.get (Env.getCtx(), 0, 0, 1007504, DisplayType.TableDir);
    	fDepart = new WTableDirEditor ("HR_Department_ID", false, false, true, departL);
    	fDepart.setMandatory(true);
    	
		MLookup srL = MLookupFactory.get (Env.getCtx(), 0, 0, 5432, DisplayType.Search);
		fSalesRep = new WSearchEditor ("SalesRep_ID", false, false, true, srL);
		fSalesRep.setMandatory(true);
    	
		MLookup roleL = MLookupFactory.get (Env.getCtx(), 0, 0, 13488, DisplayType.Search);
		fRole = new WSearchEditor ("AD_Role_ID", false, false, true, roleL);
		fRole.setValue(1000088);
		fSalesRep.setMandatory(true);
		
		 summary = new Textbox();
		 summary.setRows(5);
		 summary.setMultiline(true);
		 summary.setWidth("100%");
		 //summary.setHeight("80px"); // fixed for idempiere 10 11
		 summary.setId("summary");
		
    	//lBPartner.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
	
		//fUser.addValueChangeListener(this);
		
		SimpleDateFormat sdfTime = new SimpleDateFormat(" yyyy-MM-dd HH:mm");    

		currentRequest = new MRequest(Env.getCtx(), 0,null);
		MRequestUpdate[] updates = currentRequest.getUpdates(null);
		
		winNewRequest = (Window)Executions.createComponents( "/theme/zen/zul/kanban/request-new.zul", boxes , null);
		Div requestDoc =  (Div)  winNewRequest.getFellow("requestDoc");
		Grid grid = new Grid();
		Rows  rows = new Rows();
		Row  row = new Row();
		row.appendChild(new Label("申請人:"));
		row.setAlign("Right");
		row.appendChild(fUser.getComponent());
	    rows.appendChild(row);
	    
	    row = new Row();
		row.appendChild(new Label("優先:"));
		row.setAlign("Right");
		row.appendChild(fPriority.getComponent());
	    rows.appendChild(row);
	    
	    
		row = new Row();
		row.appendChild(new Label("部門:"));
		row.setAlign("Right");
		row.appendChild(fDepart.getComponent());
	    rows.appendChild(row);
	    
	    row = new Row();
		row.setAlign("Right");
		row.appendChild(new Label("類型:"));
		row.appendChild(fDoc.getComponent());
	    rows.appendChild(row);
	    
	    row = new Row();
		row.setAlign("Right");

		row.appendChild(new Label("負責人:"));
		row.appendChild(fSalesRep.getComponent());
	    rows.appendChild(row);
	    
	    row = new Row();
		row.setAlign("Right");

		row.appendChild(new Label("負責職務/團隊:"));
		row.appendChild(fRole.getComponent());
	    rows.appendChild(row);
	    
	    row = new Row();
	    //row.setSpans("2");
	    row.setAlign("Left");
		row.appendChild(new Label("請求內容說明"));
	    rows.appendChild(row);
	   
	    row = new Row();
	    //row.setSpans("2");
	    row.setAlign("Center");
		row.appendChild(summary);
	    rows.appendChild(row);
	   
	    row = new Row();
	   // row.setSpans("2");
	    row.setAlign("");
		row.appendChild(new Label("若有附件的需求，存檔後，可以該請求文件上按右鍵，跳轉到Window介面上傳附件。"));
	    rows.appendChild(row);
	    
	    
		grid.appendChild(rows);
		requestDoc.appendChild(grid);
		requestDoc.appendChild(new Separator());
		
		
		
		 winNewRequest.doModal();

		 Button btnSave = (Button)winNewRequest.getFellow("btnSave");
		 btnSave.addEventListener(Events.ON_CLICK,new  EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				/**
				 * 	 WSearchEditor fUser;
	 WSearchEditor fSalesRep;
	WTableDirEditor fDoc;
	 WSearchEditor fRole;
	 WTableDirEditor fDepart;
	Textbox summary;
				 */
				if(fUser.getValue()==null)
				{	
					Clients.showNotification("申請人未填");
					return;
				}
				currentRequest.setAD_User_ID((int)fUser.getValue());
				
				if(fPriority.getValue()==null)
				{	
					Clients.showNotification("優先權不能空白");
					return;
				}
				int counter = getHighPriorityCounter(currentRequest);
				if(counter > 0 &&  fPriority.getValue().toString().equals("1"))
				{
					Clients.showNotification("已有" + counter + "筆緊急Ticket , 目前限定每位主管上限為1筆");
					return;
				}
				
				currentRequest.setPriorityUser((String) fPriority.getValue());
				currentRequest.setPriority((String) fPriority.getValue());
				if(fDoc.getValue()==null)
				{	
					Clients.showNotification("類型未選");
					return;
				}
				currentRequest.setR_RequestType_ID((int)fDoc.getValue());
				if(fRole.getValue()==null)
				{	
					Clients.showNotification("團隊未選");
					return;
				}
				currentRequest.setAD_Role_ID((int)fRole.getValue());
				if(fSalesRep.getValue()==null)
				{	
					Clients.showNotification("負責人未選");
					return;
				}
				currentRequest.setSalesRep_ID((int)fSalesRep.getValue());
				
				if(fDepart.getValue()==null)
				{	
					Clients.showNotification("部門未選");
					return;
				}
				currentRequest.set_ValueOfColumn("HR_Department_ID", fDepart.getValue());
				
				if(summary.getText()==null || summary.getText().length() < 5)
				{
					Clients.showNotification("請求內容說明未填寫，至少5個字");
					return;
				}
				
				currentRequest.setSummary(summary.getText());
				if(currentRequest.save())
				{
					winNewRequest.detach();
					clearBoxes();
					addOpenItem();
					updateBoxStatus();
				}else {
					Clients.showNotification("存檔失敗！");
				}
				
				

			}
		});
		 Button btnCancel = (Button)winNewRequest.getFellow("btnCancel");
		 btnCancel.addEventListener(Events.ON_CLICK,new  EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				winNewRequest.detach();

			}
		});

	}
	protected int getHighPriorityCounter(MRequest currentRequest2) {
		String sql = "select count(*) from r_request where (ad_user_id in (select get_subordinates_id(?)) or ad_user_id = ? ) "
				+ "		and  exists (select 1 from R_Status where R_Status_ID = r_request.R_Status_ID and value in ('Processing','Open')  ) "
				+ "		and priority = '1' and r_request_id != ?";
		
		return DB.getSQLValue(null, sql, new Object[] {Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")) ,
				Integer.valueOf(Env.getCtx().getProperty("#AD_User_ID")) ,currentRequest2.getR_Request_ID()});
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
		String sql = "select ad_user_id From  R_RequestTypeUpdates Where R_RequestType_ID = ?";
		
		return DB.getSQLValue(null, sql, R_RequestType_ID);
	}
	
}
