const API = 'http://localhost:8080/api';
const $ = s => document.querySelector(s);
let seats = [], selId = null;

/* ---------- REAL API ---------- */
async function call(method, path, body) {
  const t = performance.now();
  const req = `${method} ${API}${path}` + (body ? `\nbody: ${JSON.stringify(body)}` : '');
  try {
    const res = await fetch(API + path, { method, headers: { 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
    const data = await res.json().catch(() => null);
    const ms = Math.round(performance.now() - t);
    $('#log').textContent = `${req}\n\nHTTP ${res.status}  (${ms} ms)\n${JSON.stringify(data, null, 2)}`;
    return { ok: res.ok, status: res.status, data };
  } catch (e) {
    $('#log').textContent = `${req}\n\nNetwork error: ${e.message}\nIs the backend running on port 8080?`;
    return { ok: false, data: null };
  }
}
async function loadSeats() {
  const r = await call('GET', '/seats');
  if (!r.ok) return;
  seats = r.data; if (!selId) selId = seats[0]?.id; draw();
}
function draw(processing) {
  $('#grid').innerHTML = seats.map(s => {
    const st = processing === s.id ? 'processing' : s.status.toLowerCase();
    return `<div class="seat ${st} ${s.id === selId ? 'sel' : ''}" data-id="${s.id}" tabindex="0">${s.seatNumber}</div>`;
  }).join('');
  const s = seats.find(x => x.id === selId);
  $('#info').textContent = s ? `Seat ${s.seatNumber} · ${s.status} · version ${s.version}` + (s.bookedBy ? ` · by ${s.bookedBy}` : '') : 'Select a seat.';
}
$('#grid').addEventListener('click', e => { const d = e.target.dataset.id; if (d) { selId = +d; draw(); } });
async function book(kind) {
  const s = seats.find(x => x.id === selId); if (!s) return;
  draw(s.id);
  await call('POST', `/seats/${s.id}/book/${kind}`, { userName: 'Browser user', expectedVersion: kind === 'optimistic' ? s.version : null });
  await keepLog(loadSeats);
}
async function keepLog(fn) { const t = $('#log').textContent; await fn(); $('#log').textContent = t; }
$('#bOpt').onclick = () => book('optimistic');
$('#bPes').onclick = () => book('pessimistic');
$('#bReset').onclick = async () => { await call('POST', `/seats/${selId}/reset`); await keepLog(loadSeats); };
document.querySelectorAll('[data-c]').forEach(b => b.onclick = async () => {
  const kind = b.dataset.c, n = +b.dataset.n;
  draw(selId); $('#summary').textContent = `Running ${n} real concurrent requests (${kind})...`;
  const r = await call('POST', `/demo/concurrent-booking/${kind}`, { seatId: selId, users: n });
  if (r.ok) {
    const d = r.data;
    $('#summary').textContent = `${d.successCount} of ${d.users} succeeded · final status ${d.finalSeat.status} · version ${d.finalSeat.version} · ${d.totalDurationMs} ms total`;
    $('#results tbody').innerHTML = d.results.map(x => `<tr><td>${x.user}</td><td class="${x.result}">${x.result}</td><td>${x.explanation}</td><td>${x.durationMs}</td></tr>`).join('');
  } else $('#summary').textContent = 'Request failed. See the log below.';
  await keepLog(loadSeats);
});

/* ---------- VISUAL SIMULATIONS (no backend involved) ---------- */
const optSteps = [
  { t: 'User A and User B both read seat 14C', a: 'Read seat<br>version 1', b: 'Read seat<br>version 1', db: 'Seat 14C<br>AVAILABLE<br><b>version 1</b>' },
  { t: 'Both users hold version 1', a: 'Holds version 1', b: 'Holds version 1', db: 'Seat 14C<br>AVAILABLE<br><b>version 1</b>' },
  { t: 'User A sends UPDATE ... WHERE id=14 AND version=1', a: 'UPDATE ... AND version=1', b: 'Still thinking', db: 'Checking version...' },
  { t: 'User A succeeds: 1 row updated, version becomes 2', a: 'SUCCESS', ac: 'ok', b: 'Still holds version 1', db: 'Seat 14C<br>BOOKED by A<br><b>version 2</b>', dc: 'ok' },
  { t: 'The database version is now 2', a: 'SUCCESS', ac: 'ok', b: 'Still holds version 1', db: 'Seat 14C<br>BOOKED by A<br><b>version 2</b>', dc: 'ok' },
  { t: 'User B sends UPDATE ... WHERE id=14 AND version=1', a: 'SUCCESS', ac: 'ok', b: 'UPDATE ... AND version=1', db: 'Checking version...' },
  { t: 'User B gets 0 rows updated, so a version conflict', a: 'SUCCESS', ac: 'ok', b: 'CONFLICT (HTTP 409)', bc: 'fail', db: 'Seat 14C<br>BOOKED by A<br><b>version 2</b>', dc: 'ok',
    why: 'User B asked for "version = 1", but the row is at version 2. No row matches, so Hibernate throws ObjectOptimisticLockingFailureException and nothing is overwritten. B must reload and retry.' }
];
const pesSteps = [
  { t: 'User A requests seat 14C (SELECT ... FOR UPDATE)', a: 'Requesting lock', b: 'Idle', db: 'Seat 14C<br>AVAILABLE<br>Lock: none<br>Waiting: none' },
  { t: 'Database grants the row lock to User A', a: 'Holds lock<br>Tx: ACTIVE', ac: 'ok', b: 'Idle', db: 'Seat 14C<br>AVAILABLE<br>Lock: <b>User A</b><br>Waiting: none', dc: 'locked' },
  { t: 'User B requests the same seat', a: 'Holds lock<br>Tx: ACTIVE', ac: 'ok', b: 'Requesting lock...', db: 'Seat 14C<br>AVAILABLE<br>Lock: <b>User A</b><br>Waiting: none', dc: 'locked' },
  { t: 'User B waits (blocked inside the database)', a: 'Holds lock<br>Tx: ACTIVE', ac: 'ok', b: 'WAITING for lock', bc: 'waiting', db: 'Seat 14C<br>AVAILABLE<br>Lock: <b>User A</b><br>Waiting: <b>User B</b>', dc: 'locked' },
  { t: 'User A books the seat', a: 'UPDATE seat to BOOKED<br>Tx: ACTIVE', ac: 'ok', b: 'WAITING for lock', bc: 'waiting', db: 'Seat 14C<br>BOOKED (uncommitted)<br>Lock: <b>User A</b><br>Waiting: <b>User B</b>', dc: 'locked' },
  { t: 'User A commits the transaction', a: 'Tx: COMMITTED', ac: 'ok', b: 'WAITING for lock', bc: 'waiting', db: 'Seat 14C<br>BOOKED<br>Committing...', dc: 'locked' },
  { t: 'Database releases the lock', a: 'Done', ac: 'ok', b: 'Lock granted', db: 'Seat 14C<br>BOOKED<br>Lock: none', dc: 'ok' },
  { t: 'User B proceeds and sees the seat is already booked', a: 'Done', ac: 'ok', b: 'Sees BOOKED, stops (HTTP 409)', bc: 'fail', db: 'Seat 14C<br>BOOKED<br>Lock: none', dc: 'ok',
    why: 'B never overwrote anything: it waited its turn, then read the committed state. Cost: B held a connection while waiting, so keep transactions short.' }
];
const sims = {};
function makeSim(id, steps, init) {
  const el = $('#' + id); let i = -1, timer = null;
  const draw = () => {
    const s = i < 0 ? init : steps[i];
    const set = (c, name, html, cls) => { const e = el.querySelector('.' + c); e.className = 'box ' + c + ' ' + (cls || ''); e.innerHTML = `<h4>${name}</h4>${html}`; };
    set('a', 'User A', s.a, s.ac); set('db', 'Database (seat 14C)', s.db, s.dc); set('b', 'User B', s.b, s.bc);
    el.querySelector('.timeline').innerHTML = steps.slice(0, i + 1).map(x => `<li>${x.t}</li>`).join('');
    const w = el.querySelector('.why'); w.textContent = s.why || ''; w.classList.toggle('on', !!s.why);
  };
  const stop = () => { clearInterval(timer); timer = null; };
  const next = () => { if (i < steps.length - 1) { i++; draw(); } else stop(); };
  sims[id] = {
    start() { stop(); i = 0; draw(); }, next,
    auto() { stop(); if (i >= steps.length - 1) i = -1; timer = setInterval(next, 1500); next(); },
    reset() { stop(); i = -1; draw(); }
  };
  draw();
}
makeSim('simOpt', optSteps, { a: 'Idle', b: 'Idle', db: 'Seat 14C<br>AVAILABLE<br><b>version 1</b>' });
makeSim('simPes', pesSteps, { a: 'Idle', b: 'Idle', db: 'Seat 14C<br>AVAILABLE<br>Lock: none' });
document.querySelectorAll('[data-sim]').forEach(b => b.onclick = () => sims[b.dataset.sim][b.dataset.act]());

/* ---------- ISOLATION (static explanation) ---------- */
const iso = {
  READ_UNCOMMITTED: ['Yes (in theory)', 'Yes', 'Yes'],
  READ_COMMITTED: ['No', 'Yes', 'Yes'],
  REPEATABLE_READ: ['No', 'No', 'Yes (No in PostgreSQL)'],
  SERIALIZABLE: ['No', 'No', 'No']
};
const ex = ['Tx B reads seat as BOOKED while Tx A has not committed, then A rolls back.', 'Tx A reads the seat twice; B commits a booking in between, so the two reads differ.', 'Tx A counts free seats twice; B inserts a new seat in between, so the count changes.'];
const names = ['Dirty read', 'Non-repeatable read', 'Phantom read'];
function drawIso() { $('#isoBody').innerHTML = iso[$('#iso').value].map((v, k) => `<tr><td>${names[k]}</td><td>${v}</td><td>${ex[k]}</td></tr>`).join(''); }
$('#iso').onchange = drawIso; drawIso();
loadSeats();
