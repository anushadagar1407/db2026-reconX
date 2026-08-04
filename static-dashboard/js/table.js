// File: static-dashboard/js/trades.js — TICKET-ADV106 sort + resize
(function () {
  const table = document.getElementById('trades-table');
  const tbody = document.getElementById('trades-tbody');
  let rows = [
    { tradeRef: 'EQU-20260603-0001', symbol: 'SAP.DE',  quantity: 1000, price: 125.50, status: 'MATCHED' },
    { tradeRef: 'FX-20260603-0001',  symbol: 'EUR/USD', quantity: 1_000_000, price: 1.0852, status: 'PENDING' },
    { tradeRef: 'EQU-20260603-0002', symbol: 'AAPL',    quantity: 500,  price: 178.20, status: 'BREAK' },
  ]; // canonical data — sort operates on this

  // ---------- sortable columns ----------
  table.querySelectorAll('thead th').forEach(th => {
    th.addEventListener('click', (e) => {
      if (e.target.classList.contains('resize-handle')) return; // ignore resize clicks
      const col = th.dataset.col;
      const type = th.dataset.type || 'string';
      const dir = th.getAttribute('aria-sort') === 'ascending' ? 'descending' : 'ascending';

      // clear all, set this one
      table.querySelectorAll('thead th').forEach(o => o.removeAttribute('aria-sort'));
      th.setAttribute('aria-sort', dir);

      const mult = dir === 'ascending' ? 1 : -1;
      rows.sort((a, b) => {
        const av = a[col], bv = b[col];
        if (type === 'number') return (Number(av) - Number(bv)) * mult;
        return String(av).localeCompare(String(bv)) * mult;
      });
      renderRows();
    });
  });

  // ---------- resizable columns ----------
  table.querySelectorAll('.resize-handle').forEach(handle => {
    handle.addEventListener('mousedown', (e) => {
      e.preventDefault();
      const th = handle.closest('th');
      const startX = e.clientX;
      const startWidth = th.offsetWidth;

      // Listen on DOCUMENT so the drag survives leaving the handle.
      function onMove(ev) { th.style.width = (startWidth + ev.clientX - startX) + 'px'; }
      function onUp()     { document.removeEventListener('mousemove', onMove);
                            document.removeEventListener('mouseup', onUp); }
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  });

  function renderRows() {
    tbody.innerHTML = rows.map(r => `
      <tr>
        <td>${r.tradeRef}</td><td>${r.symbol}</td>
        <td>${r.quantity}</td><td>${r.price}</td>
        <td>${r.status}</td>
      </tr>`).join('');
  }

  // initial load — hits the REST API from Day 5
  // fetch('/api/v1/trades?size=200')
  //   .then(r => r.json())
  //   .then(data => { rows = data.content || data;});
  renderRows(); 
})();