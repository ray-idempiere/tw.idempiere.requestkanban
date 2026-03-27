# ZK Gantt 面板捲動實作筆記

**日期：** 2026-03-27
**問題：** Gantt 面板沒有自己的 scrollbar，整個瀏覽器視窗一起捲動
**結論：** ZK 9 的 `Html` component 會渲染為 `<span>`，block 內容會被 HTML parser 移出去，導致 `overflow:auto` 完全失效

---

## 問題根源

### ZK 9 Html component = `<span>`

ZK 9 的 `Html` component 渲染出來的 DOM 是：

```html
<span id="z_abc123" style="overflow: auto;">
  <!-- 內容被注入這裡 -->
</span>
```

如果用 `ganttHtml.setContent(html)` 注入含有 `<div>` 或 `<table>` 的 HTML：

```html
<span id="z_abc123">
  <div style="min-width:600px;">      ← block element inside inline element
    <table>...</table>
    <div>legend</div>
  </div>
</span>
```

HTML5 規範不允許 block-level element 放在 inline element 裡面。瀏覽器的 HTML parser 會**自動把 div/table 移到 span 外面**：

```html
<span id="z_abc123"></span>          ← span 變空的！
<div style="min-width:600px;">       ← 內容跑到外面，脫離 span 管控
  <table>...</table>
  <div>legend</div>
</div>
```

**結果：**
- `span#uuid` 的 `scrollHeight = 0`（空的）
- Gantt 內容在 span 外面渲染，沒有任何 `overflow:auto` 包住它
- 頁面高度因為 Gantt 內容而增加，整個視窗捲動

### 診斷方式（browser console）

```js
// 找到 overflow:auto 的祖先元素
var t = Array.from(document.querySelectorAll('table'))
  .find(t => t.style.borderCollapse === 'separate');
var p = t.parentElement;
while (p && p.style.overflow !== 'auto') p = p.parentElement;

console.log('id:', p.id);
console.log('tagName:', p.tagName);       // SPAN ← 這就是問題
console.log('scrollHeight:', p.scrollHeight);  // 0 ← 空的
console.log('clientHeight:', p.clientHeight);  // 0 ← 完全沒高度
```

---

## 解法

### 1. 改用 `Div` component 而不是 `Html`

`Div` 渲染為 `<div>`，可以合法包含 block-level 內容：

```java
// ❌ 舊的
private Html ganttHtml;
ganttHtml = new Html();

// ✅ 新的
private Div ganttHtml;
ganttHtml = new Div();
ganttHtml.setStyle("overflow: auto;");
```

### 2. 用 JavaScript innerHTML 注入內容

ZK 的 `Div.setContent()` 不存在，改用 `Clients.evalJavaScript()` 注入：

```java
private void setGanttContent(String html) {
    String uuid = ganttHtml.getUuid();
    // 用 JS template literal，只需 escape backslash 和 backtick
    String escaped = html.replace("\\", "\\\\").replace("`", "\\`");
    Clients.evalJavaScript(
        "document.getElementById('" + uuid + "').innerHTML=`" + escaped + "`;"
    );
}
```

注意：用 backtick template literal（不是單引號或雙引號），避免 HTML 裡大量的 `"` 需要 escape。

### 3. JS 設定高度要用 `requestAnimationFrame`

Gantt 內容 render 完後，用 JS 把 div 高度設為「視窗剩餘空間」，讓 `overflow:auto` 產生 scrollbar：

```java
private void adjustGanttHeight() {
    String uuid = ganttHtml.getUuid();
    Clients.evalJavaScript(
        "(function(){" +
        "  var el=document.getElementById('" + uuid + "');" +
        "  if(!el)return;" +
        "  function setH(){" +
        "    var top=el.getBoundingClientRect().top;" +
        "    var h=window.innerHeight-top-8;" +
        "    if(h>100){el.style.height=h+'px';}" +
        "  }" +
        "  requestAnimationFrame(setH);" +   // ← 必須等瀏覽器 layout 完再量
        "  window.removeEventListener('resize',el._rkResizeH);" +
        "  el._rkResizeH=function(){requestAnimationFrame(setH);};" +
        "  window.addEventListener('resize',el._rkResizeH);" +
        "})();"
    );
}
```

**為什麼要 `requestAnimationFrame`：**
`Clients.evalJavaScript()` 發出 JS 時，瀏覽器可能還沒完成 layout reflow。直接呼叫 `getBoundingClientRect().top` 可能取得 `0`，導致高度算成整個視窗高度，沒有 overflow，沒有 scrollbar。`requestAnimationFrame` 讓瀏覽器先做一次 layout 再量。

---

## iDempiere dashboard 的 vflex 限制

iDempiere dashboard 面板的外層容器高度是 `auto`，導致 ZK 的 `vflex="1"` chain 無法傳遞到 Gantt 元件。ZK 的 vflex 需要父元件有明確的 pixel 高度才能分配，但 dashboard 沒有，所以：

- `vflex="1"` 設在 ganttHtml 上**沒有效果**
- 必須用 JavaScript 手動測量並設定 pixel 高度
- 這是 bypass ZK layout system 的 workaround，是 iDempiere dashboard 的已知限制

---

## 相關 commit

| SHA | 說明 |
|-----|------|
| `db58283` | 初次加入 `adjustGanttHeight()` JS 高度設定 |
| `9a5d510` | 改用 `requestAnimationFrame` 修正 timing |
| `76d6941` | 改用 `Div` + innerHTML 修正 ZK span 問題 |
| `8ab6d59` | 補回 `Html` import（projectPanelHtml 還在用） |
