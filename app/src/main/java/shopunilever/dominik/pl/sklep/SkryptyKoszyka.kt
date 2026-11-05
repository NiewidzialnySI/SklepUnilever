package shopunilever.dominik.pl.sklep

object SkryptyKoszyka {

    const val EKSPORT = """
(function() {
    function tekst(el) {
        return ((el && (el.textContent || el.innerText)) || '').replace(/\s+/g, ' ').trim();
    }
    function zbierz() {
        var pozycje = [];
        var selektory = [
            '.cart.item',
            '#shopping-cart-table tbody tr',
            '.minicart-items .product-item',
            '.minicart-items-wrapper .product-item',
            '.block-minicart .product-item'
        ];
        var nodes = [];
        for (var s = 0; s < selektory.length; s++) {
            var found = document.querySelectorAll(selektory[s]);
            if (found && found.length) { nodes = found; break; }
        }
        for (var i = 0; i < nodes.length; i++) {
            var n = nodes[i];
            var a = n.querySelector('a.product-item-photo, a.product-item-link, strong.product-item-name a, .product-item-name a, td.col.item a');
            var nazwa = tekst(n.querySelector('.product-item-name, strong.product-item-name, .product.name a, .col.item .product-item-name')) ||
                tekst(a) || ('Produkt ' + (i + 1));
            var qtyEl = n.querySelector('input.qty, input[data-role="cart-item-qty"], .item-qty, select.qty');
            var ilosc = 1;
            if (qtyEl) {
                var v = qtyEl.value || tekst(qtyEl);
                var parsed = parseInt(v, 10);
                if (!isNaN(parsed) && parsed > 0) ilosc = parsed;
            }
            var cena = tekst(n.querySelector('.price, .minicart-price, .col.price .price, [data-th="Price"] .price'));
            var sku = (n.getAttribute('data-product-sku') || n.getAttribute('data-cart-item-id') || '');
            var url = a && a.href ? a.href : '';
            if (nazwa) pozycje.push({ nazwa: nazwa, sku: sku, ilosc: ilosc, url: url, cena: cena });
        }
        if (window.SklepikPost) {
            window.SklepikPost({ type: 'KOSZYK_EXPORT', pozycje: pozycje, url: location.href });
        }
    }
    // Jeśli nie jesteśmy na koszyku — otwórz mini-cart / stronę koszyka i spróbuj ponownie
    var naKoszyku = location.href.indexOf('/checkout/cart') >= 0;
    if (!naKoszyku) {
        var mini = document.querySelector('[data-block="minicart"] a.showcart, .minicart-wrapper a.action.showcart, a.action.showcart');
        if (mini) try { mini.click(); } catch (e) {}
        setTimeout(zbierz, 700);
        setTimeout(function() {
            if (!document.querySelector('.minicart-items .product-item, .cart.item')) {
                if (window.SklepikPost) window.SklepikPost({ type: 'KOSZYK_EXPORT', pozycje: [], info: 'EMPTY_OR_NEED_CART_PAGE', url: location.href });
            }
        }, 1400);
    } else {
        zbierz();
    }
})();
true;
"""

    fun skryptDodajProdukt(url: String): String {
        val bezpieczny = url.replace("\\", "\\\\").replace("\"", "\\\"")
        return """window.location.href="$bezpieczny"; true;"""
    }

