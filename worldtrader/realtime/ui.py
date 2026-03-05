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
    .layout{display:grid;grid-template-columns:260px 1fr 330px;gap:8px;height:100vh;padding:8px}
    .panel{border:1px solid #2b3750;background:#141d33;border-radius:10px;padding:10px;overflow:auto}
    button,input,select{width:100%;margin:4px 0;padding:8px;border-radius:6px;border:1px solid #384869;background:#0f172a;color:#fff}
    .row{display:flex;gap:6px}.row button{flex:1}
  </style>
</head>
<body>
<div class='layout'>
  <div class='panel'>
    <h3>Marchés</h3>
    <select id='asset'><option>AAPL</option><option>BTC</option><option>GOLD</option><option>OIL</option></select>
    <h4>Watchlist</h4>
    <div>AAPL</div><div>BTC</div><div>GOLD</div><div>OIL</div>
    <h4>Données marché</h4>
    <pre id='marche'></pre>
  </div>
  <div class='panel'>
    <h3>Graphique en temps réel</h3>
    <div id='chart' style='height:520px'></div>
    <div id='legend'>OHLC</div>
    <h4>Carnet d'ordres</h4>
    <pre id='book'></pre>
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
    <pre id='portfolio'></pre>
    <h4>Historique</h4>
    <div id='notif'></div>
  </div>
</div>
<script>
let side='BUY';
const chart = LightweightCharts.createChart(document.getElementById('chart'), {layout:{background:{color:'#141d33'},textColor:'#dfe8ff'}});
const candles = chart.addCandlestickSeries();
const volumes = chart.addHistogramSeries({priceScaleId:'', priceFormat:{type:'volume'}});
const bars=[];
function setSide(s){ side=s; }
async function refresh(){
  const asset=document.getElementById('asset').value;
  const [m,b,p]=await Promise.all([
    fetch('/marche').then(r=>r.json()),
    fetch('/orderbook?asset='+asset).then(r=>r.json()),
    fetch('/portefeuille').then(r=>r.json())
  ]);
  document.getElementById('marche').textContent = JSON.stringify(m.actifs[asset], null, 2);
  document.getElementById('book').textContent = JSON.stringify(b, null, 2);
  document.getElementById('portfolio').textContent = JSON.stringify(p, null, 2);
}
async function envoyerOrdre(){
  const asset=document.getElementById('asset').value;
  const payload={asset, side, taille:Number(document.getElementById('qty').value), type_ordre:document.getElementById('otype').value, prix_limite: document.getElementById('price').value?Number(document.getElementById('price').value):null};
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
  const time=Math.floor(t.time);
  const prev=bars[bars.length-1];
  if(!prev || time>prev.time){
    const bar={time,open:t.price,high:t.price,low:t.price,close:t.price,volume:t.volume};
    bars.push(bar); candles.update(bar); volumes.update({time,value:t.volume,color:'#2e8bff88'});
  }else{
    prev.high=Math.max(prev.high,t.price); prev.low=Math.min(prev.low,t.price); prev.close=t.price; prev.volume+=t.volume;
    candles.update(prev); volumes.update({time:prev.time,value:prev.volume,color:'#2e8bff88'});
  }
};
setInterval(refresh, 500);
refresh();
</script>
</body></html>
"""
