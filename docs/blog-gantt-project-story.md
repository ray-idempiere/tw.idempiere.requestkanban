# 從一條 Scrollbar 開始的甘特圖旅程

> 寫於 2026-03-28

---

## 起源：一個看似簡單的抱怨

那天下午，有人在內部群組丟了一句話：

> 「Gantt 的 event 太多了，要一直捲整個瀏覽器視窗才看得到下面的，可以只捲那個區塊嗎？」

這是一個再普通不過的 UX 需求——讓面板內部出現 scrollbar，不要讓整個頁面跟著動。

但就是這句話，打開了一道門。

---

## 第一道牆：ZK 的 `<span>` 陷阱

我們的 iDempiere 看板是用 ZK 9 框架搭的。表面上，只要在 Gantt 的容器元件加一行 `overflow: auto` 就行了。

實際上，花了好幾個小時才找到問題所在。

ZK 9 的 `Html` 元件，渲染出來是 `<span>`。而 `<span>` 是 inline 元素——HTML5 規範不允許 block-level 的 `<div>`、`<table>` 放在 inline 元素裡面。瀏覽器的 HTML parser 會默默把內容**搬到 span 外面**，讓那個加了 `overflow: auto` 的容器變成空殼，`scrollHeight` 是 0，完全失效。

打開 DevTools，在 console 裡追蹤祖先元素的 `scrollHeight`，才終於抓到那個空殼的 span。

解法不複雜，但需要繞開框架：把 `Html` 換成 `Div`，再用 `Clients.evalJavaScript()` 把 HTML 內容注入 `innerHTML`。高度則用 `requestAnimationFrame` 等瀏覽器做完一次 layout reflow 後，才去量 `getBoundingClientRect().top`，動態算出剩餘視窗空間再填上去。

這不是優雅的解法，但是在 iDempiere dashboard 的 `height: auto` 容器環境下，這是唯一有效的路。

---

## 一旦能捲動，就想看更多

scrollbar 問題解決後，隨之而來的是另一個念頭：

> 「既然已經可以看了，為什麼不讓它更好看？」

Gantt 的 bar 原本顏色單調、沒有層次感。於是我們做了一次視覺升級：

- 暖色系主題（Open=藍、Processing=琥珀、Verify=綠、Problem=紅、Closed=灰）
- 表頭固定（`position: sticky; top: 0`），捲動時日期始終看得到
- 左側的 Document No 欄位也固定（`position: sticky; left: 0`），讓你在大量 task 之間捲動時，還是知道每一列對應哪一張單
- `border-collapse: separate` 搭配 `box-shadow` 模擬格線，讓 sticky 不會因為 border-collapse 衝突而消失

這些細節，放在畫面上不起眼，但少了任何一個，閱讀體驗就會差一截。

---

## 然後，出現了真正有趣的部分

有人問：

> 「我們有些 Request 是跨部門合作的。能不能在 Gantt 上看出哪些 task 屬於同一個專案？」

這個需求，把 Gantt 從「工具」升級成了「專案看板」。

我們在 Gantt 視圖的左側加了一個專案面板：

```
[ + New Project ]
📁 官網改版 2026
📁 ERP 升級 Q2
📁 (Unassigned)
```

每個專案可以從這裡建立，也可以直接點進去開 iDempiere 的 C_Project 視窗。

更直覺的部分是：**拖曳**。

把 Gantt 上任何一列的 Document No 欄位，拖到左側的專案名稱上，這張 Request 就被歸入了那個專案。拖到「Unassigned」就解除關聯。整個操作不需要離開看板、不需要開表單。

Gantt 主體也會依專案分組，折疊顯示，讓跨部門的全局進度一眼掌握。

---

## 最後加上的那一筆：呼吸的邊框

開發過程的最後，有人提到：

> 「如果卡片是我自己提的，可以讓它有點不一樣嗎？」

Kanban 卡片加了一條藍色左邊框，然後又問：

> 「CSS 可以做閃爍效果嗎？」

可以。不過「閃爍」太刺激，我們改成了「呼吸」——`@keyframes rkBorderPulse`，邊框顏色從實藍慢慢淡到幾乎透明，再回來，5 秒一個循環，安靜地提示你：這是你自己的案子。

Gantt 視圖後來也套上了同樣的效果。你的名字在哪一列，那一列的左邊框就會輕輕地呼吸著。

---

## 應用場景

這個 Gantt view 不是用來取代專業的專案管理工具，它的定位是：

**讓使用 iDempiere 的中小型團隊，不需要離開 ERP，就能用甘特圖的方式 review 跨部門請求的進度。**

特別適合幾種情境：

### 主管的週例會
打開 Gantt，選「本月」，切換到「All」scope，看所有部門正在進行中的請求排在哪幾週。有沒有撞期？有沒有某個時間點任務特別密集？五分鐘內就能掃完。

### 跨部門專案
把相關的 Request 拖進同一個 `C_Project`，Gantt 就會分組顯示。IT、行銷、業務各自的子任務，在同一個時間軸上對齊，不用開三個不同的系統。

### 申請人追蹤自己的案件
切換到「Private」scope，看自己提的所有請求。藍色呼吸的邊框告訴你哪些是自己的，一眼就找到。

### 回顧已結案的任務
勾選「Display Finalized」，讓已關閉的 Request 也出現在 Gantt 上。可以看這個月實際完成了哪些事、花了多少時間。

---

## 小結

這兩天的開發，從一個 scrollbar 的小抱怨出發，最後長成了一個有視覺層次、支援專案分組、支援拖曳歸類、還會為你呼吸的甘特看板。

技術面的挑戰不算小（ZK span、vflex 的限制、sticky 的 z-index 戰爭），但更有趣的是：**每解決一個問題，就會看見下一個更值得解決的問題**。

這大概就是做工具軟體最讓人著迷的地方吧。
