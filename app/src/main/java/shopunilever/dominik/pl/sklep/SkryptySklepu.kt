package shopunilever.dominik.pl.sklep

/**
 * Skrypty JS wstrzykiwane do WebView — port z Harmonogramu (webview-skrypty).
 * Klucz: WYKRYJ / SESJA — decyzja po DOM, nie po URL.
 */
object SkryptySklepu {

    const val MOSTEK = """
(function() {
    if (window.__sklepikMostek) return;
    window.__sklepikMostek = true;
    window.SklepikPost = function(obj) {
        try {
            if (window.AndroidBridge && window.AndroidBridge.onWiadomosc) {
                window.AndroidBridge.onWiadomosc(JSON.stringify(obj));
            }
        } catch (e) {}
    };
    window.SklepikLog = function(msg) {
        window.SklepikPost({ type: 'LOG', msg: String(msg) });
    };
    true;
})();
"""

    /** Lepsza skala / viewport na telefonie — mniej „desktopowej” strony w WebView. */
    const val RESPONSYWNOSC = """
(function() {
    try {
        var m = document.querySelector('meta[name="viewport"]');
        if (!m) {
            m = document.createElement('meta');
            m.setAttribute('name', 'viewport');
            (document.head || document.documentElement).appendChild(m);
        }
        m.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes, viewport-fit=cover');
        document.documentElement.style.setProperty('-webkit-text-size-adjust', '100%');
        document.body && document.body.style.setProperty('max-width', '100vw');
        document.body && document.body.style.setProperty('overflow-x', 'auto');
    } catch (e) {}
    true;
})();
"""

    /**
     * 1) Meetanshi „PRZEJDŹ DO SKLEPU”
     * 2) Amasty modal „Akceptuj wszystkie” (zasłania formularz — krytyczne)
     * Retry wewnątrz, bo modal ładuje się przez Knockout z opóźnieniem.
     */
    const val CIASTECZKA = """
(function() {
    if (window.__sklepikCiasteczkaTrwa) return;
    window.__sklepikCiasteczkaTrwa = true;
    function post(info) {
        if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'ciasteczka', sukces: true, info: info });
    }
    function kliknij(el) {
        if (!el) return false;
        try {
            el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
            el.click();
            return true;
        } catch (e) { return false; }
    }
    function ukryj(el) {
        if (!el) return;
        el.style.setProperty('display', 'none', 'important');
        el.style.setProperty('visibility', 'hidden', 'important');
        el.style.setProperty('pointer-events', 'none', 'important');
        el.classList.remove('_show');
    }
    function amastyOtwarte() {
        return document.querySelector('.amgdprcookie-modal-container._show, .modal-popup.amgdprcookie-modal-container._show, aside.amgdprcookie-modal-container._show');
    }
    function kliknijAmasty() {
        var modal = amastyOtwarte() || document.querySelector('.amgdprcookie-modal-container, [data-role=gdpr-cookie-container]');
        var btn = (modal && (modal.querySelector('button.amgdprcookie-button.-allow') || modal.querySelector('.amgdprcookie-button.-allow'))) ||
            document.querySelector('button.amgdprcookie-button.-allow') ||
            document.querySelector('.amgdprcookie-buttons-block .amgdprcookie-button.-allow');
        if (btn && kliknij(btn)) { post('AMASTY_ALLOW'); return true; }
        var all = document.querySelectorAll('button, a');
        for (var i = 0; i < all.length; i++) {
            var t = ((all[i].textContent || '') + '').toLowerCase().replace(/\s+/g, ' ').trim();
            if (t === 'akceptuj wszystkie' || t.indexOf('akceptuj wszystkie') >= 0 || t.indexOf('allow all') >= 0) {
                if (kliknij(all[i])) { post('TEKST:akceptuj wszystkie'); return true; }
            }
        }
        return false;
    }
    function kliknijPrzejdz() {
        if (typeof window.cookieNoticeAccept === 'function') {
            try { window.cookieNoticeAccept(); } catch (e) {}
        }
        var przejdz = document.querySelector('a.m-button.m-accept') ||
            document.querySelector('#m-cookienotice a.m-accept') ||
            document.querySelector('#action-custom-css a.m-accept');
        if (przejdz && kliknij(przejdz)) { post('PRZEJDZ_DO_SKLEPU'); return true; }
        var els = document.querySelectorAll('a, button');
        for (var i = 0; i < els.length; i++) {
            var t = ((els[i].textContent || '') + '').toLowerCase().replace(/\s+/g, ' ').trim();
            if (t.indexOf('przejdź do sklepu') >= 0 || t.indexOf('przejdz do sklepu') >= 0) {
                if (kliknij(els[i])) { post('TEKST:przejdz do sklepu'); return true; }
            }
        }
        return false;
    }
    var proba = 0;
    function tick() {
        proba++;
        kliknijPrzejdz();
        kliknijAmasty();
        ukryj(document.getElementById('m-cookienotice'));
        ukryj(document.getElementById('action-custom-css'));
        if (!amastyOtwarte() || proba >= 12) {
            var m = amastyOtwarte();
            if (m) {
                kliknijAmasty();
                ukryj(m);
                var confirm = document.querySelector('.modals-overlay');
                ukryj(confirm);
            }
            document.body.classList.remove('_has-modal');
            document.body.style.setProperty('overflow', 'auto', 'important');
            window.__sklepikCiasteczkaTrwa = false;
            post(amastyOtwarte() ? 'PARTIAL' : 'DONE');
            return;
        }
        setTimeout(tick, 700);
    }
    tick();
})();
true;
"""

