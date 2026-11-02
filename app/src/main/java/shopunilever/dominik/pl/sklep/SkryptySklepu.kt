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

    /** Wykrycie etapu formularza: email / SMS / hasło / już zalogowany */
    const val WYKRYJ = """
(function() {
    try {
        var tokenField = document.getElementById('access_token') ||
            document.querySelector('input[name="token"]') ||
            document.querySelector('input[name="access_token"]') ||
            document.querySelector('input[id*="token"]') ||
            document.querySelector('input[name*="token"]');
        if (tokenField && tokenField.offsetParent !== null) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'SMS_TOKEN_FIELD' });
            return;
        }
        var emailField = document.getElementById('access_email');
        if (emailField && emailField.offsetParent !== null) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'EMAIL_FIELD' });
            return;
        }
        var passwordField = document.getElementById('pass') ||
            document.querySelector('input[type="password"]#pass') ||
            document.querySelector('input[name="login[password]"]') ||
            document.querySelector('input[type="password"]');
        if (passwordField && passwordField.offsetParent !== null) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'PASSWORD_FIELD' });
            return;
        }
        var logoutLink = document.querySelector('a[href*="logout"], a[href*="customer/account/logout"]');
        if (logoutLink) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'ALREADY_LOGGED_IN' });
            return;
        }
        if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'NO_RECOGNIZED_ELEMENTS' });
    } catch (e) {
        if (window.SklepikLog) window.SklepikLog('wykryj: ' + e.message);
    }
})();
true;
"""

    const val SESJA = """
(function() {
    try {
        var logoutLink = document.querySelector('a[href*="logout"], a[href*="customer/account/logout"]');
        var accountLink = document.querySelector('.header.links li.welcome, .customer-welcome, .authorization-link');
        var loginLink = document.querySelector('a[href*="login"], a[href*="access-email"], a[href*="/access"]');
        var emailField = document.querySelector('#access_email');
        var passField = document.querySelector('#pass');
        if (logoutLink || (accountLink && accountLink.offsetParent !== null)) {
            if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'LOGGED_IN' });
        } else if (passField && passField.offsetParent !== null) {
            if (window.SklepikPost) window.SklepikPost({ type: 'WYKRYJ', wynik: 'PASSWORD_FIELD' });
        } else if (loginLink || emailField) {
            if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'NEEDS_LOGIN' });
        } else {
            if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'UNKNOWN' });
        }
    } catch (e) {
        if (window.SklepikPost) window.SklepikPost({ type: 'SESJA', wynik: 'UNKNOWN' });
    }
})();
true;
"""

    fun skryptPrzekieruj(url: String): String {
        val bezpieczny = url.replace("\\", "\\\\").replace("\"", "\\\"")
        return """window.location.href="$bezpieczny"; true;"""
    }

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

    fun skryptWypelnijHaslo(haslo: String): String {
        val bezpieczny = haslo.replace("\\", "\\\\").replace("\"", "\\\"")
        return """
(function() {
    if (window.jQuery && !window.jQuery.fn.validation) {
        window.jQuery.fn.validation = function() { return { valid: function() { return true; } }; };
    }
    if (window.isProcessingLogin) return;
    window.isProcessingLogin = true;

    function post(info) {
        if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: true, info: info });
    }
    function znajdzCheckbox() {
        return document.querySelector('input[id^="amprivacy-checkbox-"]') ||
            document.querySelector('input[name="amgdpr_agreement[statute_ap]"]') ||
            document.querySelector('input[type="checkbox"][name*="gdpr"]') ||
            document.querySelector('input[type="checkbox"][name*="privacy"]') ||
            document.querySelector('input[type="checkbox"][id*="privacy"]') ||
            document.querySelector('.amprivacy-checkbox input[type="checkbox"]') ||
            document.querySelector('form#login-form input[type="checkbox"]');
    }
    function zaznaczCheckbox() {
        var gdpr = znajdzCheckbox();
        if (!gdpr || gdpr.checked) return !!gdpr;
        try {
            gdpr.dispatchEvent(new MouseEvent('click', { view: window, bubbles: true, cancelable: true }));
        } catch (e) {}
        gdpr.checked = true;
        gdpr.dispatchEvent(new Event('input', { bubbles: true }));
        gdpr.dispatchEvent(new Event('change', { bubbles: true }));
        if (gdpr.id) {
            var label = document.querySelector('label[for="' + gdpr.id + '"]');
            if (label) try { label.click(); } catch (e) {}
        }
        return true;
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
        // 1) Najpierw hasło
        var passField = document.getElementById('pass') ||
            document.querySelector('input[name="login[password]"]') ||
            document.querySelector('input[type="password"]');
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