    /**
     * Po wejściu na kartę produktu — czeka aż DOM/Magento domknie formularz,
     * dopiero wtedy jedno kliknięcie „Dodaj do koszyka” (bez strzału w pustkę).
     */
    const val KLIKNIJ_DODAJ_DO_KOSZYKA = """
(function() {
    if (window.__sklepikDodajeTeraz) {
        if (window.SklepikPost) window.SklepikPost({ type: 'KOSZYK_DODANO', sukces: true, info: 'SKIP_DUP' });
        return true;
    }
    window.__sklepikDodajeTeraz = true;
    var start = Date.now();
    var maxMs = 12000;
    var gotowyOd = 0;
    var minStabilnyMs = 450;

    function post(sukces, info) {
        if (window.SklepikPost) window.SklepikPost({ type: 'KOSZYK_DODANO', sukces: !!sukces, info: info || '' });
    }
    function resetFlag() {
        window.__sklepikDodajeTeraz = false;
    }
    function widoczny(el) {
        if (!el) return false;
        if (el.disabled) return false;
        if (el.getAttribute && el.getAttribute('disabled') != null) return false;
        if (el.classList && (el.classList.contains('disabled') || el.classList.contains('loading'))) return false;
        var st = window.getComputedStyle ? window.getComputedStyle(el) : null;
        if (st && (st.visibility === 'hidden' || st.display === 'none' || st.opacity === '0')) return false;
        return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
    }
    function znajdzPrzycisk() {
        var btn = document.querySelector('#product-addtocart-button') ||
            document.querySelector('form#product_addtocart_form button.action.tocart') ||
            document.querySelector('button.action.tocart#product-addtocart-button') ||
            document.querySelector('button[data-role="tocart"]');
        if (!btn) {
            var all = document.querySelectorAll('button.action.tocart, button[type="submit"]');
            for (var i = 0; i < all.length; i++) {
                var t = ((all[i].textContent || '') + '').toLowerCase().replace(/\s+/g, ' ').trim();
                if (t.indexOf('dodaj do koszyka') >= 0 || t.indexOf('add to cart') >= 0) {
                    btn = all[i];
                    break;
                }
            }
        }
        return btn;
    }
    function stronaProduktuGotowa() {
        var form = document.querySelector('form#product_addtocart_form, form[data-role="tocart-form"]');
        var btn = znajdzPrzycisk();
        if (!btn || !widoczny(btn)) return null;
        // Magento: przycisk bywa w DOM zanim widget catalogAddToCart się podłączy
        if (!form && !document.querySelector('.product-info-main, .product-add-form, #product_addtocart_form')) {
            return null;
        }
        if (document.readyState !== 'complete' && document.readyState !== 'interactive') return null;
        return btn;
    }
    function niedostepnyBezFormy() {
        var url = (location.href || '').toLowerCase();
        var tekst = ((document.body && (document.body.innerText || document.body.textContent)) || '').toLowerCase();
        var maForme = !!document.querySelector('#product-addtocart-button, form#product_addtocart_form, form[data-role="tocart-form"]');
        if (maForme) return null;
        if (url.indexOf('/noroute') >= 0 || url.indexOf('404') >= 0) return 'STRONA_404';
        if (!!document.querySelector('.stock.unavailable, .out-of-stock, .product-info-stock-sku .unavailable')) {
            return 'BRAK_PRODUKTU';
        }
        if (/nie\s*znaleziono|strona nie istnieje|out of stock|produkt.*niedost/.test(tekst)) {
            return 'BRAK_PRODUKTU';
        }
        return null;
    }
    function kliknijRaz(el) {
        try {
            el.focus();
            el.click();
            return true;
        } catch (e) {
            try {
                var ev = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                el.dispatchEvent(ev);
                return true;
            } catch (e2) { return false; }
        }
    }
    function tick() {
        if (!window.__sklepikDodajeTeraz) return;
        var brak = niedostepnyBezFormy();
        if (brak) {
            resetFlag();
            post(false, brak);
            return;
        }
        var btn = stronaProduktuGotowa();
        if (btn) {
            if (!gotowyOd) gotowyOd = Date.now();
            if (Date.now() - gotowyOd >= minStabilnyMs) {
                if (kliknijRaz(btn)) {
                    setTimeout(resetFlag, 2800);
                    post(true, 'OK');
                    return;
                }
            }
        } else {
            gotowyOd = 0;
        }
        if (Date.now() - start > maxMs) {
            resetFlag();
            post(false, znajdzPrzycisk() ? 'BRAK_PRODUKTU' : 'BRAK_PRZYCISKU');
            return;
        }
        setTimeout(tick, 350);
    }
    setTimeout(tick, 200);
    true;
})();
"""

    const val URL_KOSZYKA = "https://sklepfirmowyunilever.pl/checkout/cart/"
}
