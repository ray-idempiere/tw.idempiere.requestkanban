✨ 專案簡介
tw.idempiere.requestkanban 模組旨在為 iDempiere 用戶提供一個直觀、可視化的請求管理介面。透過 Kanban 模式，使用者可以輕鬆地追蹤請求從建立到完成的整個生命週期，提高工作效率和協作能力。

🚀 功能特色
請求視覺化：以卡片形式展示所有待處理的請求。

看板模式：支援拖放操作，輕鬆變更請求的狀態或階段。

整合 iDempiere：直接與 iDempiere 的請求（Request）系統整合。

簡潔儀表板：提供一個全屏的綠色背景儀表板介面，專注於任務管理。

🛠️ 技術細節
專案啟動檔案 (ZUL)
本專案的核心介面是透過Dashboard Content 管理以下 ZUL 檔案啟動：

路徑: /theme/zen/dashboard/request-kanban.zul

ZUL 檔案內容
XML

<zk xmlns:n="native">
 <window id="requestkanban" border="none" width="100%" height="100%" 
          style="background-color:green"  
          use="tw.idempiere.requestkanban.dashboard.RequestKanbanDashboard">
  </window>
</zk>
關鍵類別
Controller 類別: tw.idempiere.requestkanban.dashboard.RequestKanbanDashboard

此類別是 ZUL 檔案的後端控制器（Composer），負責處理業務邏輯、資料載入、以及看板上的互動事件。

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

System Tenant中,  Dashboard Content中配置並指向 /theme/zen/dashboard/request-kanban.zul。

點擊該選單即可進入請求看板儀表板。

🤝 貢獻
歡迎任何形式的貢獻，包括但不限於 Bug 回報、功能建議或程式碼提交 (Pull Requests)。

📜 授權
GPL