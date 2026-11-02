package shopunilever.dominik.pl.sklep

/**
 * Skrypty JS wstrzykiwane do WebView — port z Harmonogramu (webview-skrypty).
 */
object SkryptySklepu {

    const val MOSTEK = """
(function() {
    if (window.SklepikPost) return;
    window.SklepikPost = function(obj) {
        try {
            if (window.AndroidBridge && window.AndroidBridge.onWiadomosc) {
                window.AndroidBridge.onWiadomosc(JSON.stringify(obj));
            }
        } catch (e) {}
    };
    true;
})();
"""

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
    try {
        var smsField = document.querySelector('#access_token') ||
            document.querySelector('input[name="access_token"]') ||
            document.querySelector('input[type="tel"]') ||
            document.querySelector('input[name*="token"]');
        if (!smsField) {
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'sms', sukces: false, info: 'SMS_FIELD_NOT_FOUND' });
            return;
        }
        smsField.value = "$bezpieczny";
        smsField.dispatchEvent(new Event('input', { bubbles: true }));
        smsField.dispatchEvent(new Event('change', { bubbles: true }));
        var submitBtn = document.querySelector('.action.submit.primary.btn-send-token') ||
            document.querySelector('button.action.submit.primary') ||
            document.querySelector('button[type="submit"]');
        if (!submitBtn) {
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'sms', sukces: false, info: 'SMS_SUBMIT_NOT_FOUND' });
            return;
        }
        setTimeout(function() {
            submitBtn.click();
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'sms', sukces: true, info: 'SMS_CODE_INJECTED' });
        }, 1200);
    } catch (err) {
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
    if (window.isProcessingLogin) return;
    window.isProcessingLogin = true;
    try {
        var passField = document.getElementById('pass');
        if (!passField) {
            window.isProcessingLogin = false;
            if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: false, info: 'PASS_FIELD_NOT_FOUND' });
            return;
        }
        passField.value = "$bezpieczny";
        passField.dispatchEvent(new Event('input', { bubbles: true }));
        passField.dispatchEvent(new Event('change', { bubbles: true }));
        var gdpr = document.querySelector('input[id^="amprivacy-checkbox-"]') ||
            document.querySelector('input[name="amgdpr_agreement[statute_ap]"]');
        if (gdpr && !gdpr.checked) {
            gdpr.checked = true;
            gdpr.dispatchEvent(new Event('change', { bubbles: true }));
        }
        setTimeout(function() {
            var submitBtn = document.getElementById('send2');
            if (submitBtn) {
                submitBtn.click();
                window.isProcessingLogin = false;
                if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: true });
            } else {
                window.isProcessingLogin = false;
                if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: false, info: 'SEND2_NOT_FOUND' });
            }
        }, 2500);
    } catch (err) {
        window.isProcessingLogin = false;
        if (window.SklepikPost) window.SklepikPost({ type: 'KROK', krok: 'haslo', sukces: false, info: String(err.message) });
    }
})();
true;
""".trimIndent()
    }
}