    /** Wspólne helpery widoczności / „czy to sklep zalogowany”. */
    private const val JS_HELPERY = """
    function sklepikWidoczny(el) {
        if (!el) return false;
        var st = window.getComputedStyle(el);
        if (st.display === 'none' || st.visibility === 'hidden' || Number(st.opacity) === 0) return false;
        var r = el.getBoundingClientRect();
        if (r.width < 2 || r.height < 2) return false;
        var p = el;
        while (p) {
            if (p.id === 'authenticationPopup') {
                var pst = window.getComputedStyle(p);
                if (pst.display === 'none' || p.style.display === 'none') return false;
            }
            p = p.parentElement;
        }
        return true;
    }
    function sklepikCzyStronaHasla() {
        // UWAGA: Magento trzyma ukryte #pass / #login-form też na /access i na home —
        // NIE wolno brać samego istnienia w DOM. Tylko URL login LUB WIDOCZNE hasło.
        var href = (location.href || '').toLowerCase();
        var bodyCls = (document.body && document.body.className) || '';
        if (href.indexOf('customer/account/login') >= 0) return true;
        if (bodyCls.indexOf('customer-account-login') >= 0) return true;
        var pass = document.getElementById('pass') ||
            document.querySelector('input[name="login[password]"]');
        if (sklepikWidoczny(pass)) return true;
        return false;
    }
    function sklepikCzyZalogowanySklep() {
        var href = (location.href || '').toLowerCase();
        var bodyCls = (document.body && document.body.className) || '';
        // Formularz hasła / login Magento — NIGDY nie traktuj jako zalogowany
        // (header ma .navigation / .minicart i wcześniej dawał fałszywy LOGGED_IN).
        if (sklepikCzyStronaHasla()) return false;
        if (href.indexOf('/access') >= 0 || bodyCls.indexOf('cms-access') >= 0) {
            // Strona bramki — zalogowany tylko gdy NIE ma widocznego formularza dostępu
            if (sklepikWidoczny(document.getElementById('access_email'))) return false;
            if (sklepikWidoczny(document.getElementById('access_token'))) return false;
            if (sklepikWidoczny(document.querySelector('input[name="access_token"]'))) return false;
            // Samo /access z wyborem metody = nie zalogowany
            if (href.indexOf('access-email') < 0 && href.indexOf('access-code') < 0) return false;
        }
        var logout = document.querySelector('a[href*="customer/account/logout"], a[href*="/logout"]');
        if (sklepikWidoczny(logout)) return true;
        var welcome = document.querySelector('.customer-welcome, .header.links li.welcome, .logged-in, .customer-name');
        if (sklepikWidoczny(welcome)) return true;
        // Katalog / koszyk / produkty = jesteśmy w sklepie po bramce
        // (bez stron logowania — już odfiltrowane wyżej)
        if (document.querySelector('.product-item, .products-grid, .category-products, .page-products')) {
            if (!sklepikWidoczny(document.getElementById('access_email')) &&
                !sklepikWidoczny(document.getElementById('access_token'))) {
                return true;
            }
        }
        // Root sklepu bez formularza access / login
        if (href.indexOf('/access') < 0 && href.indexOf('login') < 0 &&
            !sklepikWidoczny(document.getElementById('access_email')) &&
            !sklepikWidoczny(document.querySelector('#access_email_form'))) {
            // Nie mylić z pustą/404
            if (bodyCls.indexOf('cms-no-route') < 0 && bodyCls.indexOf('noroute') < 0) {
                if (document.querySelector('.product-item, .products-grid, .page-products, .category-products')) return true;
            }
        }
        return false;
    }
"""

