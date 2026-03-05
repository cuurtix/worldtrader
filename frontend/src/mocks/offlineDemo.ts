export function installOfflineDemo() {
  const sampleTrades = Array.from({ length: 240 }, (_, i) => ({
    tradeId: String(i + 1),
    ticker: 'AAPL',
    price: 190 + Math.sin(i / 9) * 2 + (Math.random() - 0.5),
    qty: 1 + (i % 8),
    timestamp: new Date(Date.now() - (240 - i) * 1000).toISOString()
  }));

  const originalFetch = window.fetch.bind(window);
  window.fetch = async (input, init) => {
    const url = String(input);
    if (url.includes('/api/v1/trades/')) return new Response(JSON.stringify(sampleTrades), { status: 200 });
    if (url.includes('/api/v1/portfolio/')) return new Response(JSON.stringify({ traderId: 'retail_player', cash: 1_000_000, realizedPnl: 0, unrealizedPnl: 0, positions: [] }), { status: 200 });
    if (url.includes('/api/v1/orders') || url.includes('/api/v1/regime')) return new Response('{}', { status: 200 });
    return originalFetch(input, init);
  };
}
