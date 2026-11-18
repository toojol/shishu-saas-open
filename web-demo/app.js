// 市枢开源规则演示：只负责把输入发给 saas-demo 服务，并展示结果。
(function () {
  "use strict";

  function $(id) {
    return document.getElementById(id);
  }

  function esc(text) {
    return String(text == null ? "" : text)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  }

  function showError(el, message) {
    el.innerHTML = '<p class="error">' + esc(message) + "</p>";
  }

  async function postText(url, body) {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "text/plain; charset=utf-8" },
      body: body,
    });
    return res.json().then(function (data) {
      return { ok: res.ok, data: data };
    });
  }

  async function getJson(url) {
    const res = await fetch(url);
    return res.json().then(function (data) {
      return { ok: res.ok, data: data };
    });
  }

  function run(button, fn) {
    button.addEventListener("click", function () {
      button.disabled = true;
      Promise.resolve()
        .then(fn)
        .catch(function (err) {
          // 网络或服务未启动时给出提示
          alert("请求失败：" + err.message + "\n请确认演示服务已启动。");
        })
        .finally(function () {
          button.disabled = false;
        });
    });
  }

  // ---- 1. 联营对账 ----
  run($("consign-run"), async function () {
    const out = $("consign-out");
    const r = await postText("/api/consign/settle", $("consign-input").value);
    if (!r.ok) {
      return showError(out, r.data.error);
    }
    const rows = r.data.statements.map(function (s) {
      return (
        "<tr><td>" + esc(s.partnerId) + "</td><td>" + s.lineCount + "</td>" +
        "<td>" + esc(s.netSales) + "</td><td>" + esc(s.rate) + "%</td>" +
        "<td>" + esc(s.storeTake) + "</td><td>" + esc(s.payableToPartner) + "</td></tr>"
      );
    });
    out.innerHTML =
      "<table><thead><tr><th>合作方</th><th>行数</th><th>净销售</th><th>综合扣点</th>" +
      "<th>店抽成</th><th>应付合作方</th></tr></thead><tbody>" +
      rows.join("") +
      "</tbody></table>" +
      '<p class="note">店抽成 = 净销售 × 扣点；应付合作方 = 净销售 − 店抽成。扣点按行分别计算后再汇总。</p>';
  });

  // ---- 2. 日结对账 ----
  run($("day-run"), async function () {
    const out = $("day-out");
    const r = await postText("/api/finance/day-close", $("day-input").value);
    if (!r.ok) {
      return showError(out, r.data.error);
    }
    const d = r.data;
    const methods = Object.keys(d.byMethod).map(function (k) {
      return "<tr><td>" + esc(k) + "</td><td>" + esc(d.byMethod[k]) + "</td></tr>";
    });
    const verdict = d.balanced
      ? '<p class="ok">销售额与收款合计一致。</p>'
      : '<p class="warn">差额 ' + esc(d.unmatched) + " 元，需要人工核对。</p>";
    const cash = d.cashDiff != null
      ? '<p class="' + (d.cashDiff.startsWith("-") ? "warn" : "ok") + '">现金' +
        (d.cashDiff.startsWith("-") ? "短款 " : "长款 ") + esc(d.cashDiff.replace("-", "")) + " 元</p>"
      : "";
    out.innerHTML =
      "<p>应收销售额 <b>" + esc(d.salesTotal) + "</b>，收款合计 <b>" + esc(d.tenderTotal) + "</b></p>" +
      verdict +
      "<table><thead><tr><th>支付方式</th><th>金额</th></tr></thead><tbody>" +
      methods.join("") +
      "</tbody></table>" +
      cash;
  });

  // ---- 3. 秤条码 ----
  run($("scale-run"), async function () {
    const out = $("scale-out");
    const code = $("scale-code").value.trim();
    const r = await getJson("/api/device/scale-decode?code=" + encodeURIComponent(code));
    if (!r.ok) {
      return showError(out, r.data.error);
    }
    const d = r.data;
    if (!d.ok) {
      return showError(out, d.reason);
    }
    out.innerHTML =
      "<table><tbody>" +
      "<tr><th>PLU</th><td>" + d.plu + "</td></tr>" +
      "<tr><th>中间段原始值</th><td>" + d.payloadRaw + "</td></tr>" +
      "<tr><th>换算结果</th><td>" + esc(d.value) + " " + esc(d.unit) + "</td></tr>" +
      "<tr><th>校验位</th><td>" + (d.checksumOk ? "正确" : "错误（RELAX 模式）") + "</td></tr>" +
      "</tbody></table>";
  });

  // ---- 4. PLU 帧 ----
  run($("plu-run"), async function () {
    const out = $("plu-out");
    const url =
      "/api/device/plu-frame?plu=" + encodeURIComponent($("plu-no").value.trim()) +
      "&name=" + encodeURIComponent($("plu-name").value) +
      "&price=" + encodeURIComponent($("plu-price").value.trim());
    const r = await getJson(url);
    if (!r.ok) {
      return showError(out, r.data.error);
    }
    out.innerHTML =
      '<pre class="frame">' + esc(r.data.frame) + "</pre>" +
      '<p class="note">共 ' + r.data.bytes + " 字节（GB2312）。品名区位码：" + esc(r.data.nameAreaCode) + "</p>";
  });
})();