    /** Wykrycie etapu formularza — tylko WIDOCZNE pola (ukryty #pass w popupie Magento nie liczy się). */
    val WYKRYJ = """
(function() {
$JS_HELPERY
    try {
        if (document.querySelector('.amgdprcookie-modal-container._show') ||
            (document.querySelector('button.amgdprcookie-button.-allow') && document.querySelector('.modals-overlay, .amgdprcookie-modal-container._show'))) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'COOKIE_BANNER' });
            return;
        }
        var meet = document.getElementById('m-cookienotice');
        if (meet && sklepikWidoczny(meet) && document.querySelector('a.m-button.m-accept')) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'COOKIE_BANNER' });
            return;
        }
        var href = (location.href || '').toLowerCase();
        // Kolejność: WIDOCZNE pola access → hasło → wybór metody → sesja.
        // Ukryte #pass na /access i na home NIE liczą się.
        var tokenField = document.getElementById('access_token') ||
            document.querySelector('input[name="access_token"]') ||
            document.querySelector('input[name="token"]');
        if (sklepikWidoczny(tokenField)) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'SMS_TOKEN_FIELD' });
            return;
        }
        var emailField = document.getElementById('access_email');
        if (sklepikWidoczny(emailField)) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'EMAIL_FIELD' });
            return;
        }
        // Prawdziwy ekran hasła: URL Magento login LUB widoczne #pass
        if (sklepikCzyStronaHasla()) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'PASSWORD_FIELD' });
            return;
        }
        var passwordField = document.getElementById('pass') ||
            document.querySelector('input[name="login[password]"]');
        if (sklepikWidoczny(passwordField)) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'PASSWORD_FIELD' });
            return;
        }
        // /access — wybór: Autologowanie / Kod SMS / Karta
        var linkSms = document.querySelector('a[href*="access-email"]') ||
            document.querySelector('a.pagebuilder-button-link[href*="access-email"]');
        var naWyborzeUrl = (href.indexOf('/access') >= 0 || href.endsWith('/access')) &&
            href.indexOf('access-email') < 0 &&
            href.indexOf('access-code') < 0 &&
            href.indexOf('customer/account') < 0;
        var maWyborMetod = naWyborzeUrl && (
            sklepikWidoczny(document.getElementById('ad_login')) ||
            !!document.querySelector('.access-azure-login, .autologin-content') ||
            (linkSms && sklepikWidoczny(linkSms))
        );
        if (maWyborMetod) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'ACCESS_METHOD_CHOICE' });
            return;
        }
        if (sklepikCzyZalogowanySklep()) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'ALREADY_LOGGED_IN' });
            return;
        }
        if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'NO_RECOGNIZED_ELEMENTS' });
    } catch (e) {
        if (window.SklepikLog) window.SklepikLog('wykryj: ' + e.message);
    }
})();
true;
""".trimIndent()

