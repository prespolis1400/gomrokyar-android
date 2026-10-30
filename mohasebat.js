if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/mohasebat-varedat/sw.js');
    });
  }

'use strict';

    /* ==========================================================
       §A — CONSTANTS & CONFIGURATION
       ========================================================== */
    const CONFIG = Object.freeze({
      API_URL:        'https://gomrokyar.heidar.workers.dev/',
      API_TIMEOUT:    8000,
      TOAST_DURATION: 4000,

      // نرخ‌های محاسباتی
      RATES: {
        INSURANCE_DEFAULT:  0.0005,   // ۰.۰۵٪ بیمه پیش‌فرض
        STANDARD_TAX:       0.0087,    // 8.7 در ۱۰۰۰ عوارض استاندارد
        FOOD_DRUG_TAX:      0.002,    // ۲ در ۱۰۰۰ غذا و دارو
        ALIOLHESAB_TAX:     0.02,     // ۲٪ مالیات علی‌الحساب
        VAT:                0.10,     // ۱۰٪ ارزش افزوده
        TAavoni_DISCOUNT:   4 / 10,   // ۴/۱۰ تخفیف تعاونی از VAT
        TARIFF_DISCOUNT:    0.50,     // ۵۰٪ تخفیف حقوق ورودی تعاونی
      },

      // نرخ‌های fallback وقتی API در دسترس نیست
      FALLBACK_CURRENCIES: [
        { currency: 'دلار آمریکا (USD)', rate: 1310661 },
        { currency: 'یورو (EUR)',        rate: 1551964 },
        { currency: 'درهم امارات (AED)', rate: 356885  },
      ],

      // آیتم‌های پیش‌فرض باکس ۰۴۲ که بعد از ریست checked باشند
      DEFAULT_CHECKED_042: ['هلال احمر', 'عوارض ۱۲ در ۱۰۰۰'],
    });

    /* ==========================================================
       §B — CACHED DOM REFERENCES
       ========================================================== */
    const DOM = {};
    function cacheDom() {
      const ids = [
        'loadingOverlay', 'inpWeight', 'inpAmount', 'inpCurrency', 'inpRate',
        'inpTariff', 'inpManualInsurance', 'chkStandard', 'chkFoodDrug',
        'resultCard', 'resultTitle', 'resultContent', 'aboutModal',
        'toast', 'themeIcon', 'box042Container',
        'btnReset', 'btnAbout', 'btnTheme', 'btnCalcAzad',
        'btnCalcTaavoni', 'btnCloseModal',
      ];
      ids.forEach(id => { DOM[id] = document.getElementById(id); });
    }

    /* ==========================================================
       §C — UTILITY FUNCTIONS
       ========================================================== */

    /** تبدیل رشته به عدد با حذف کاما و کاراکترهای غیرمجاز */
    function parseNum(id) {
      const el = DOM[id];
      if (!el) return 0;
      return parseFloat(el.value.replace(/,/g, '').replace(/[^0-9.]/g, '')) || 0;
    }

    /** فرمت عدد با جداکننده هزارگان */
    function fmt(val) {
      return isNaN(val) ? '۰' : Math.round(val).toLocaleString('en-US');
    }

    /** کپی متن در کلیپ‌بورد با fallback */
    async function copyToClipboard(text) {
      if (!text) return;
      try {
        await navigator.clipboard.writeText(text);
      } catch {
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
      }
      showToast(`✅ ${text} کپی شد!`);
    }

    /** نمایش Toast */
    let toastTimer = null;
    function showToast(msg, type = 'success', duration = CONFIG.TOAST_DURATION) {
      const t = DOM.toast;
      t.className = `toast ${type} show`;
      t.textContent = msg;
      clearTimeout(toastTimer);
      toastTimer = setTimeout(() => t.classList.remove('show'), duration);
    }

    /** فرمت‌دهی زنده ورودی عددی */
    function liveFormatInput(e) {
      const raw = e.target.value.replace(/,/g, '').replace(/[^0-9.]/g, '');
      if (!raw) { e.target.value = ''; return; }
      const [intPart, decPart] = raw.split('.');
      e.target.value = parseInt(intPart, 10).toLocaleString('en-US') + (decPart !== undefined ? '.' + decPart : '');
      DOM.resultCard.classList.remove('visible');
    }

    /* ==========================================================
       §D — API LAYER
       ========================================================== */
    async function fetchWithTimeout(url, timeout = CONFIG.API_TIMEOUT) {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), timeout);
      try {
        return await fetch(url, { signal: ctrl.signal });
      } finally {
        clearTimeout(timer);
      }
    }

    async function loadCurrencies() {
      const sel = DOM.inpCurrency;
      DOM.loadingOverlay.classList.remove('hidden');
      sel.innerHTML = '<option value="">در حال بارگذاری نرخ‌ها...</option>';

      let success = false;
      try {
        const res = await fetchWithTimeout(CONFIG.API_URL);
        const json = await res.json();
        if (json.success && Array.isArray(json.data) && json.data.length) {
          currencyData = json.data;
          success = true;
        }
      } catch {
        showToast('خطا در اتصال به سرور! نرخ‌های پیش‌فرض بارگذاری شدند.', 'error', 5000);
      }

      sel.innerHTML = '<option value="">-- انتخاب ارز مورد نظر --</option>';
      const list = success
        ? currencyData.map(c => ({
            label: `${c.currency} : ${Number(String(c.rate).replace(/,/g, '').replace(/[^0-9.]/g, '')).toLocaleString('en-US')} ریال`,
            rate:  String(c.rate).replace(/,/g, '').replace(/[^0-9.]/g, ''),
          }))
        : CONFIG.FALLBACK_CURRENCIES.map(c => ({
            label: `${c.currency} : ${c.rate.toLocaleString('en-US')} ریال`,
            rate:  String(c.rate),
          }));

      list.forEach(item => {
        sel.insertAdjacentHTML('beforeend', `<option value="${item.rate}">${item.label}</option>`);
      });

      DOM.loadingOverlay.classList.add('hidden');
    }

    let currencyData = [];

    /* ==========================================================
       §E — CALCULATION ENGINE
       ========================================================== */

    /**
     * محاسبه آیتم‌های باکس ۰۴۲
     * @returns {{ total: number, html: string }}
     */
    function calcBox042(weight, totalValue, tariff) {
      let total = 0;
      let html = '';

      DOM.box042Container.querySelectorAll('.box042-item:checked').forEach(cb => {
        const type = cb.dataset.type;
        const rate = parseFloat(cb.dataset.rate);
        const name = cb.dataset.name;

        let cost;
        switch (type) {
          case 'tariff': cost = Math.round(tariff     * rate); break;
          case 'value':  cost = Math.round(totalValue * rate); break;
          case 'weight': cost = Math.round(weight     * rate); break;
          default:       cost = 0;
        }

        total += cost;
        html += `<div class="box042-chip" data-copy="${fmt(cost)}">
                   <span style="color:var(--text-muted)">${name}:</span>
                   <span class="box042-chip-val">${fmt(cost)}</span>
                 </div>`;
      });

      if (!html) html = '<div style="opacity:.6;font-size:.85rem">موردی انتخاب نشده است</div>';
      return { total, html };
    }

    /**
     * هسته اصلی محاسبه
     * @param {'azad'|'taavoni'} mode
     */
        function calculate(mode) {
      try {
        const weight   = parseNum('inpWeight');
        const amount   = parseNum('inpAmount');
        const rate     = parseNum('inpRate');
        const tariffPct = parseFloat(DOM.inpTariff.value) || 0;
        const manualIns = parseNum('inpManualInsurance');

        if (!weight || !amount || !rate) {
          showToast('مقادیر وزن، ارزش و نرخ ارز را کامل وارد کنید.', 'error');
          return;
        }

        const R = CONFIG.RATES;

        // ۱) بیمه
        const insurance = manualIns > 0 ? manualIns : Math.round(amount * R.INSURANCE_DEFAULT * rate);

        // ۲) ارزش سیف
        const cifValue = Math.round((amount * rate) + insurance + 1);

        // ۳) حقوق ورودی
        const tariff = Math.round(cifValue * (tariffPct / 100));

        // ۴) باکس ۰۴۲
        const box042 = calcBox042(weight, cifValue, tariff);

        // ۵) مالیات ۲٪ علی‌الحساب
        const tax4 = Math.round((cifValue + tariff) * R.ALIOLHESAB_TAX);

        // ۶) عوارض ثابت
        const standard = DOM.chkStandard.checked ? Math.round((cifValue * R.STANDARD_TAX)*1.1) : 0;
        const foodDrug = DOM.chkFoodDrug.checked ? Math.round(cifValue * R.FOOD_DRUG_TAX) : 0;

        // ۷) ارزش افزوده
        const vat = Math.round((cifValue + tariff) * R.VAT);

        // پیدا کردن مبلغ هلال احمر بدون وابستگی به DOM.chkRedCrescent
        function isBox042CheckedByText(text) {
          const labels = document.querySelectorAll('label');
          for (const label of labels) {
            const txt = label.innerText || label.textContent || '';
            if (txt.includes(text)) {
              const checkbox = label.querySelector('input[type="checkbox"]');
              if (checkbox && checkbox.checked) return true;
            }
          }

          const inputs = document.querySelectorAll('input[type="checkbox"]');
          for (const inp of inputs) {
            const txt =
              inp.getAttribute('title') ||
              inp.getAttribute('aria-label') ||
              inp.dataset.title ||
              inp.id || '';
            if (txt.includes(text) && inp.checked) return true;
          }

          return false;
        }

        let redCrescentAmount = 0;

        // اگر calcBox042 آیتم‌ها را برمی‌گرداند، از آنجا هلال احمر را پیدا کن
        if (Array.isArray(box042.items)) {
          const item = box042.items.find(i => String(i.title || '').includes('هلال احمر'));
          if (item) redCrescentAmount = item.amount || 0;
        }
        // اگر calcBox042 مقدار هلال احمر را جدا برمی‌گرداند
        else if (typeof box042.redCrescent === 'number') {
          redCrescentAmount = box042.redCrescent;
        }
        // در غیر این صورت از روی چک‌باکس داخل صفحه پیدا کن
        else if (isBox042CheckedByText('هلال احمر')) {
          redCrescentAmount = Math.round(tariff * 0.01);
        }

        // ۸) محاسبه نهایی بر اساس مود
        let totalPayment = 0;
        let costPerKg = 0;
        let box049 = 0;
        let basePayment = 0;

        if (mode === 'taavoni') {
          // جمع پرداختی پایه = ارزش افزوده + مجموع باکس 042 + حقوق ورودی
          basePayment = vat + box042.total + tariff;

          // باکس 049 در حالت اولیه: حقوق ورودی + هلال احمر
          box049 = tariff + redCrescentAmount;

          // جمع پرداختی نهایی = جمع پرداختی پایه - باکس 049
          totalPayment = basePayment - box049;

          // بهای تمام‌شده هر کیلو
          costPerKg = Math.round((totalPayment + tax4 + standard + foodDrug) / weight);
        } else {
          totalPayment = tariff + box042.total + vat;
          costPerKg    = Math.round((totalPayment + tax4 + standard + foodDrug) / weight);
        }

        renderResults({
          mode,
          insurance,
          cifValue,
          tariff,
          box042Total: box042.total,
          box042Html:  box042.html,
          vat,
          tax4,
          standard,
          foodDrug,
          totalPayment,
          costPerKg,
          manualIns,
          weight,
          basePayment,
          box049,
          redCrescentAmount
        });

      } catch (error) {
        console.error(error);
        showToast('خطای محاسبه: ' + error.message, 'error');
      }
    }

    /* ==========================================================
       §F — RENDERING
       ========================================================== */

            function renderResults(res) {
      const isTaav = res.mode === 'taavoni';
      const container = DOM.resultContent;
      container.dataset.resultData = JSON.stringify(res);

      container.innerHTML = `
        <div class="result-kpi">
          <div class="kpi-title">بهای تمام‌شده هر کیلوگرم خالص</div>
          <div class="kpi-val" data-copy="${fmt(res.costPerKg)}" title="کلیک برای کپی">
            ${fmt(res.costPerKg)} <span style="font-size:1.1rem;font-weight:normal;opacity:.8">ریال</span>
          </div>
        </div>

        <div class="details-grid">
          ${detailItem(res.manualIns > 0 ? 'بیمه حمل (دستی)' : 'بیمه حمل (۰.۰۵٪)', res.insurance)}
          ${detailItem('ارزش کل ریالی (سیف)', res.cifValue)}
          ${detailItem('حقوق ورودی', res.tariff)}
          ${detailItem('ارزش افزوده (۱۰٪)', res.vat)}

          ${detailItem('مالیات ۲٪ علی‌الحساب', res.tax4)}
          ${detailItem('عوارض استاندارد', res.standard)}
          ${detailItem('غذا و دارو', res.foodDrug)}

          <div class="box042">
            <div class="box042-header">
              <span>تجمیع عوارض باکس ۰۴۲</span>
              <span data-copy="${fmt(res.box042Total)}" style="cursor:pointer">${fmt(res.box042Total)} ریال</span>
            </div>
            <div class="box042-chips">${res.box042Html}</div>
          </div>

          ${isTaav ? `
            <div class="box042" style="border:2px solid #ea580c; background:rgba(234,88,12,.08)">
              <div class="box042-header box049-header">
                <span>🎯 باکس ۰۴۹ (تخفیف حقوق ورودی و عوارض)</span>
                <span data-copy="${fmt(res.box049)}" style="cursor:pointer">- ${fmt(res.box049)} ریال</span>
              </div>
              <div class="box042-chips box049-chips">
                <span class="chip">حقوق ورودی: ${fmt(res.tariff)}</span>
                <span class="chip">هلال احمر: ${fmt(res.redCrescentAmount || 0)}</span>
              </div>
            </div>

            <div style="grid-column:1/-1; border:1px dashed #7c3aed; background:rgba(124,58,237,.10); border-radius:12px; padding:12px;">
              <label style="display:flex; align-items:center; gap:8px; cursor:pointer;">
                <input type="checkbox" id="discountCheck">
                <span>🎯 اعمال تخفیف ۵۰٪ حقوق ورودی (در باکس ۰۴۹)</span>
              </label>
            </div>
          ` : ''}

          <div class="total-payment ${isTaav ? 'taavoni' : ''}" data-copy="${fmt(res.totalPayment)}">
            <span>${isTaav ? 'جمع پرداختی (تعاونی)' : 'جمع کل پرداختی'}</span>
            <span class="total-amount">${fmt(res.totalPayment)} <small>ریال</small></span>
          </div>
        </div>
      `;

      // اتصال listener تخفیف تعاونی
      if (isTaav) {
        const dc = document.getElementById('discountCheck');
        if (dc) dc.addEventListener('change', recalcWithDiscount);
      }

      showResultCard(res.mode);
    }

    /** ساخت HTML یک آیتم جزئیات */
    function detailItem(title, value) {
      return `<div class="detail-item" data-copy="${fmt(value)}">
                <span class="detail-title">${title}</span>
                <span class="detail-val">${fmt(value)}</span>
              </div>`;
    }

    function showResultCard(mode) {
      const card  = DOM.resultCard;
      const isTaav = mode === 'taavoni';
      const label  = isTaav ? 'تعاونی' : 'آزاد';
      const icon   = isTaav ? 'apartment' : 'analytics';

      card.className = `result-card visible ${isTaav ? 'taavoni-mode' : ''}`;
      DOM.resultTitle.innerHTML = `
        <span class="material-icons-round" aria-hidden="true">${icon}</span>
        نتایج محاسبات
        <span class="mode-badge mode-badge--${mode}">${label}</span>
      `;
      card.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

        /** بازمحاسبه با تخفیف ۵۰٪ حقوق ورودی (تعاونی) */
    function recalcWithDiscount() {
      const container = DOM.resultContent;
      const res = JSON.parse(container.dataset.resultData || '{}');

      if (!res.tariff || res.basePayment === undefined) return;

      const checkbox = document.getElementById('discountCheck');
      const isActive = checkbox ? checkbox.checked : false;

      const redCrescentAmount = res.redCrescentAmount || 0;
      const halfTariff = Math.round(res.tariff / 2);

      // بدون تیک: باکس 049 = حقوق ورودی + هلال احمر
      // با تیک: باکس 049 = حقوق ورودی / 2 + هلال احمر
      const box049New = isActive
        ? halfTariff + redCrescentAmount
        : res.tariff + redCrescentAmount;

      // جمع پرداختی نهایی = جمع پرداختی پایه - باکس 049
      const finalPayment = res.basePayment - box049New;

      // بهای تمام‌شده هر کیلو
      const costPerKg = Math.round(
        (finalPayment + res.tax4 + res.standard + res.foodDrug) / res.weight
      );

      // به‌روزرسانی باکس 049
      const box049El = container.querySelector('.box049-header')?.closest('.box042');
      if (box049El) {
        const headerSpan = box049El.querySelector('.box049-header span:last-child');
        if (headerSpan) {
          headerSpan.textContent = `- ${fmt(box049New)} ریال`;
          headerSpan.dataset.copy = fmt(box049New);
        }

        const chips = box049El.querySelector('.box049-chips');
        if (chips) {
          chips.innerHTML = `
            <span class="chip">حقوق ورودی${isActive ? ' / ۲' : ''}: ${fmt(isActive ? halfTariff : res.tariff)}</span>
            <span class="chip">هلال احمر: ${fmt(redCrescentAmount)}</span>
          `;
        }
      }

      // به‌روزرسانی جمع پرداختی
      const totalEl = container.querySelector('.total-payment .total-amount');
      if (totalEl) {
        totalEl.innerHTML = `${fmt(finalPayment)} <small>ریال</small>`;
      }

      const totalPaymentBox = container.querySelector('.total-payment');
      if (totalPaymentBox) {
        totalPaymentBox.dataset.copy = fmt(finalPayment);
      }

      // به‌روزرسانی KPI
      const kpiEl = container.querySelector('.kpi-val');
      if (kpiEl) {
        kpiEl.innerHTML = `${fmt(costPerKg)} <span style="font-size:1.1rem;opacity:.8">ریال</span>`;
        kpiEl.dataset.copy = fmt(costPerKg);
      }
    }

    /* ==========================================================
       §G — FORM ACTIONS
       ========================================================== */
    function resetForm() {
      ['inpWeight', 'inpAmount', 'inpCurrency', 'inpRate', 'inpManualInsurance'].forEach(id => {
        if (DOM[id]) DOM[id].value = '';
      });
      DOM.inpTariff.value = '5';
      DOM.chkStandard.checked = true;
      DOM.chkFoodDrug.checked = true;

      DOM.box042Container.querySelectorAll('.box042-item').forEach(cb => {
        cb.checked = CONFIG.DEFAULT_CHECKED_042.includes(cb.dataset.name);
      });

      DOM.resultCard.classList.remove('visible', 'taavoni-mode');
      DOM.resultContent.innerHTML = '';
      showToast('فرم با موفقیت ریست شد.');
    }

    /* ==========================================================
       §H — THEME
       ========================================================== */
    function toggleTheme() {
      const html = document.documentElement;
      const isDark = html.getAttribute('data-theme') === 'dark';
      html.setAttribute('data-theme', isDark ? 'light' : 'dark');
      DOM.themeIcon.textContent = isDark ? 'dark_mode' : 'light_mode';
    }

    /* ==========================================================
       §I — MODAL
       ========================================================== */
    function openModal()  { DOM.aboutModal.classList.add('show'); }
    function closeModal() { DOM.aboutModal.classList.remove('show'); }

    /* ==========================================================
       §J — EVENT BINDING
       ========================================================== */
    function bindEvents() {
      // دکمه‌ها
      DOM.btnReset.addEventListener('click', resetForm);
      DOM.btnAbout.addEventListener('click', openModal);
      DOM.btnCloseModal.addEventListener('click', closeModal);
      DOM.btnTheme.addEventListener('click', toggleTheme);
      DOM.btnCalcAzad.addEventListener('click', () => calculate('azad'));
      DOM.btnCalcTaavoni.addEventListener('click', () => calculate('taavoni'));

      // بستن مودال با کلیک روی پس‌زمینه
      DOM.aboutModal.addEventListener('click', e => {
        if (e.target === DOM.aboutModal) closeModal();
      });

      // فرمت زنده ورودی‌های عددی
      ['inpWeight', 'inpAmount', 'inpRate', 'inpManualInsurance'].forEach(id => {
        DOM[id].addEventListener('input', liveFormatInput);
      });

      // تغییر ارز → درج خودکار نرخ
      DOM.inpCurrency.addEventListener('change', e => {
        DOM.inpRate.value = e.target.value ? Number(e.target.value).toLocaleString('en-US') : '';
        DOM.resultCard.classList.remove('visible');
      });

      // هر چک‌باکس تغییر کند → نتیجه پنهان شود
      document.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.addEventListener('change', () => DOM.resultCard.classList.remove('visible'));
      });

      // Event Delegation برای کپی
      document.addEventListener('click', e => {
        const target = e.target.closest('[data-copy]');
        if (target) copyToClipboard(target.dataset.copy);
      });
    }

    /* ==========================================================
       §K — INITIALIZATION
       ========================================================== */
    function init() {
      cacheDom();
      bindEvents();
      document.documentElement.setAttribute('data-theme', 'light');
      loadCurrencies();
    }

    document.addEventListener('DOMContentLoaded', init);
  /* ==========================================================
   §H — TOOLTIP راهنمای نحوه محاسبه (افزودنی جدید - بدون تغییر کدهای قبلی)
   ========================================================== */
