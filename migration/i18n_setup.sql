DELETE FROM AD_Message_Trl WHERE AD_Message_ID IN (SELECT AD_Message_ID FROM AD_Message WHERE Value LIKE 'RK_%');
DELETE FROM AD_Message WHERE Value LIKE 'RK_%';

INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, Value, MsgText, MsgType, EntityType)
VALUES 
(2000000, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Requester', 'Requester:', 'I', 'U'),
(2000001, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Priority', 'Priority:', 'I', 'U'),
(2000002, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Department', 'Department:', 'I', 'U'),
(2000003, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Type', 'Type:', 'I', 'U'),
(2000004, 0, 0, 'Y', now(), 100, now(), 100, 'RK_SalesRep', 'Sales Rep:', 'I', 'U'),
(2000005, 0, 0, 'Y', now(), 100, now(), 100, 'RK_ResponsibleRole', 'Responsible Role/Team:', 'I', 'U'),
(2000006, 0, 0, 'Y', now(), 100, now(), 100, 'RK_SummaryLabel', 'Request Summary', 'I', 'U'),
(2000007, 0, 0, 'Y', now(), 100, now(), 100, 'RK_SaveAndClose', 'Save and Close', 'I', 'U'),
(2000008, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Cancel', 'Cancel', 'I', 'U'),
(2000009, 0, 0, 'Y', now(), 100, now(), 100, 'RK_SaveError', 'Save Failed!', 'E', 'U'),
(2000010, 0, 0, 'Y', now(), 100, now(), 100, 'RK_PriorityMandatory', 'Priority cannot be empty', 'E', 'U'),
(2000011, 0, 0, 'Y', now(), 100, now(), 100, 'RK_RequesterMandatory', 'Requester is required', 'E', 'U'),
(2000012, 0, 0, 'Y', now(), 100, now(), 100, 'RK_NoUpdates', 'No updates', 'I', 'U'),
(2000013, 0, 0, 'Y', now(), 100, now(), 100, 'RK_UpdateHistory', 'Update History', 'I', 'U'),
(2000014, 0, 0, 'Y', now(), 100, now(), 100, 'RK_RequestFormTitle', 'Request Application', 'I', 'U'),
(2000015, 0, 0, 'Y', now(), 100, now(), 100, 'RK_AttachmentHint', 'For attachments, right-click on the request after saving.', 'I', 'U'),
(2000016, 0, 0, 'Y', now(), 100, now(), 100, 'RK_HighPriorityLimit', 'Already %1 high priority ticket(s). Limit is 1 per supervisor.', 'W', 'U'),
(2000017, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Summary', 'Summary:', 'I', 'U'),
(2000018, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Message', 'Message:', 'I', 'U'),
(2000019, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Quantity', 'Quantity:', 'I', 'U'),
(2000020, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Zoom', 'Zoom', 'I', 'U'),
(2000021, 0, 0, 'Y', now(), 100, now(), 100, 'RK_TypeMandatory', 'Type is required', 'E', 'U'),
(2000022, 0, 0, 'Y', now(), 100, now(), 100, 'RK_TeamMandatory', 'Team is required', 'E', 'U'),
(2000023, 0, 0, 'Y', now(), 100, now(), 100, 'RK_SalesRepMandatory', 'Sales Rep is required', 'E', 'U'),
(2000024, 0, 0, 'Y', now(), 100, now(), 100, 'RK_DepartmentMandatory', 'Department is required', 'E', 'U'),
(2000025, 0, 0, 'Y', now(), 100, now(), 100, 'RK_SummaryMandatory', 'Summary is required (min 5 chars)', 'E', 'U'),
(2000026, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Private', 'Private', 'I', 'U'),
(2000027, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Subordinates', 'Subordinates', 'I', 'U'),
(2000028, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Team', 'Team', 'I', 'U'),
(2000029, 0, 0, 'Y', now(), 100, now(), 100, 'RK_All', 'All', 'I', 'U'),
(2000030, 0, 0, 'Y', now(), 100, now(), 100, 'RK_RequestKanban', 'Request Kanban', 'I', 'U'),
(2000031, 0, 0, 'Y', now(), 100, now(), 100, 'RK_NewRequest', 'New Request', 'I', 'U'),
(2000032, 0, 0, 'Y', now(), 100, now(), 100, 'RK_NoRequests', 'No requests in this status', 'I', 'U'),
(2000033, 0, 0, 'Y', now(), 100, now(), 100, 'RK_StartTime', 'Start Time', 'I', 'U'),
(2000034, 0, 0, 'Y', now(), 100, now(), 100, 'RK_EndTime', 'End Time', 'I', 'U'),
(2000035, 0, 0, 'Y', now(), 100, now(), 100, 'RK_BasicInfo', 'Basic Info', 'I', 'U'),
(2000036, 0, 0, 'Y', now(), 100, now(), 100, 'RK_SearchPlaceholder', '🔍 Search summary, document no…', 'I', 'U'),
(2000037, 0, 0, 'Y', now(), 100, now(), 100, 'RK_ListView', 'List View', 'I', 'U'),
(2000038, 0, 0, 'Y', now(), 100, now(), 100, 'RK_KanbanView', 'Kanban View', 'I', 'U'),
(2000039, 0, 0, 'Y', now(), 100, now(), 100, 'RK_GanttView', 'Gantt View', 'I', 'U'),
(2000040, 0, 0, 'Y', now(), 100, now(), 100, 'RK_ThisWeek', 'This Week', 'I', 'U'),
(2000041, 0, 0, 'Y', now(), 100, now(), 100, 'RK_ThisMonth', 'This Month', 'I', 'U'),
(2000042, 0, 0, 'Y', now(), 100, now(), 100, 'RK_ThisQuarter', 'This Quarter', 'I', 'U'),
(2000043, 0, 0, 'Y', now(), 100, now(), 100, 'RK_NoDateSet', 'No date set', 'I', 'U'),
(2000044, 0, 0, 'Y', now(), 100, now(), 100, 'RK_Unassigned', 'Unassigned', 'I', 'U')
,(2000045, 0, 0, 'Y', now(), 100, now(), 100, 'RK_InvalidDateRange', 'Start date must be before end date', 'E', 'U');

INSERT INTO AD_Message_Trl (AD_Message_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, MsgText, MsgTip, IsTranslated)
SELECT AD_Message_ID, 'zh_TW', 0, 0, 'Y', now(), 100, now(), 100, 
    CASE Value
        WHEN 'RK_Requester' THEN '申請人:'
        WHEN 'RK_Priority' THEN '優先:'
        WHEN 'RK_Department' THEN '部門:'
        WHEN 'RK_Type' THEN '類型:'
        WHEN 'RK_SalesRep' THEN '負責人:'
        WHEN 'RK_ResponsibleRole' THEN '負責職務/團隊:'
        WHEN 'RK_SummaryLabel' THEN '請求內容說明'
        WHEN 'RK_SaveAndClose' THEN '存檔離開'
        WHEN 'RK_Cancel' THEN '取消'
        WHEN 'RK_SaveError' THEN '存檔失敗！'
        WHEN 'RK_PriorityMandatory' THEN '優先權不能空白'
        WHEN 'RK_RequesterMandatory' THEN '申請人未填'
        WHEN 'RK_NoUpdates' THEN '沒有更新'
        WHEN 'RK_UpdateHistory' THEN '更新歷史'
        WHEN 'RK_RequestFormTitle' THEN '請求申請 (Request Form)'
        WHEN 'RK_AttachmentHint' THEN '若有附件的需求，存檔後，可以在該請求文件上按右鍵，跳轉到 Window 介面上傳附件。'
        WHEN 'RK_HighPriorityLimit' THEN '已有 %1 筆緊急Ticket，目前限定每位主管上限為 1 筆'
        WHEN 'RK_Summary' THEN 'Summary:'
        WHEN 'RK_Message' THEN 'Message:'
        WHEN 'RK_Quantity' THEN '數量:'
        WHEN 'RK_Zoom' THEN '縮放'
        WHEN 'RK_TypeMandatory' THEN '類型未選'
        WHEN 'RK_TeamMandatory' THEN '團隊未選'
        WHEN 'RK_SalesRepMandatory' THEN '負責人未選'
        WHEN 'RK_DepartmentMandatory' THEN '部門未選'
        WHEN 'RK_SummaryMandatory' THEN '請求內容說明未填寫，至少 5 個字'
        WHEN 'RK_Private' THEN '個人'
        WHEN 'RK_Subordinates' THEN '部屬'
        WHEN 'RK_Team' THEN '團隊'
        WHEN 'RK_All' THEN '全部'
        WHEN 'RK_RequestKanban' THEN '請求看板'
        WHEN 'RK_NewRequest' THEN '新增請求'
        WHEN 'RK_NoRequests' THEN '此狀態目前沒有請求'
        WHEN 'RK_StartTime' THEN '開始時間'
        WHEN 'RK_EndTime' THEN '結束時間'
        WHEN 'RK_BasicInfo' THEN '基本資訊'
        WHEN 'RK_SearchPlaceholder' THEN '🔍 搜尋摘要、單號…'
        WHEN 'RK_ListView' THEN '列表視圖'
        WHEN 'RK_KanbanView' THEN '看板視圖'
        WHEN 'RK_GanttView' THEN '甘特視圖'
        WHEN 'RK_ThisWeek' THEN '本週'
        WHEN 'RK_ThisMonth' THEN '本月'
        WHEN 'RK_ThisQuarter' THEN '本季'
        WHEN 'RK_NoDateSet' THEN '未設定時間'
        WHEN 'RK_Unassigned' THEN '未指派'
        WHEN 'RK_InvalidDateRange' THEN '開始日期必須早於結束日期'
    END, NULL, 'Y'
FROM AD_Message WHERE Value LIKE 'RK_%';