    val SESJA = """
(function() {
$JS_HELPERY
    try {
        var bodyCls = (document.body && document.body.className) || '';
        if (bodyCls.indexOf('cms-no-route') >= 0 || bodyCls.indexOf('noroute') >= 0) {
            if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'NOT_FOUND' });
            return;
        }
        if (sklepikCzyStronaHasla()) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'PASSWORD_FIELD' });
            return;
        }
        if (sklepikCzyZalogowanySklep()) {
            if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'LOGGED_IN' });
            return;
        }
        if (sklepikWidoczny(document.querySelector('#pass'))) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'PASSWORD_FIELD' });
            return;
        }
        var hrefSesja = (location.href || '').toLowerCase();
        // Uwaga: query ?access-email= na /customer/account/login NIE jest bramką /access
        var naBramceAccess = hrefSesja.indexOf('/access') >= 0 ||
            hrefSesja.indexOf('/access-email') >= 0 ||
            hrefSesja.indexOf('/access-code') >= 0;
        if (sklepikWidoczny(document.getElementById('access_email')) ||
            sklepikWidoczny(document.getElementById('access_token')) ||
            naBramceAccess) {
            if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'NEEDS_LOGIN' });
            return;
        }
        if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'UNKNOWN' });
    } catch (e) {
        if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'UNKNOWN' });
    }
})();
true;
""".trimIndent()

    fun skryptPrzekieruj(url: String): String {
        val bezpieczny = url.replace("\\", "\\\\").replace("\"", "\\\"")
        return """window.location.href="$bezpieczny"; true;"""
    }

    /**
     * Diagnostyka logowania — zrzut URL + kluczowych pól DOM (widoczność, wartość).
     * Filtr Logcat: tag `SklepLogin`.
     */
    val DIAG_STRONY = """
(function() {
$JS_HELPERY
    function opisPola(el) {
        if (!el) return { jest: false };
        var val = '';
        try {
            if (el.type === 'password') val = el.value ? ('***(' + el.value.length + ')') : '';
            else if (el.type === 'checkbox') val = el.checked ? 'checked' : 'unchecked';
            else val = (el.value || '').toString().slice(0, 80);
        } catch (e) { val = '?'; }
        return {
            jest: true,
            id: el.id || '',
            name: el.name || '',
            type: el.type || el.tagName || '',
            widoczny: sklepikWidoczny(el),
            wartosc: val,
            disabled: !!el.disabled
        };
    }
    function opisSelektor(sel) {
        try { return opisPola(document.querySelector(sel)); } catch (e) { return { jest: false, blad: String(e) }; }
    }
    try {
        var href = location.href || '';
        var bodyCls = (document.body && document.body.className) || '';
        var linkSms = document.querySelector('a[href*="access-email"]');
        var dump = {
            type: 'DIAG',
            href: href,
            path: location.pathname || '',
            title: document.title || '',
            bodyClass: bodyCls.slice(0, 200),
            czyStronaHasla: sklepikCzyStronaHasla(),
            czyZalogowany: sklepikCzyZalogowanySklep(),
            pola: {
                access_email: opisPola(document.getElementById('access_email')),
                access_token: opisPola(document.getElementById('access_token') || document.querySelector('input[name="access_token"]')),
                pass: opisPola(document.getElementById('pass')),
                login_password: opisSelektor('input[name="login[password]"]'),
                email_login: opisPola(document.getElementById('email') || document.querySelector('input[name="login[username]"]')),
                ad_login: opisPola(document.getElementById('ad_login')),
                send2: opisPola(document.getElementById('send2')),
                form_login: opisSelektor('form#login-form, form.form-login, #customer-login-form'),
                checkbox_privacy: opisSelektor('input[id^="amprivacy-checkbox-"], input[name*="amgdpr_agreement"], input[type="checkbox"][name*="privacy"]'),
                link_access_email: linkSms ? { jest: true, href: linkSms.getAttribute('href') || '', tekst: (linkSms.textContent || '').trim().slice(0, 40), widoczny: sklepikWidoczny(linkSms) } : { jest: false },
                azure: !!document.querySelector('.access-azure-login, .autologin-content'),
                navigation: !!document.querySelector('.navigation'),
                minicart: !!document.querySelector('.minicart-wrapper'),
                logout: !!document.querySelector('a[href*="customer/account/logout"]')
            }
        };
        if (window.SklepikPost) window.SklepikPost(dump);
    } catch (e) {
        if (window.SklepikPost) window.SklepikPost({ type: 'DIAG', blad: String(e && e.message || e) });
    }
})();
true;
""".trimIndent()

