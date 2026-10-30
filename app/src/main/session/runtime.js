// Production adapter. The reviewed character/gesture engine above remains shared with the prototype.
// All persistence goes through the local Android bridge, never through browser storage or a network.
function freshSets() {
  S.choices = EXS.slice(); S.setCache = EXS.map(() => new Map());
  S.sets = EXS.map(ex => {
    const logs = HOST.logs.filter(row => row.exerciseId === ex.id);
    const count = Math.max(ex.sets.length, ...logs.map(row => row.index + 1));
    return Array.from({ length: count }, (_, index) => {
      const done = logs.find(row => row.index === index), initial = ex.sets[index] || ex.sets[0];
      return { r: done?.r ?? initial[0], kg: done?.kg ?? initial[1], done: !!done };
    });
  });
}
function selectionCatalog(kind) {
  const items = kind === 'warm' ? [...OTHERS, ...CUSTOM.warm] : [...EXS, ...HOST.catalog, ...CUSTOM.exercise];
  return [...new Map(items.map(item => [item.id, item])).values()];
}
function sessionSnapshot() {
  const caches = S.setCache.map((cache, slot) => new Map([...cache, [S.choices[slot].id, S.sets[slot]]]));
  const counters = new Map(), logs = [], managed = new Set();
  for (const cache of caches) for (const [id, rows] of cache) {
    managed.add(id);
    for (const row of rows) if (row.done) {
      const index = counters.get(id) || 0;
      logs.push({ exerciseId: id, index, r: row.r, kg: row.kg }); counters.set(id, index + 1);
    }
  }
  const state = {
    ...S,
    restLeft: S.restDeadline ? library.restSeconds : Math.round(S.restLeft || 0),
    restOver: S.restDeadline ? 0 : Math.round(S.restOver || 0),
    warmLeft: S.warmDeadline ? (S.warmChoice?.min || 3) * 60 : Math.round(S.warmLeft || 0),
    setCache: caches.map(cache => [...cache]),
  };
  return { version: 1, date: HOST.date, state,
    managedIds: [...managed], logs, library: { ...library, custom: CUSTOM } };
}
function sessionCheckpoint() {
  if (!booted) return true;
  try {
    const text = JSON.stringify(sessionSnapshot());
    if (text === lastCheckpoint) return true;
    if (!AndroidSession.save(text)) throw new Error('save');
    lastCheckpoint = text; return true;
  } catch (_) { toast('No se pudo guardar. Mantén la sesión abierta e inténtalo de nuevo.'); return false; }
}
function leaveSession() { updateSessionClock(); if (sessionCheckpoint()) AndroidSession.exit(); }
function general() { leaveSession(); }
function topBar() {
  return `<div class="row"><button class="tool" onclick="leaveSession()" aria-label="Vista general">${icon('home', 22)}</button>
    <div class="groups">${GROUPS.map(([key, name]) => `<span>${icon(key, 18)}${htmlText(name)}</span>`).join('')}</div>
    <button class="tool" onclick="openSessionSettings()" aria-label="Ajustes de la sesión">${icon('gear', 22)}</button></div>`;
}
function phrase(kind) {
  const values = library[kind], last = library['last' + kind];
  const pool = values.filter(value => value !== last), value = pick(pool.length ? pool : values);
  library['last' + kind] = value;
  return value;
}
function openSessionSettings() {
  let panel = $('sessionSettings');
  if (!panel) { panel = document.createElement('div'); panel.id = 'sessionSettings'; panel.className = 'panel open settingsScroll'; $('phone').append(panel); }
  panel.innerHTML = `<form class="entry" id="sessionPreferences"><div class="row"><h3>A tu manera</h3><button type="button" class="btn" onclick="closeSessionSettings()">CERRAR</button></div>
    <label>Frases de inicio · una por línea<textarea name="opening" maxlength="2000" required>${htmlText(library.opening.join('\n'))}</textarea></label>
    <label>Frases al terminar · una por línea<textarea name="closing" maxlength="2000" required>${htmlText(library.closing.join('\n'))}</textarea></label>
    <label>Descanso entre ejercicios · segundos<input name="rest" type="number" min="10" max="900" step="1" value="${library.restSeconds}" required></label>
    <button class="btn" type="submit">GUARDAR</button></form>`;
  $('sessionPreferences').onsubmit = event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const lines = key => [...new Set(String(form.get(key)).split('\n').map(x => x.trim().slice(0, 120)).filter(Boolean))].slice(0, 20);
    const opening = lines('opening'), closing = lines('closing');
    if (!opening.length || !closing.length) { toast('Escribe al menos una frase en cada lista.'); return; }
    library.opening = opening; library.closing = closing; library.restSeconds = Math.max(10, Math.min(900, +form.get('rest')));
    if (!opening.includes(S.openingPhrase)) S.openingPhrase = phrase('opening');
    if (!closing.includes(S.closingPhrase)) S.closingPhrase = phrase('closing');
    if (sessionCheckpoint()) { closeSessionSettings(); if (S.stage === 'start') start(); else if (S.stage === 'end') end(); }
  };
}
function closeSessionSettings() { $('sessionSettings')?.remove(); }
function sessionBack() {
  if ($('sessionSettings')) { closeSessionSettings(); return; }
  const open = document.querySelector('.panel.open, .sheet.open');
  if (open) { if (open.id === 'detail') closeDetail(); else open.classList.remove('open'); return; }
  if (S.stage === 'pickExercise' || S.stage === 'warm') { cancelSelection(); return; }
  leaveSession();
}
function updateSessionClock() {
  const now = Date.now();
  if (S.stage === 'rest' && S.restDeadline) {
    const left = (S.restDeadline - now) / 1000;
    S.restLeft = Math.max(0, left); S.restOver = Math.max(0, -left);
    const el = $('restClock');
    if (el) { el.textContent = left > 0 ? fmt(Math.ceil(left)) : '+' + fmt(S.restOver); el.style.color = left > 0 ? '#F5F5F5' : '#D71921'; }
    restFigure();
  }
  if (S.stage === 'warmRun' && S.warmDeadline) {
    S.warmLeft = Math.max(0, (S.warmDeadline - now) / 1000);
    const el = document.querySelector('.js-warm'); if (el) el.textContent = fmt(Math.ceil(S.warmLeft));
  }
  document.querySelectorAll('.js-clock').forEach(el => el.textContent = fmt(elapsed()));
  if (S.stage === 'ex' && S.exerciseDeadline) {
    const el = $('setTimer'); if (el) {
      const left = Math.max(0, (S.exerciseDeadline - now) / 1000);
      el.textContent = fmt(Math.ceil(left)); el.classList.toggle('red', left <= 0);
    }
  }
}
function sessionResume() { updateSessionClock(); sessionCheckpoint(); }

