INDEX_HTML = """
<!doctype html>
<html lang='fr'>
<head>
  <meta charset='utf-8' />
  <meta name='viewport' content='width=device-width,initial-scale=1' />
  <title>WorldTrader - Simulateur de marché</title>
  <script src='https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js'></script>
  <style>
    body{margin:0;background:#0d1323;color:#eaf0ff;font-family:Arial}
    .layout{display:grid;grid-template-columns:280px 1fr 340px;gap:8px;height:100vh;padding:8px}
    .panel{border:1px solid #2b3750;background:#141d33;border-radius:10px;padding:10px;overflow:auto}
    button,input,select{width:100%;margin:4px 0;padding:8px;border-radius:6px;border:1px solid #384869;background:#0f172a;color:#fff}
    .row{display:flex;gap:6px}.row button{flex:1}
    .watch{display:grid;grid-template-columns:1fr 1fr;gap:6px;font-size:12px}
    .metric-grid{display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-top:8px}
    .metric{background:#0f172a;border:1px solid #2e3d5d;border-radius:8px;padding:8px}
    .label{display:block;color:#96a8c9;font-size:11px;margin-bottom:4px}
    .value{font-weight:bold}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{padding:4px 6px;border-bottom:1px solid #26324a;text-align:right}
    th:first-child,td:first-child{text-align:left}
    .asks td{color:#ff8585}
    .bids td{color:#7be39b}
    #ticks{font-size:12px;max-height:180px;overflow:auto;background:#0f172a;border:1px solid #2e3d5d;border-radius:8px;padding:8px}
    #notif{font-size:12px;padding:8px;background:#0f172a;border:1px solid #2e3d5d;border-radius:8px;min-height:20px}
    #legend{font-size:12px;color:#9db0d3;margin-top:6px}
  </style>
</head>
<body>
<div class='layout'>
  <div class='panel'>
    <h3>Marchés</h3>
    <select id='asset'><option>AAPL</option><option>BTC</option><option>GOLD</option><option>OIL</option></select>
    <h4>Watchlist</h4>
    <div class='watch'><div>AAPL</div><div>BTC</div><div>GOLD</div><div>OIL</div></div>

    <h4>Données marché</h4>
    <div class='metric-grid'>
      <div class='metric'><span class='label'>Meilleure offre (bid)</span><span id='bestBid' class='value'>-</span></div>
      <div class='metric'><span class='label'>Meilleure demande (ask)</span><span id='bestAsk' class='value'>-</span></div>
      <div class='metric'><span class='label'>Prix médian</span><span id='mid' class='value'>-</span></div>
      <div class='metric'><span class='label'>Spread</span><span id='spread' class='value'>-</span></div>
      <div class='metric'><span class='label'>Dernier prix</span><span id='lastPrice' class='value'>-</span></div>
      <div class='metric'><span class='label'>Dernier volume</span><span id='lastVolume' class='value'>-</span></div>
    </div>
    <h4>Régime</h4>
    <div id='regime' class='metric'>-</div>
  </div>

  <div class='panel'>
    <h3>Graphique en temps réel</h3>
    <div id='chart' style='height:470px'></div>
    <div id='legend'>OHLC</div>

    <h4>Carnet d'ordres</h4>
    <table>
      <thead><tr><th>Type</th><th>Prix</th><th>Quantité</th></tr></thead>
      <tbody id='bookRows'></tbody>
    </table>
  </div>

  <div class='panel'>
    <h3>Ticket d'ordre</h3>
    <div class='row'><button onclick="setSide('BUY')">Acheter</button><button onclick="setSide('SELL')">Vendre</button></div>
    <select id='otype'><option value='MARKET'>Ordre marché</option><option value='LIMIT'>Ordre limite</option></select>
    <input id='qty' type='number' value='1' placeholder='Quantité'/>
    <input id='price' type='number' step='0.01' placeholder='Prix limite'/>
    <button onclick='envoyerOrdre()'>Envoyer ordre</button>
    <input id='cancel' placeholder='ID ordre à annuler'/>
    <button onclick='annulerOrdre()'>Annuler ordre</button>

    <h4>Portefeuille joueur</h4>
    <div class='metric-grid'>
      <div class='metric'><span class='label'>Solde</span><span id='solde' class='value'>-</span></div>
      <div class='metric'><span class='label'>Profit / perte</span><span id='pnl' class='value'>-</span></div>
    </div>
    <div id='positions' class='metric' style='margin-top:8px'>Positions ouvertes: -</div>

    <h4>Historique des ticks</h4>
    <div id='ticks'></div>
    <h4>Notifications</h4>
    <div id='notif'></div>
  </div>
</div>

<script>
let side='BUY';
const chart = LightweightCharts.createChart(document.getElementById('chart'), {layout:{background:{color:'#141d33'},textColor:'#dfe8ff'}});
const candles = chart.addCandlestickSeries();
const volumes = chart.addHistogramSeries({priceScaleId:'', priceFormat:{type:'volume'}});
const bars=[];
const tickLog=[];
let currentAsset='AAPL';

function fmt(v){ return (v===null || v===undefined) ? '-' : Number(v).toFixed(2); }
function setSide(s){ side=s; document.getElementById('notif').textContent='Direction sélectionnée: '+(s==='BUY'?'Acheter':'Vendre'); }

function renderTicks(){
  const rows = tickLog.slice(-12).reverse().map(t => `<div>${new Date(t.ts).toLocaleTimeString()} - ${t.symbol}: ${fmt(t.price)} (${t.size})</div>`);
  document.getElementById('ticks').innerHTML = rows.join('') || 'Aucun tick reçu';
}

function renderBook(book){
  const body = document.getElementById('bookRows');
  const asks = (book.asks || []).slice(0, 6).map(([p,q]) => `<tr class='asks'><td>Vente</td><td>${fmt(p)}</td><td>${q}</td></tr>`).join('');
  const bids = (book.bids || []).slice(0, 6).map(([p,q]) => `<tr class='bids'><td>Achat</td><td>${fmt(p)}</td><td>${q}</td></tr>`).join('');
  body.innerHTML = asks + bids;
}

async function refresh(){
  currentAsset = document.getElementById('asset').value;
  const [m,b,p]=await Promise.all([
    fetch('/marche').then(r=>r.json()),
    fetch('/orderbook?asset='+currentAsset).then(r=>r.json()),
    fetch('/portefeuille').then(r=>r.json())
  ]);

  const asset = m.actifs[currentAsset] || {};
  document.getElementById('bestBid').textContent = fmt(asset.best_bid);
  document.getElementById('bestAsk').textContent = fmt(asset.best_ask);
  document.getElementById('mid').textContent = fmt(asset.mid);
  document.getElementById('spread').textContent = fmt(asset.spread);
  document.getElementById('regime').textContent = asset.regime || '-';

  document.getElementById('solde').textContent = fmt(p.solde);
  document.getElementById('pnl').textContent = fmt(p.profit_perte);
  const pos = Object.entries(p.positions_ouvertes || {}).map(([s,q]) => `${s}: ${q}`).join(' | ');
  document.getElementById('positions').textContent = 'Positions ouvertes: ' + (pos || 'Aucune');

  renderBook(b);
}

async function envoyerOrdre(){
  const payload={asset:currentAsset, side, taille:Number(document.getElementById('qty').value), type_ordre:document.getElementById('otype').value, prix_limite: document.getElementById('price').value?Number(document.getElementById('price').value):null};
  const r=await fetch('/ordre',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}).then(r=>r.json());
  document.getElementById('notif').textContent = 'Ordre envoyé: '+r.order_id;
}

async function annulerOrdre(){
  const oid=document.getElementById('cancel').value;
  const r=await fetch('/annuler',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({order_id:oid})}).then(r=>r.json());
  document.getElementById('notif').textContent = r.message;
}

const ws = new WebSocket((location.protocol==='https:'?'wss':'ws')+'://'+location.host+'/ws/ticks');
ws.onmessage=(event)=>{
  const t=JSON.parse(event.data);
  const symbol = t.symbol || t.asset;
  const ts = t.ts || Math.floor((t.time || Date.now()/1000) * 1000);
  const px = t.price;
  const sz = t.size ?? t.volume ?? 0;
  if(symbol===currentAsset){
    const time=Math.floor(ts/1000);
    const prev=bars[bars.length-1];
    if(!prev || time>prev.time){
      const bar={time,open:px,high:px,low:px,close:px,volume:sz};
      bars.push(bar); candles.update(bar); volumes.update({time,value:sz,color:'#2e8bff88'});
    }else{
      prev.high=Math.max(prev.high,px); prev.low=Math.min(prev.low,px); prev.close=px; prev.volume+=sz;
      candles.update(prev); volumes.update({time:prev.time,value:prev.volume,color:'#2e8bff88'});
    }
    document.getElementById('lastPrice').textContent = fmt(px);
    document.getElementById('lastVolume').textContent = sz;
    document.getElementById('legend').textContent = `O ${fmt(bars[bars.length-1].open)} H ${fmt(bars[bars.length-1].high)} L ${fmt(bars[bars.length-1].low)} C ${fmt(bars[bars.length-1].close)} V ${bars[bars.length-1].volume}`;
  }
  tickLog.push({symbol, ts, price:px, size:sz});
  renderTicks();
};

document.getElementById('asset').addEventListener('change', () => {
  bars.length = 0;
  candles.setData([]);
  volumes.setData([]);
  document.getElementById('lastPrice').textContent = '-';
  document.getElementById('lastVolume').textContent = '-';
});

setInterval(refresh, 600);
refresh();
</script>
</body></html>
"""
