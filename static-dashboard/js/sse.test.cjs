const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

class FakeElement {
  constructor(feed = null) {
    this.feed = feed;
    this.children = [];
    this.className = '';
    this.innerHTML = '';
    this.textContent = '';
    this.classList = {
      remove: (name) => {
        this.className = this.className
          .split(' ')
          .filter((item) => item && item !== name)
          .join(' ');
      },
    };
  }

  prepend(element) {
    element.feed = this;
    this.children.unshift(element);
  }

  get lastElementChild() {
    return this.children.at(-1);
  }

  remove() {
    this.feed.children = this.feed.children.filter((item) => item !== this);
  }
}

function loadSseScript() {
  const feed = new FakeElement();
  const status = new FakeElement();
  const timers = [];

  class FakeEventSource {
    constructor(url) {
      this.url = url;
      FakeEventSource.instance = this;
    }

    close() {}
  }

  const context = {
    console,
    document: {
      getElementById: (id) => ({ 'trade-feed': feed, 'sse-status': status })[id],
      createElement: () => new FakeElement(),
    },
    EventSource: FakeEventSource,
    Intl,
    setTimeout: (callback) => timers.push(callback),
    window: {
      location: { hostname: 'localhost', port: '5500' },
      addEventListener() {},
    },
  };
  const source = fs.readFileSync(path.join(__dirname, 'sse.js'), 'utf8');
  vm.runInNewContext(source, context);

  return { feed, source: FakeEventSource.instance, timers };
}

test('prepends safe formatted trade cards, removes animation marker, and caps feed at 50', () => {
  const fixture = loadSseScript();

  for (let index = 0; index < 51; index += 1) {
    fixture.source.onmessage({
      data: JSON.stringify({
        tradeRef: index === 50 ? '<script>alert(1)</script>' : `TRD-${index}`,
        instrumentSymbol: 'SAP&DE',
        quantity: 1000,
        price: 125.5,
        currency: 'EUR',
        status: index === 50 ? 'UNMATCHED' : 'MATCHED',
      }),
    });
  }

  assert.equal(fixture.feed.children.length, 50);
  assert.match(fixture.feed.children[0].className, /trade-card--break/);
  assert.match(fixture.feed.children[0].className, /trade-card--new/);
  assert.match(fixture.feed.children[0].innerHTML, /&lt;script&gt;alert\(1\)&lt;\/script&gt;/);
  assert.match(fixture.feed.children[0].innerHTML, /SAP&amp;DE/);
  assert.match(fixture.feed.children[0].innerHTML, /qty=1,000/);
  assert.match(fixture.feed.children[0].innerHTML, /price=125\.50/);
  assert.doesNotMatch(fixture.feed.children.at(-1).innerHTML, /TRD-0/);

  fixture.timers.forEach((callback) => callback());
  assert.doesNotMatch(fixture.feed.children[0].className, /trade-card--new/);
});