let library = {
  opening: ['Un momento para ti.', 'Hoy también cuenta.', 'Vamos a tu ritmo.'],
  closing: ['Hoy te hiciste un espacio.', 'Un paso más, a tu manera.', 'Quédate con lo que avanzaste.'],
  restSeconds: HOST.restSeconds || 90,
  ...(HOST.library || {}),
};
let booted = false, lastCheckpoint = '', restoring = true;
S.speed = 1;
if (HOST.saved?.version === 1 && HOST.saved.date === HOST.date) {
  const saved = HOST.saved.state;
  if (saved?.choices?.length === EXS.length && saved.sets?.length === EXS.length && saved.setCache?.length === EXS.length) {
    Object.assign(S, saved); S.setCache = saved.setCache.map(cache => new Map(cache));
  }
}
S.speed = 1;
S.openingPhrase ||= phrase('opening');
S.closingPhrase ||= phrase('closing');
// No fake history for bundled warm-ups.
OTHERS.forEach(item => delete item.avg);

const prototypeStart = start;
start = function() { prototypeStart(); document.querySelector('.intro h1').textContent = S.openingPhrase; };
const prototypeEnd = end;
end = function() { prototypeEnd(); document.querySelector('.intro h1').textContent = S.closingPhrase; };
const prototypeGo = go;
go = function(stage) {
  if (stage === 'general') { leaveSession(); return; }
  updateSessionClock();
  if (!restoring && stage === 'end') {
    const pending = S.sets.findIndex(rows => !rows.length || rows.some(row => !row.done));
    if (pending >= 0) { toast('Todavía quedan series por registrar.'); beginExerciseSelection(pending); return; }
  }
  if (!restoring && stage === 'rest') {
    if (S.stage !== 'pickExercise') S.restLeft = library.restSeconds;
    S.restDeadline = Date.now() + (S.restLeft > 0 ? S.restLeft : -S.restOver) * 1000;
  }
  if (!restoring && stage === 'warmRun') S.warmDeadline = Date.now() + S.warmLeft * 1000;
  if (S.stage === 'rest' && stage !== 'rest') S.restDeadline = null;
  if (restoring) {
    S.stage = stage;
    ({ start, warm, pickExercise, warmRun, ex, rest, end })[stage]();
  } else prototypeGo(stage);
  sessionCheckpoint();
};
const prototypeArc = bindArc;
bindArc = function(done) {
  prototypeArc(() => {
    if (S.stage === 'ex' && (!S.sets[S.exIdx].length || S.sets[S.exIdx].some(row => !row.done))) { toast('Marca tus series antes de continuar.'); return; }
    done();
  });
};
const prototypeRenderSets = renderSets;
renderSets = function() {
  prototypeRenderSets();
  if (exerciseAt(S.exIdx).timed) {
    $('sets').querySelectorAll('small').forEach(el => { if (el.textContent === 'REPS') el.textContent = 'SEG'; });
    $('sets').querySelectorAll('.step span').forEach(el => { el.textContent = el.textContent.replace(' reps', ' s'); });
  }
};
const prototypeEx = ex;
ex = function() {
  prototypeEx();
  if (exerciseAt(S.exIdx).timed) {
    const index = S.sets[S.exIdx].findIndex(row => !row.done);
    const key = `${S.exIdx}:${exerciseAt(S.exIdx).id}:${index}`;
    if (index >= 0) {
      if (S.timerKey !== key) { S.timerKey = key; S.exerciseDeadline = Date.now() + S.sets[S.exIdx][index].r * 1000; }
      const timer = document.createElement('div'); timer.className = 'big'; timer.id = 'setTimer'; $('area').append(timer);
      updateSessionClock();
    }
  } else S.exerciseDeadline = null;
};
const prototypeMark = markSet;
markSet = function(index) { prototypeMark(index); if (exerciseAt(S.exIdx).timed) ex(); sessionCheckpoint(); };
const prototypeBump = bump;
bump = function(index, key, delta) {
  const row = S.sets[S.exIdx][index];
  if (key === 'kg' && row.kg + delta > 500 || key === 'r' && row.r + delta > 86400) return;
  prototypeBump(index, key, delta); sessionCheckpoint();
};
const prototypeNew = openNewExercise;
openNewExercise = function() {
  prototypeNew();
  if (S.selection.kind === 'exercise') {
    const label = document.createElement('label'); label.innerHTML = 'Medir en<select name="unit"><option value="reps">Repeticiones</option><option value="seconds">Segundos</option></select>';
    $('newExercise').insertBefore(label, $('entryError'));
  }
};
const prototypeCreate = createCustomItem;
createCustomItem = function(kind, fields, zone) {
  if (CUSTOM[kind].length >= 100) throw new Error('El catálogo admite hasta 100 ejercicios propios por tipo.');
  const item = prototypeCreate(kind, fields, zone);
  if (kind === 'exercise') item.timed = fields.unit === 'seconds';
  item.id = 'custom-' + crypto.randomUUID();
  return item;
};
// Persist every meaningful edit, including alternatives and new catalog entries.
for (const name of ['replaceExercise', 'addSet', 'delSet', 'previewOther', 'closeDetail']) {
  const original = window[name]; window[name] = function(...args) { const value = original(...args); sessionCheckpoint(); return value; };
}
function openDash() {
  const e = exerciseAt(S.exIdx), panel = $('pDash');
  const logs = HOST.history.filter(row => row.exerciseId === e.id);
  const dates = [...new Set(logs.map(row => row.date))].sort().reverse().slice(0, 12);
  panel.innerHTML = `<div class="row"><h3>${htmlText(e.name)}</h3><button class="tool" aria-label="Cerrar progreso" onclick="$('pDash').classList.remove('open')">${icon('close', 20)}</button></div>
    ${dates.length ? dates.map(date => `<div class="sec">${date}</div><p class="num">${logs.filter(row => row.date === date).map(row => `${row.r}${e.timed ? ' s' : ' reps'} · ${row.kg} kg`).join('<br>')}</p>`).join('') : '<p class="note">Todavía no hay sesiones anteriores. Tus próximos registros aparecerán aquí.</p>'}`;
  panel.style.overflowY = 'auto'; panel.classList.add('open'); swipe(panel, { right: () => panel.classList.remove('open') });
}
// Correct the introductory warm-up preview wording for items without a dedicated animation.
const initialStage = ['start', 'warm', 'pickExercise', 'warmRun', 'ex', 'rest', 'end'].includes(S.stage) ? S.stage : S.startedAt ? 'ex' : 'start';
go(initialStage); restoring = false; booted = true; sessionResume();
setInterval(updateSessionClock, 250);
setInterval(sessionCheckpoint, 1500);
document.addEventListener('visibilitychange', () => { updateSessionClock(); sessionCheckpoint(); });