    /** Na /access wybierz „Kod SMS” → /access-email (nie Azure / nie karta). */
    val WYBIERZ_KOD_SMS = """
(function() {
    try {
        var a = document.querySelector('a[href*="access-email"]') ||
            document.querySelector('a.pagebuilder-button-link[href*="access-email"]');
        if (a) {
            try {
                a.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
            } catch (e) {}
            a.click();
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'wybor', sukces: true, info: 'KLIK_KOD_SMS' });
            return;
        }
        var links = document.querySelectorAll('a, button, span');
        for (var i = 0; i < links.length; i++) {
            var t = ((links[i].textContent || '') + '').toLowerCase().replace(/\s+/g, ' ').trim();
            if (t === 'kod sms' || t.indexOf('kod sms') >= 0) {
                var cel = links[i].closest('a') || links[i];
                try { cel.click(); } catch (e) {}
                if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'wybor', sukces: true, info: 'TEKST_KOD_SMS' });
                return;
            }
        }
        window.location.href = 'https://sklepfirmowyunilever.pl/access-email';
        if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'wybor', sukces: true, info: 'REDIRECT_ACCESS_EMAIL' });
    } catch (e) {
        window.location.href = 'https://sklepfirmowyunilever.pl/access-email';
    }
})();
true;
""".trimIndent()

    fun skryptWypelnijEmail(email: String): String {
        val bezpieczny = email.replace("\\", "\\\\").replace("\"", "\\\"")
        return """
(function() {
    if (window.__sklepikEmailWyslany || window.__sklepikEmailWTrakcie) return;
    window.__sklepikEmailWTrakcie = true;
    setTimeout(function() {
        try {
            var emailInput = document.querySelector('#access_email');
            if (!emailInput) {
                window.__sklepikEmailWTrakcie = false;
                if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'email', sukces: false, info: 'EMAIL_FIELD_NOT_FOUND' });
                return;
            }
            emailInput.value = "$bezpieczny";
            emailInput.dispatchEvent(new Event('input', { bubbles: true }));
            emailInput.dispatchEvent(new Event('change', { bubbles: true }));
            var btn = document.querySelector('.action.submit.primary.btn-send-token') ||
                document.querySelector('button.action.submit.primary') ||
                document.querySelector('button[type="submit"]') ||
                document.querySelector('.btn-send-token');
            if (!btn) {
                window.__sklepikEmailWTrakcie = false;
                if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'email', sukces: false, info: 'SUBMIT_BUTTON_NOT_FOUND' });
                return;
            }
            var checkCount = 0;
            var checkReady = setInterval(function() {
                checkCount++;
                var isBtnReady = btn && !btn.disabled && !btn.classList.contains('disabled');
                var isCaptchaLoaded = document.querySelector('textarea[name="g-recaptcha-response"]') ||
                    document.querySelector('.grecaptcha-badge');
                if (checkCount < 8) return;
                if ((isBtnReady && isCaptchaLoaded) || checkCount >= 28) {
                    clearInterval(checkReady);
                    setTimeout(function() {
                        window.__sklepikEmailWyslany = true;
                        btn.focus();
                        btn.click();
                        window.__sklepikEmailWTrakcie = false;
                        if (window.SklepikPost) window.SklepikPost({ type: 'EMAIL_WYNIK', wynik: 'EMAIL_SENT' });
                    }, 1500);
                }
            }, 500);
        } catch (err) {
            window.__sklepikEmailWTrakcie = false;
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'email', sukces: false, info: String(err.message) });
        }
    }, 800);
})();
true;
""".trimIndent()
    }

