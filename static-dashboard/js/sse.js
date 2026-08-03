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
  let sse = null;

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
        JSON.parse(event.data);
        // TICKET-ADV105 will render the parsed trade into #trade-feed.
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