(function () {
  const TIP_FORMULAS = {
    'بهای تمام‌شده هر کیلوگرم خالص': '(جمع پرداختی + مالیات ۲٪ + عوارض استاندارد + غذا و دارو) ÷ وزن خالص',
    'جمع پرداختی (تعاونی)': '(ارزش افزوده + مجموع باکس ۰۴۲ + حقوق ورودی) − باکس ۰۴۹',
    'جمع کل پرداختی': 'حقوق ورودی + مجموع باکس ۰۴۲ + ارزش افزوده',
    'باکس ۰۴۹': 'بدون تیک: حقوق ورودی + هلال احمر | با تیک ۵۰٪: (حقوق ورودی ÷ ۲) + هلال احمر — این مبلغ از جمع پرداختی کسر می‌شود',
    'باکس ۰۴۲': 'مجموع تمام عوارض فعال‌شده در باکس ۰۴۲',
    'تجمیع عوارض باکس ۰۴۲': 'مجموع تمام عوارض فعال‌شده در باکس ۰۴۲',
    'بیمه حمل (دستی)': 'مبلغ بیمه را به‌صورت دستی وارد کرده‌اید',
    'بیمه حمل (۰.۰۵٪)': 'ارزش کل × نرخ ارز × ۰.۰۰۵',
    'ارزش کل ریالی (سیف)': '(ارزش کل × نرخ ارز) + بیمه حمل + ۱',
    'حقوق ورودی': 'ارزش سیف × (درصد حقوق ورودی ÷ ۱۰۰)',
    'ارزش افزوده (۱۰٪)': '(ارزش سیف + حقوق ورودی) × ۱۰٪',
    'مالیات ۲٪ علی‌الحساب': '(ارزش سیف + حقوق ورودی) × ۲٪',
    'عوارض استاندارد': 'ارزش سیف × ۰.۰۰ (۲ در هزار)',
    'غذا و دارو': 'ارزش سیف × ۰.۰۰۲ (۲ در هزار)',
    'هلال احمر': 'حقوق ورودی × ۱٪',
    'نهاده دامی': 'ارزش سیف × ۱٪',
    'عوارض ۱۲ در ۱۰۰۰': 'ارزش سیف × ۰.۰۱۲',
    'پسماند': 'ارزش سیف × ۰.۰۰۰۵',
    'گوشت قرمز': 'وزن خالص × ۲۷۵۰ ریال',
    'گوشت مرغ': 'وزن خالص × ۱۱۰ ریال',
    'خودرو بنزینی': 'ارزش سیف × ۱۰٪',
    'خودرو سواری': 'ارزش سیف × ۵٪',
    'کالای آسیب‌رسان': 'ارزش سیف × ۱۰٪',
    'غلات/چوب': 'وزن خالص × ۲۰ ریال',
    'چای': 'وزن خالص × ۳۰۰ ریال',
    'پنبه/کنجاله': 'وزن خالص × ۵۰۰ ریال',
    'میوه/سبزیجات': 'وزن خالص × ۷۰۰ ریال',
    'سم آماده': 'وزن خالص × ۱۴۴۰ ریال'
  };

  const tip = document.createElement('div');
  tip.id = 'calcTooltip';
  document.body.appendChild(tip);

  function findFormula(text) {
    if (!text) return null;
    text = text.trim();
    if (TIP_FORMULAS[text]) return TIP_FORMULAS[text];
    for (const key of Object.keys(TIP_FORMULAS)) {
      if (text.includes(key)) return TIP_FORMULAS[key];
    }
    return null;
  }

  function titleFrom(target) {
    // تیک تخفیف ۵۰٪
    const label = target.closest('label');
    if (label && label.querySelector('#discountCheck')) return '@DISCOUNT@';

    const host = target.closest('.chip, .detail-item, .box042, .total-payment, .result-kpi');
    if (!host) return null;

    if (host.classList.contains('chip')) {
      if (host.parentElement &&
          host.parentElement.classList.contains('box049-chips') &&
          host.textContent.includes('حقوق ورودی')) {
        return '@BOX049_TARIFF@';
      }
      return host.textContent;
    }

    if (host.classList.contains('detail-item')) {
      const t = host.querySelector('.detail-title');
      return t ? t.textContent : null;
    }

    if (host.classList.contains('result-kpi')) return 'بهای تمام‌شده هر کیلوگرم خالص';

    if (host.classList.contains('total-payment')) {
      const s = host.querySelector('span');
      return s ? s.textContent : null;
    }

    const h = host.querySelector('.box049-header span:first-child, .box042-header span:first-child');
    return h ? h.textContent : null;
  }

  function hideTip() { tip.style.display = 'none'; }

  document.addEventListener('mousemove', function (e) {
    if (!e.target || !e.target.closest) { hideTip(); return; }

    const inside = e.target.closest('#resultContent');
    if (!inside) { hideTip(); return; }

    const raw = titleFrom(e.target);
    let text = null;

    if (raw === '@DISCOUNT@') {
      text = 'با فعال‌کردن این تیک، در فرمول باکس ۰۴۹ به‌جای کل حقوق ورودی، نصف حقوق ورودی محاسبه می‌شود';
    } else if (raw === '@BOX049_TARIFF@') {
      text = 'سهم حقوق ورودی در باکس ۰۴۹ | بدون تیک: کل حقوق ورودی | با تیک: حقوق ورودی ÷ ۲';
    } else {
      text = findFormula(raw);
    }

    if (!text) { hideTip(); return; }

    tip.textContent = '💡 ' + text;
    tip.style.display = 'block';

    const pad = 14;
    let x = e.clientX + pad;
    let y = e.clientY + pad;
    const r = tip.getBoundingClientRect();
    if (x + r.width > window.innerWidth - 6)  x = e.clientX - r.width - pad;
    if (y + r.height > window.innerHeight - 6) y = e.clientY - r.height - pad;
    tip.style.left = x + 'px';
    tip.style.top  = y + 'px';
  });

  document.addEventListener('mouseleave', hideTip);
  window.addEventListener('scroll', hideTip, true);
})();