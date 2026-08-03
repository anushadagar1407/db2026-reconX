// TICKET-ADV104 — EventSource subscription and connection-state badge.
(function () {
  const feed = document.getElementById('trade-feed');
  const status = document.getElementById('sse-status');
  if (!feed || !status) return;

  // python -m http.server has no reverse proxy. Keep production same-origin,
  // but point the documented localhost:5500 development server at Spring Boot.
  const streamUrl = window.location.hostname === 'localhost' && window.location.port === '5500'
    ? 'http://localhost:8080/api/v1/trades/stream'
    : '/api/v1/trades/stream';
  const maxFeedEntries = 50;
  const formatQty = new Intl.NumberFormat('en-US');
  const formatPrice = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  });
  let sse = null;

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, function (character) {
      return {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
      }[character];
    });
  }

  function prependTradeRow(trade) {
    const statusValue = String(trade.status ?? '').toUpperCase();
    const statusModifier = {
      MATCHED: 'trade-card--matched',
      UNMATCHED: 'trade-card--break',
    }[statusValue];
    const symbol = trade.instrumentSymbol ?? trade.symbol;
    const quantity = trade.quantity ?? trade.qty;
    const classes = ['trade-card', statusModifier, 'trade-card--new'].filter(Boolean);
    const row = document.createElement('article');

    row.className = classes.join(' ');
    row.innerHTML = `
      <header class="trade-card__header">
        <strong>${escapeHtml(trade.tradeRef)}</strong>
        <span>${escapeHtml(statusValue)}</span>
      </header>
      <div class="trade-card__body">
        <span>${escapeHtml(symbol)}</span>
        <span>qty=${escapeHtml(formatQty.format(quantity))}</span>
        <span>price=${escapeHtml(formatPrice.format(trade.price))}</span>
        <span>${escapeHtml(trade.currency)}</span>
      </div>`;

    feed.prepend(row);
    setTimeout(function () {
      row.classList.remove('trade-card--new');
    }, 500);

    while (feed.children.length > maxFeedEntries) {
      feed.lastElementChild.remove();
    }
  }

  function updateConnectionBadge(text, variant) {
    status.textContent = text;
    status.className = `sse-status sse-status--${variant}`;
  }

  function connect() {
    sse = new EventSource(streamUrl);

    sse.onopen = function () {
      updateConnectionBadge('Live', 'live');
    };

    sse.onmessage = function (event) {
      try {
        prependTradeRow(JSON.parse(event.data));
      } catch (error) {
        console.warn('Ignored malformed trade-stream event', error);
      }
    };

    sse.onerror = function () {
      updateConnectionBadge('Reconnecting…', 'reconnecting');
      // EventSource reconnects automatically. Do not call connect() here.
    };
  }

  window.addEventListener('beforeunload', function () {
    sse?.close();
  });

  connect();
})();