    fun skryptWypelnijSms(kod: String): String {
        val bezpieczny = kod.replace("\\", "\\\\").replace("\"", "\\\"")
        return """
(function() {
    if (window.__sklepikSmsWTrakcie) return;
    window.__sklepikSmsWTrakcie = true;
    try {
        var smsField = document.getElementById('access_token') ||
            document.querySelector('input[name="access_token"]') ||
            document.querySelector('input[name="token"]') ||
            document.querySelector('input[id*="token"]') ||
            document.querySelector('input[name*="token"]') ||
            document.querySelector('input[type="tel"]');
        if (!smsField) {
            window.__sklepikSmsWTrakcie = false;
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'sms', sukces: false, info: 'SMS_FIELD_NOT_FOUND' });
            return;
        }
        smsField.focus();
        smsField.value = "$bezpieczny";
        smsField.dispatchEvent(new Event('input', { bubbles: true }));
        smsField.dispatchEvent(new Event('change', { bubbles: true }));
        var submitBtn = document.querySelector('.action.submit.primary.btn-send-token') ||
            document.querySelector('button.action.submit.primary') ||
            document.querySelector('button[type="submit"]') ||
            document.querySelector('input[type="submit"]');
        if (!submitBtn) {
            window.__sklepikSmsWTrakcie = false;
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'sms', sukces: false, info: 'SMS_SUBMIT_NOT_FOUND' });
            return;
        }
        setTimeout(function() {
            submitBtn.focus();
            submitBtn.click();
            window.__sklepikSmsWTrakcie = false;
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'sms', sukces: true, info: 'SMS_CODE_INJECTED' });
        }, 1200);
    } catch (err) {
        window.__sklepikSmsWTrakcie = false;
        if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'sms', sukces: false, info: String(err.message) });
    }
})();
true;
""".trimIndent()
    }

