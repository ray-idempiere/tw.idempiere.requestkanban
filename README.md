🌐 English Version README.md
tw.idempiere.requestkanban: iDempiere Request Kanban Dashboard
An OSGi module for the iDempiere ERP system that provides a visual Kanban interface for managing and tracking Requests.

✨ Project Overview
The tw.idempiere.requestkanban module aims to provide iDempiere users with an intuitive, visual interface for request management. Utilizing the Kanban methodology, users can easily track the entire lifecycle of a request from creation to completion, thereby improving work efficiency and team collaboration.

🚀 Features
Request Visualization: Displays all pending requests as movable cards for quick comprehension.

Kanban Mode: Supports drag-and-drop operations, allowing users to easily change the status or stage of a request.

iDempiere Integration: Directly and seamlessly integrates with iDempiere's core Request system.

Clean Dashboard: Provides a full-screen dashboard interface with a green background, focusing purely on task management.

🛠️ Technical Details
Project Startup File (ZUL)
The core interface of this project is managed via Dashboard Content and points to the following ZUL file for launching:

Path: /theme/zen/dashboard/request-kanban.zul

ZUL File Content (XML Snippet):

XML

<zk xmlns:n="native">
 <window id="requestkanban" border="none" width="100%" height="100%" 
          style="background-color:green"  
          use="tw.idempiere.requestkanban.dashboard.RequestKanbanDashboard">
  </window>
</zk>
Key Class
Controller Class: tw.idempiere.requestkanban.dashboard.RequestKanbanDashboard

This class serves as the backend controller (Composer) for the ZUL file, responsible for handling business logic, loading data from the iDempiere database, and managing interactive events on the Kanban board.

⚙️ Installation and Usage
Prerequisites
iDempiere server is installed and configured.

Familiarity with deploying iDempiere OSGi modules.

Installation Steps
Compile this project into an OSGi Bundle (JAR file).

Deploy the compiled JAR file to the iDempiere server's felix/bundle or the designated plugin directory.

Ensure the tw.idempiere.requestkanban Bundle is started within the iDempiere System Admin interface.

Usage
Log in to iDempiere.

In the System Tenant, configure a new Dashboard Content entry and point it to /theme/zen/dashboard/request-kanban.zul.

Link this Dashboard Content to an appropriate Menu Item.

Click the menu item to access the Request Kanban Dashboard.

🤝 Contributing
Contributions in any form are welcome, including but not limited to bug reports, feature suggestions, or code submissions (Pull Requests).

📜 License
This project is licensed under GPL.


🌐 中文版 README.md
tw.idempiere.requestkanban：iDempiere 請求看板儀表板
一個用於 iDempiere ERP 系統的 OSGi 模組，提供一個可視化的看板（Kanban）介面來管理和追蹤請求（Requests）。

✨ 專案簡介
tw.idempiere.requestkanban 模組旨在為 iDempiere 用戶提供一個直觀、可視化的請求管理介面。透過 Kanban 模式，使用者可以輕鬆地追蹤請求從建立到完成的整個生命週期，從而提高工作效率和團隊協作能力。

🚀 功能特色
請求視覺化：以卡片形式展示所有待處理的請求，一目了然。

看板模式（Kanban）：支援拖放操作，使用者可以輕鬆地變更請求的狀態或階段。

整合 iDempiere：直接與 iDempiere 的核心請求（Request）系統無縫整合。

簡潔儀表板：提供一個全屏的綠色背景儀表板介面，專注於任務管理。

🛠️ 技術細節
專案啟動檔案 (ZUL)
本專案的核心介面是透過Dashboard Content 管理並指向以下 ZUL 檔案來啟動：

路徑: /theme/zen/dashboard/request-kanban.zul

ZUL 檔案內容 (XML 程式碼片段):

XML

<zk xmlns:n="native">
 <window id="requestkanban" border="none" width="100%" height="100%" 
          style="background-color:green"  
          use="tw.idempiere.requestkanban.dashboard.RequestKanbanDashboard">
  </window>
</zk>
關鍵類別
Controller 類別: tw.idempiere.requestkanban.dashboard.RequestKanbanDashboard

此類別是 ZUL 檔案的後端控制器（Composer），負責處理業務邏輯、從 iDempiere 資料庫載入資料、以及處理看板上的互動事件。

⚙️ 如何安裝與使用
前置條件
已安裝並配置 iDempiere 伺服器。

熟悉 iDempiere OSGi 模組的部署方式。

安裝步驟
將此專案編譯成 OSGi Bundle (JAR 檔)。

將編譯好的 JAR 檔部署到 iDempiere 伺服器的 felix/bundle 或指定的插件目錄。

在 iDempiere 系統管理介面中，確保 tw.idempiere.requestkanban Bundle 已啟動。

使用方法
登入 iDempiere。

在 System Tenant 中，透過 Dashboard Content 視窗配置一個新的儀表板內容，並將其指向 /theme/zen/dashboard/request-kanban.zul。

將此 Dashboard Content 連結到適當的選單項目。

點擊該選單即可進入請求看板儀表板。

🤝 貢獻
歡迎任何形式的貢獻，包括但不限於 Bug 回報、功能建議或程式碼提交 (Pull Requests)。

📜 授權
本專案採用 GPL 授權。