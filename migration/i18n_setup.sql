DELETE FROM AD_Message_Trl WHERE AD_Message_ID IN (SELECT AD_Message_ID FROM AD_Message WHERE Value LIKE 'RK_%');
DELETE FROM AD_Message WHERE Value LIKE 'RK_%';

INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, Value, MsgText, MsgType, EntityType)
SELECT nextid('AD_Message'::varchar, 'N'::varchar), 0, 0, 'Y', now(), 100, now(), 100, v.val, v.msg, v.type, 'U'
FROM (VALUES 
    ('RK_Requester', 'Requester:', 'I'),
    ('RK_Priority', 'Priority:', 'I'),
    ('RK_Department', 'Department:', 'I'),
    ('RK_Type', 'Type:', 'I'),
    ('RK_SalesRep', 'Sales Rep:', 'I'),
    ('RK_ResponsibleRole', 'Responsible Role/Team:', 'I'),
    ('RK_SummaryLabel', 'Request Summary', 'I'),
    ('RK_SaveAndClose', 'Save and Close', 'I'),
    ('RK_Cancel', 'Cancel', 'I'),
    ('RK_SaveError', 'Save Failed!', 'E'),
    ('RK_PriorityMandatory', 'Priority cannot be empty', 'E'),
    ('RK_RequesterMandatory', 'Requester is required', 'E'),
    ('RK_NoUpdates', 'No updates', 'I'),
    ('RK_UpdateHistory', 'Update History', 'I'),
    ('RK_RequestFormTitle', 'Request Application', 'I'),
    ('RK_AttachmentHint', 'For attachments, right-click on the request after saving.', 'I'),
    ('RK_HighPriorityLimit', 'Already %1 high priority ticket(s). Limit is 1 per supervisor.', 'W'),
    ('RK_Summary', 'Summary:', 'I'),
    ('RK_Message', 'Message:', 'I'),
    ('RK_Quantity', 'Quantity:', 'I'),
    ('RK_Zoom', 'Zoom', 'I'),
    ('RK_TypeMandatory', 'Type is required', 'E'),
    ('RK_TeamMandatory', 'Team is required', 'E'),
    ('RK_SalesRepMandatory', 'Sales Rep is required', 'E'),
    ('RK_DepartmentMandatory', 'Department is required', 'E'),
    ('RK_SummaryMandatory', 'Summary is required (min 5 chars)', 'E'),
    ('RK_Private', 'Private', 'I'),
    ('RK_Subordinates', 'Subordinates', 'I'),
    ('RK_Team', 'Team', 'I'),
    ('RK_All', 'All', 'I'),
    ('RK_RequestKanban', 'Request Kanban', 'I'),
    ('RK_NewRequest', 'New Request', 'I'),
    ('RK_NoRequests', 'No requests in this status', 'I'),
    ('RK_StartTime', 'Start Time', 'I'),
    ('RK_EndTime', 'End Time', 'I'),
    ('RK_BasicInfo', 'Basic Info', 'I'),
    ('RK_SearchPlaceholder', '🔍 Search summary, document no…', 'I'),
    ('RK_ListView', 'List View', 'I'),
    ('RK_KanbanView', 'Kanban View', 'I'),
    ('RK_GanttView', 'Gantt View', 'I'),
    ('RK_ThisWeek', 'This Week', 'I'),
    ('RK_ThisMonth', 'This Month', 'I'),
    ('RK_ThisQuarter', 'This Quarter', 'I'),
    ('RK_NoDateSet', 'No date set', 'I'),
    ('RK_Unassigned', '(未分配)', 'I'),
    ('RK_InvalidDateRange', 'Start date must be before end date', 'E'),
    ('RK_DisplayFinalClose', 'Display Final Close', 'I'),
    ('RK_Projects',              '專案',                   'I'),
    ('RK_NewProject',            '+ 新增專案',              'I'),
    ('RK_ProjectName',           '專案名稱',                'I'),
    ('RK_ProjectStart',          '開始日期',                'I'),
    ('RK_ProjectEnd',            '結束日期',                'I'),
    ('RK_ProjectNameMandatory',  'Project name is required', 'E'),
    ('RK_ProjectSaveError',      'Failed to save project',   'E'),
    ('RK_NoProjects',            '(尚無專案)',                 'I'),
    ('RK_RequestNotFound',       'Request not found',         'E')
) AS v(val, msg, type);

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
        WHEN 'RK_Unassigned' THEN '(未分配)'
        WHEN 'RK_InvalidDateRange' THEN '開始日期必須早於結束日期'
        WHEN 'RK_DisplayFinalClose' THEN '顯示最終關閉'
        WHEN 'RK_Projects' THEN '專案'
        WHEN 'RK_NewProject' THEN '+ 新增專案'
        WHEN 'RK_ProjectName' THEN '專案名稱'
        WHEN 'RK_ProjectStart' THEN '開始日期'
        WHEN 'RK_ProjectEnd' THEN '結束日期'
        WHEN 'RK_ProjectNameMandatory' THEN 'Project name is required'
        WHEN 'RK_ProjectSaveError' THEN 'Failed to save project'
        WHEN 'RK_NoProjects' THEN '(尚無專案)'
        WHEN 'RK_RequestNotFound' THEN 'Request not found'
    END, NULL, 'Y'
FROM AD_Message WHERE Value LIKE 'RK_%';