    fun skryptWypelnijHaslo(haslo: String, email: String = ""): String {
        val bezpieczny = haslo.replace("\\", "\\\\").replace("\"", "\\\"")
        val bezpiecznyEmail = email.replace("\\", "\\\\").replace("\"", "\\\"")
        return """
(function() {
    if (window.jQuery && !window.jQuery.fn.validation) {
        window.jQuery.fn.validation = function() { return { valid: function() { return true; } }; };
    }
    // Pozwól na retry gdy poprzednia próba utknęła na tej samej stronie
    if (window.isProcessingLogin && window.__sklepikHasloTs &&
        (Date.now() - window.__sklepikHasloTs) < 12000) return;
    window.isProcessingLogin = true;
    window.__sklepikHasloTs = Date.now();

    function post(info) {
        if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: true, info: info });
    }
    function zaznaczJeden(gdpr) {
        if (!gdpr) return false;
        if (gdpr.checked) return true;
        try {
            gdpr.focus();
            gdpr.click();
        } catch (e) {}
        try {
            gdpr.dispatchEvent(new MouseEvent('click', { view: window, bubbles: true, cancelable: true }));
        } catch (e) {}
        gdpr.checked = true;
        try { gdpr.setAttribute('checked', 'checked'); } catch (e) {}
        gdpr.dispatchEvent(new Event('input', { bubbles: true }));
        gdpr.dispatchEvent(new Event('change', { bubbles: true }));
        if (window.jQuery) {
            try { window.jQuery(gdpr).prop('checked', true).trigger('change').trigger('click'); } catch (e) {}
        }
        if (gdpr.id) {
            var label = document.querySelector('label[for="' + gdpr.id + '"]');
            if (label) try { label.click(); } catch (e) {}
        }
        var parentLabel = gdpr.closest('label');
        if (parentLabel) try { parentLabel.click(); } catch (e) {}
        return !!gdpr.checked;
    }
    function zaznaczCheckbox() {
        var ok = false;
        // Unilever: zgoda to często HIDDEN input value=0 → ustaw 1
        var ukryte = document.querySelectorAll(
            'input[name="amgdpr_agreement[statute_ap]"], input[name*="amgdpr_agreement"], input[type="hidden"][name*="gdpr"], input[type="hidden"][name*="statute"]'
        );
        for (var h = 0; h < ukryte.length; h++) {
            try {
                ukryte[h].value = '1';
                ukryte[h].setAttribute('value', '1');
                ukryte[h].dispatchEvent(new Event('input', { bubbles: true }));
                ukryte[h].dispatchEvent(new Event('change', { bubbles: true }));
                if (window.jQuery) {
                    try { window.jQuery(ukryte[h]).val('1').trigger('change'); } catch (e) {}
                }
                ok = true;
            } catch (e) {}
        }
        var selektory = [
            'input[type="checkbox"][id^="amprivacy-checkbox-"]',
            'input[type="checkbox"][name*="amgdpr_agreement"]',
            'input[type="checkbox"][name*="gdpr"]',
            'input[type="checkbox"][name*="privacy"]',
            'input[type="checkbox"][name*="statute"]',
            'input[type="checkbox"][id*="privacy"]',
            'input[type="checkbox"][id*="amprivacy"]',
            '.amprivacy-checkbox input[type="checkbox"]',
            '.amgdpr-form input[type="checkbox"]',
            'form#login-form input[type="checkbox"]',
            'form.form-login input[type="checkbox"]',
            '.form-customer-login input[type="checkbox"]',
            '#customer-login-form input[type="checkbox"]'
        ];
        for (var s = 0; s < selektory.length; s++) {
            var lista = document.querySelectorAll(selektory[s]);
            for (var i = 0; i < lista.length; i++) {
                if (zaznaczJeden(lista[i])) ok = true;
            }
        }
        // Etykiety / klikalne bloki regulaminu
        var labels = document.querySelectorAll('label, .amprivacy-checkbox, .statute-info, .amgdpr-checkbox, [data-amgdpr-js]');
        for (var L = 0; L < labels.length; L++) {
            var txt = ((labels[L].textContent || '') + '').toLowerCase();
            if (txt.indexOf('polityk') >= 0 || txt.indexOf('regulamin') >= 0 ||
                txt.indexOf('statut') >= 0 || txt.indexOf('zgadzam') >= 0 ||
                txt.indexOf('akceptuj') >= 0 || txt.indexOf('prywatno') >= 0) {
                try { labels[L].click(); } catch (e) {}
                var inp = labels[L].querySelector('input[type="checkbox"]');
                if (inp && zaznaczJeden(inp)) ok = true;
            }
        }
        return ok;
    }
    function captchaGotowa() {
        var ta = document.querySelector('textarea[name="g-recaptcha-response"]');
        if (ta && ta.value && ta.value.length > 10) return true;
        if (document.querySelector('.grecaptcha-badge')) return true;
        if (document.querySelector('iframe[src*="recaptcha"]')) return true;
        if (typeof grecaptcha !== 'undefined') return true;
        return false;
    }
    function przyciskLogowania() {
        return document.getElementById('send2') ||
            document.querySelector('button.action.login.primary') ||
            document.querySelector('#login-form button.action.primary') ||
            document.querySelector('#login-form button[type="submit"]') ||
            document.querySelector('button[type="submit"].action.login');
    }

    try {
        function widoczny(el) {
            if (!el) return false;
            var st = window.getComputedStyle(el);
            if (st.display === 'none' || st.visibility === 'hidden') return false;
            var r = el.getBoundingClientRect();
            return r.width > 2 && r.height > 2;
        }
        // 0) Email na formularzu Magento login (często pusty mimo ?access-email=)
        var emailVal = "$bezpiecznyEmail";
        if (emailVal) {
            var emailLogin = document.getElementById('email') ||
                document.querySelector('input[name="login[username]"]') ||
                document.querySelector('#login-form input[type="email"]');
            if (emailLogin) {
                emailLogin.focus();
                emailLogin.value = emailVal;
                emailLogin.dispatchEvent(new Event('input', { bubbles: true }));
                emailLogin.dispatchEvent(new Event('change', { bubbles: true }));
                post('EMAIL_LOGIN_WPISANY');
            }
        }
        // 1) Hasło — widoczne pole (nie popup Magento)
        var passField = document.getElementById('pass');
        if (!widoczny(passField)) {
            passField = document.querySelector('input[name="login[password]"]');
        }
        if (!widoczny(passField)) {
            var candidates = document.querySelectorAll('input[type="password"]');
            passField = null;
            for (var pi = 0; pi < candidates.length; pi++) {
                if (widoczny(candidates[pi])) { passField = candidates[pi]; break; }
            }
        }
        // Na /customer/account/login bierz #pass nawet jeśli chwilowo „niewidoczne”
        if (!passField && (location.href || '').toLowerCase().indexOf('customer/account/login') >= 0) {
            passField = document.getElementById('pass') ||
                document.querySelector('input[name="login[password]"]') ||
                document.querySelector('input[type="password"]');
        }
        if (!passField) {
            window.isProcessingLogin = false;
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: false, info: 'PASS_FIELD_NOT_FOUND' });
            return;
        }
        passField.focus();
        passField.value = "$bezpieczny";
        passField.dispatchEvent(new Event('input', { bubbles: true }));
        passField.dispatchEvent(new Event('change', { bubbles: true }));
        post('HASLO_WPISANE');

        // 2) Potem checkbox (po krótkiej pauzie)
        setTimeout(function() {
            zaznaczCheckbox();
            post('CHECKBOX_ZAZNACZONY');

            // 3) Czekaj aż captcha się dociągnie, dopiero potem Zaloguj
            setTimeout(function() {
                var checkCount = 0;
                var trySubmit = setInterval(function() {
                    checkCount++;
                    var submitBtn = przyciskLogowania();
                    var isBtnReady = submitBtn && !submitBtn.disabled && !submitBtn.classList.contains('disabled');
                    var isCaptchaLoaded = captchaGotowa();

                    if ((isBtnReady && isCaptchaLoaded) || checkCount >= 40) {
                        clearInterval(trySubmit);
                        post('CAPTCHA_GOTOWA_CZEKAM');
                        // dodatkowa pauza po pełnym załadowaniu captcha
                        setTimeout(function() {
                            zaznaczCheckbox();
                            submitBtn = przyciskLogowania();
                            if (!submitBtn) {
                                window.isProcessingLogin = false;
                                if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: false, info: 'SEND2_NOT_FOUND' });
                                return;
                            }
                            submitBtn.focus();
                            setTimeout(function() {
                                try {
                                    submitBtn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
                                } catch (e) {}
                                submitBtn.click();
                                post('PRZYCISK_ZALOGUJ');
                                setTimeout(function() {
                                    window.isProcessingLogin = false;
                                    if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: true, info: 'HASLO_WYSLANE' });
                                }, 1200);
                                setTimeout(function() {
                                    if (document.getElementById('pass')) {
                                        var loginForm = document.getElementById('login-form') || document.getElementById('customer-login-form');
                                        if (loginForm) try { loginForm.submit(); } catch (e) {}
                                    }
                                }, 5000);
                            }, 500);
                        }, 3000);
                    }
                }, 500);
            }, 4500);
        }, 900);
    } catch (err) {
        window.isProcessingLogin = false;
        if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: false, info: String(err.message) });
    }
})();
true;
""".trimIndent()
    }
}
