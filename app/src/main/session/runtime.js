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
    restLeft: S.restDeadline ? 0 : Math.round(S.restLeft || 0),
    restOver: S.restDeadline ? 0 : Math.round(S.restOver || 0),
    warmLeft: S.warmDeadline ? 0 : Math.round(S.warmLeft || 0),
    exerciseLeft: S.exerciseDeadline ? 0 : Math.round(S.exerciseLeft || 0),
    setCache: caches.map(cache => [...cache]),
  };
  return { version: 1, date: HOST.date, state,
    managedIds: [...managed], logs, library: { ...library, custom: CUSTOM } };
}
function sessionCheckpoint() {
  if (!booted) return true;
  syncAlert();
  try {
    const text = JSON.stringify(sessionSnapshot());
    if (text === lastCheckpoint) return true;
    if (!AndroidSession.save(text)) throw new Error('save');
    lastCheckpoint = text; return true;
  } catch (_) { toast('No se pudo guardar.'); return false; }
}
function leaveSession() { updateSessionClock(); if (sessionCheckpoint()) AndroidSession.exit(); }
/** Peso y medidas and Ajustes stay native; the scene reopens where it was. */
function leaveSessionTo(target) { updateSessionClock(); if (sessionCheckpoint()) AndroidSession.openNative(target); }
ICONS.reset = { main: 'M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8', red: 'M3 3v5h5' };
ICONS.pause = { main: 'M9 5v14m6-14v14' };
ICONS.play = { main: 'M8 5l11 7-11 7z' };
/** Felt answers, one vocabulary for the whole session: tick, press, confirm, success, warn, alert. */
function haptic(kind) {
  // One gesture can reach two answers (slide, then what it confirms); it is felt once.
  const now = Date.now();
  if (kind === lastHaptic.kind && now - lastHaptic.at < 80) return;
  lastHaptic = { kind, at: now };
  try { AndroidSession.haptic(kind); } catch (_) {}
}
const timerDeadlines = () => [S.restDeadline, S.warmDeadline, S.exerciseDeadline].filter(Boolean);
/** The phone keeps the next deadline, so it still vibrates with the screen off or the scene closed. */
function syncAlert() {
  const now = Date.now(), next = S.finished ? 0 : Math.min(Infinity, ...timerDeadlines().filter(at => at > now));
  const at = Number.isFinite(next) ? next : 0;
  if (at === armedAlert) return;
  armedAlert = at;
  try { AndroidSession.armAlert(at); } catch (_) {}
}
/** While the scene is on screen the alert is ours; one that passed while away already rang on the phone. */
function checkAlerts(felt) {
  const due = timerDeadlines().filter(at => at <= Date.now()).pop();
  if (!due || S.alerted === due) return;
  S.alerted = due;
  if (felt) haptic('alert');
}
/** A long time away is a break, not training: the clocks pick up where they were left. */
function absorbAbsence() {
  if (!S.startedAt || S.finished || !S.seenAt) return;
  const gap = Date.now() - S.seenAt, running = Math.max(0, ...timerDeadlines().map(at => at - S.seenAt));
  if (gap <= AWAY_LIMIT + running) return;
  S.startedAt = Math.min(Date.now(), S.startedAt + gap);
  for (const key of ['restDeadline', 'warmDeadline', 'exerciseDeadline']) if (S[key]) S[key] += gap;
}
/** Warm-up and timed sets share the controls: pause keeps what is left, regresar goes back to the full time. */
function activeTimer() {
  if (S.stage === 'warmRun') return { deadline: 'warmDeadline', left: 'warmLeft', paused: 'warmPaused', total: (S.warmChoice?.min || 3) * 60 };
  const row = S.sets[S.exIdx].find(item => !item.done);
  return { deadline: 'exerciseDeadline', left: 'exerciseLeft', paused: 'exercisePaused', total: row?.r || 0 };
}
function timerControls() {
  return `<div class="timerControls" onpointerdown="event.stopPropagation()"><button class="timerBtn" onclick="restartTimer()" aria-label="Regresar">${icon('reset', 22, '#F5F5F5', false)}</button>
    <button class="timerBtn main" id="timerToggle" onclick="toggleTimer()"></button></div>`;
}
function paintTimerToggle() {
  const el = $('timerToggle'); if (!el) return;
  const paused = S[activeTimer().paused];
  el.innerHTML = icon(paused ? 'play' : 'pause', 24, '#F5F5F5', false);
  el.setAttribute('aria-label', paused ? 'Seguir' : 'Pausar');
}
function toggleTimer() {
  haptic('press');
  updateSessionClock();
  const timer = activeTimer();
  S[timer.paused] = !S[timer.paused];
  S[timer.deadline] = S[timer.paused] ? null : Date.now() + S[timer.left] * 1000;
  paintTimerToggle(); updateSessionClock(); sessionCheckpoint();
}
function restartTimer() {
  haptic('press');
  const timer = activeTimer();
  S[timer.left] = timer.total;
  if (!S[timer.paused]) S[timer.deadline] = Date.now() + timer.total * 1000;
  updateSessionClock(); sessionCheckpoint();
}
function resetSession() {
  HOST.logs = [];
  HOST.saved = null;
  S.warm = 0; S.warmChoice = null; S.exIdx = 0; S.next = 0;
  S.startedAt = null; S.endedAt = null; S.finished = false; S.resumeStage = null;
  S.restLeft = library.restSeconds || 90; S.restOver = 0; S.warmLeft = 0;
  S.restDeadline = null; S.warmDeadline = null; S.exerciseDeadline = null; S.timerKey = null;
  S.warmPaused = false; S.exercisePaused = false; S.exerciseLeft = 0;
  S.selection = null; S.alerted = null; S.seenAt = null;
  S.openingPhrase = phrase('opening');
  S.closingPhrase = phrase('closing');
  freshSets();
  lastCheckpoint = '';
  try { AndroidSession.reset?.(); } catch (_) {}
  closeSessionSettings();
  go('general');
}
function topBar() {
  return `<div class="row"><button class="tool" onclick="go('general')" aria-label="Vista general">${icon('home', 22)}</button>
    <div class="groups">${GROUPS.map(([key, name]) => `<span>${icon(key, 18)}${htmlText(name)}</span>`).join('')}</div>
    <div class="row" style="gap:6px"><button class="tool" data-hold="reset" aria-label="Reiniciar rutina">${icon('reset', 22)}</button>
    <button class="tool" onclick="openSessionSettings()" aria-label="Ajustes de la sesión">${icon('gear', 22)}</button></div></div>`;
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
    <label>Frases de inicio<textarea name="opening" maxlength="2000" required>${htmlText(library.opening.join('\n'))}</textarea></label>
    <label>Frases al terminar<textarea name="closing" maxlength="2000" required>${htmlText(library.closing.join('\n'))}</textarea></label>
    <label>Descanso · segundos<input name="rest" type="number" min="10" max="900" step="1" value="${library.restSeconds}" required></label>
    <div class="row"><button class="btn on" type="button" data-hold="reset">REINICIAR RUTINA</button><button class="btn" type="submit">GUARDAR</button></div></form>`;
  $('sessionPreferences').onsubmit = event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const lines = key => [...new Set(String(form.get(key)).split('\n').map(x => x.trim().slice(0, 120)).filter(Boolean))].slice(0, 20);
    const opening = lines('opening'), closing = lines('closing');
    if (!opening.length || !closing.length) { toast('Escribe al menos una frase en cada lista.'); return; }
    library.opening = opening; library.closing = closing; setRestSeconds(+form.get('rest'));
    try { AndroidSession.saveRestSeconds(library.restSeconds); } catch (_) {}
    if (!opening.includes(S.openingPhrase)) S.openingPhrase = phrase('opening');
    if (!closing.includes(S.closingPhrase)) S.closingPhrase = phrase('closing');
    if (sessionCheckpoint()) { closeSessionSettings(); if (S.stage === 'start') start(); else if (S.stage === 'end') end(); }
  };
}
function closeSessionSettings() { $('sessionSettings')?.remove(); }
/** One rest length, shared with the native settings; a rest under way keeps the one it started with. */
function setRestSeconds(seconds) { library.restSeconds = Math.max(10, Math.min(900, seconds)); }
function sessionBack() {
  if ($('sessionSettings')) { closeSessionSettings(); return; }
  if (H.picker) { closePicker(); return; }
  if ($('scrim') && !$('scrim').hidden) { closeHomeSheet(); return; }
  const open = document.querySelector('.panel.open, .sheet.open');
  if (open) { if (open.id === 'detail') closeDetail(); else open.classList.remove('open'); return; }
  if (S.stage === 'pickExercise' || S.stage === 'warm') { cancelSelection(); return; }
  if (S.stage === 'plain' && H.plainPick && !S.restDeadline) { plainCancel(); return; }
  if (S.stage === 'general' || S.stage === 'end') { leaveSession(); return; }
  go(S.stage === 'routine' && H.routineFrom || 'general');
}
function updateSessionClock() {
  const now = Date.now();
  if (S.startedAt && !S.finished && !sessionAway && !document.hidden) S.seenAt = now - now % 30000;
  if ((S.stage === 'rest' || S.stage === 'plain') && S.restDeadline) {
    const left = (S.restDeadline - now) / 1000;
    S.restLeft = Math.max(0, left); S.restOver = Math.max(0, -left);
    const el = $('restClock');
    if (el) { el.textContent = left > 0 ? fmt(Math.ceil(left)) : '+' + fmt(S.restOver); el.style.color = left > 0 ? '#F5F5F5' : '#D71921'; }
    restFigure();
  }
  if (S.stage === 'warmRun') {
    if (S.warmDeadline) S.warmLeft = Math.max(0, (S.warmDeadline - now) / 1000);
    const el = document.querySelector('.js-warm'); if (el) el.textContent = fmt(Math.ceil(S.warmLeft));
    // Pausing is also how a warm-up is cut short: the pill comes up.
    $('arc')?.classList.toggle('ready', S.warmLeft <= 0 || S.warmPaused);
  }
  document.querySelectorAll('.js-clock').forEach(el => el.textContent = fmt(elapsed()));
  if (S.stage === 'ex' || S.stage === 'plain') {
    const el = $('setTimer'); if (el) {
      if (S.exerciseDeadline) S.exerciseLeft = Math.max(0, (S.exerciseDeadline - now) / 1000);
      el.textContent = fmt(Math.ceil(S.exerciseLeft)); el.classList.toggle('red', S.exerciseLeft <= 0);
    }
  }
  paintResumeClock();
  if (!sessionAway && !document.hidden) checkAlerts(true);
}
function sessionResume() { sessionAway = false; absorbAbsence(); checkAlerts(false); updateSessionClock(); sessionCheckpoint(); }
function sessionPause() { sessionAway = true; $('track')?.dispatchEvent(new Event('sessionpause')); }

let library = {
  opening: ['Un momento para ti.', 'Hoy también cuenta.', 'Vamos a tu ritmo.'],
  closing: ['Hoy te hiciste un espacio.', 'Un paso más, a tu manera.', 'Quédate con lo que avanzaste.'],
  ...(HOST.library || {}),
};
library.restSeconds = HOST.restSeconds || 90;
const AWAY_LIMIT = 20 * 60 * 1000;
const LIVE_STAGES = ['warm', 'pickExercise', 'warmRun', 'ex', 'rest', 'plain'];
let booted = false, lastCheckpoint = '', restoring = true, sessionAway = false, armedAlert = 0, lastHaptic = {};
S.speed = 1;
if (HOST.saved?.version === 1 && HOST.saved.date === HOST.date) {
  const saved = HOST.saved.state, plan = EXS.map(item => item.id).join();
  if ((saved?.planIds?.join() ?? plan) === plan && saved?.choices?.length === EXS.length && saved.sets?.length === EXS.length && saved.setCache?.length === EXS.length) {
    Object.assign(S, saved); S.setCache = saved.setCache.map(cache => new Map(cache));
  } else if (saved?.startedAt) {
    // Guardar changed the routine mid-session: what was logged stays, and it picks up at the first thing left to do.
    Object.assign(S, { startedAt: saved.startedAt, warmChoice: saved.warmChoice, finished: !!saved.finished, endedAt: saved.endedAt, seenAt: saved.seenAt, stage: 'general' });
  }
}
S.planIds = EXS.map(item => item.id);
S.speed = 1;
S.openingPhrase ||= phrase('opening');
S.closingPhrase ||= phrase('closing');
// No fake history for bundled warm-ups.
OTHERS.forEach(item => delete item.avg);

const prototypeStart = start;
start = function() {
  prototypeStart(); document.querySelector('.intro h1').textContent = S.openingPhrase;
  $('track').dataset.stunt = 'climb point'; document.querySelector('.intro h1').dataset.stunt = 'hang';
  document.querySelector('.tool[aria-label="Ajustes de la sesión"]').dataset.stunt = 'top';
};
for (const stageFn of ['warmRun', 'ex', 'rest']) {
  const prev = window[stageFn];
  window[stageFn] = function(...args) {
    const res = prev(...args);
    const homeBtn = $('view')?.querySelector('button.tool[onclick="go(\'general\')"]');
    if (homeBtn && !homeBtn.parentElement.classList.contains('topTools')) {
      const wrap = document.createElement('div');
      wrap.className = 'row topTools';
      wrap.style.gap = '6px';
      wrap.innerHTML = `<button class="tool" data-hold="reset" aria-label="Reiniciar rutina">${icon('reset', 22)}</button>`;
      homeBtn.replaceWith(wrap);
      wrap.append(homeBtn);
    }
    if (stageFn === 'warmRun') { document.querySelector('.js-warm').insertAdjacentHTML('afterend', timerControls()); paintTimerToggle(); }
    document.querySelector('.nextCard')?.setAttribute('data-stunt', 'climb point');
    $('restClock')?.setAttribute('data-stunt', 'hang');
    return res;
  };
}
const prototypeOpenNext = openNext;
openNext = function() {
  prototypeOpenNext();
  const panel = $('pNext');
  panel.insertAdjacentHTML('beforeend', `<div class="nextFoot"><button class="btn on" data-hold="reset">REINICIAR RUTINA</button>${slidePill('Terminar')}</div>`);
  bindSkipSlide(panel.querySelector('.skipSlide'), () => go('end', true));
};
const prototypeBeginSelection = beginExerciseSelection;
beginExerciseSelection = function(slot, returnStage = S.stage === 'rest' ? 'rest' : 'ex') {
  if (S.stage === 'plain') { $('pNext').classList.remove('open'); plainJump(slot); return; }
  prototypeBeginSelection(slot, returnStage);
};
/** The first exercise with sets left after [after], going round, in whatever order they were taken. */
function nextPending(after) {
  return [...S.sets.keys()].map(i => (after + 1 + i) % S.sets.length).find(i => S.sets[i].some(row => !row.done)) ?? -1;
}
/** Where to go back to from Hoy, even if the stage it was on can no longer open. */
function resumeTarget() {
  const stage = S.resumeStage;
  if (stage === 'warm' || stage === 'rest' || stage === 'ex' || stage === 'plain') return stage;
  if (stage === 'warmRun' && S.warmChoice) return stage;
  if (stage === 'pickExercise' && S.selection) return stage;
  const pending = nextPending(-1);
  if (pending < 0) return 'end';
  S.exIdx = pending;
  return 'ex';
}
function resumeLabel() {
  const stage = resumeTarget();
  if (stage === 'warmRun') return S.warmChoice.name;
  if (stage === 'warm' || stage === 'end') return '';
  return exerciseAt(stage === 'rest' || stage === 'plain' && S.restDeadline ? S.next : stage === 'pickExercise' ? S.selection.slot : S.exIdx).name;
}
function paintResumeClock() {
  const el = $('resumeClock'); if (!el) return;
  const stage = S.resumeStage, now = Date.now();
  let left = null;
  if (stage === 'rest' || stage === 'plain' && S.restDeadline) left = S.restDeadline ? (S.restDeadline - now) / 1000 : S.restLeft;
  else if (stage === 'warmRun') left = S.warmDeadline ? (S.warmDeadline - now) / 1000 : S.warmLeft;
  el.textContent = left === null ? fmt(elapsed()) : left > 0 ? fmt(Math.ceil(left)) : '+' + fmt(-left);
  el.classList.toggle('red', left !== null && left <= 0);
}
/** [direct] skips the routing that looks for what is left: it is how a session ends early. */
go = function(stage, direct = false) {
  updateSessionClock();
  const from = S.stage;
  if ((stage === 'general' || stage === 'routine') && S.startedAt && LIVE_STAGES.includes(from)) {
    // Backing out of the first choice with nothing done never really started.
    if ((from === 'warm' || from === 'plain') && !S.warmChoice && !S.sets.some(rows => rows.some(row => row.done))) { S.startedAt = null; S.seenAt = null; S.resumeStage = null; S.exerciseDeadline = null; S.timerKey = null; } else S.resumeStage = from;
  }
  if (!restoring && !direct && (stage === 'rest' || stage === 'end') && (from === 'ex' || from === 'warmRun')) {
    const pending = nextPending(from === 'ex' ? S.exIdx : -1);
    stage = pending < 0 ? 'end' : 'rest';
    if (pending >= 0) S.next = pending;
  }
  if (!restoring) {
    if (stage === 'rest' && !S.restDeadline) {
      S.restLeft = S.restTotal = library.restSeconds; S.restOver = 0;
      S.restDeadline = Date.now() + S.restLeft * 1000;
    }
    if (stage === 'warmRun' && !S.warmPaused && !S.warmDeadline) S.warmDeadline = Date.now() + S.warmLeft * 1000;
    if (stage === 'ex' || stage === 'end') S.restDeadline = null;
    if (stage === 'rest' || stage === 'end') { S.warmDeadline = null; S.warmPaused = false; S.exerciseDeadline = null; S.exercisePaused = false; S.timerKey = null; }
    if (stage === 'start') { freshSets(); S.startedAt = null; S.endedAt = null; S.exIdx = 0; }
  }
  startGestureCleanup?.(); startGestureCleanup = null;
  $('phone').classList.remove('detail-open');
  if (!['general', 'routine', 'end', 'week', 'goals'].includes(stage)) $('view').className = '';
  S.stage = stage;
  ({ general, routine, week: weekView, goals: goalsView, start, warm, pickExercise, warmRun, ex, rest, plain, end })[stage]();
  updateSessionClock();
  bounceDock();
  // New content answers at once; the short fade only softens the change, it never delays it.
  if (!restoring && from !== stage && !motionPreference.matches) $('view').animate([{ opacity: 0 }, { opacity: 1 }], { duration: 170, easing: 'ease-out' });
  syncKeepAwake();
  sessionCheckpoint();
};
/** The screen stays on only while a session is on it. */
function syncKeepAwake() {
  try { AndroidSession.keepAwake(!!S.startedAt && !S.finished && LIVE_STAGES.includes(S.stage)); } catch (_) {}
}
const prototypeRenderSets = renderSets;
renderSets = function() {
  prototypeRenderSets();
  if (exerciseAt(S.exIdx).timed) {
    $('sets').querySelectorAll('small').forEach(el => { if (el.textContent === 'REPS') el.textContent = 'SEG'; });
    $('sets').querySelectorAll('.step span').forEach(el => { el.textContent = el.textContent.replace(' reps', ' s'); });
  }
  // With every set gone the exercise is skipped, so the way forward stays open.
  $('arc').classList.toggle('ready', S.sets[S.exIdx].every(row => row.done));
};
/** The timer belongs to the first unmarked set of a timed exercise; it survives leaving and coming back. */
function syncSetTimer() {
  const item = exerciseAt(S.exIdx), rows = S.sets[S.exIdx], index = item.timed ? rows.findIndex(row => !row.done) : -1;
  const key = index >= 0 ? `${S.exIdx}:${item.id}:${index}` : null;
  if (S.timerKey !== key) {
    S.timerKey = key; S.exercisePaused = false; S.exerciseLeft = key ? rows[index].r : 0;
    S.exerciseDeadline = key ? Date.now() + S.exerciseLeft * 1000 : null;
  }
  $('area').querySelectorAll('#setTimer, .timerControls').forEach(el => el.remove());
  if (key) { $('area').insertAdjacentHTML('beforeend', `<div class="big" id="setTimer"></div>${timerControls()}`); paintTimerToggle(); updateSessionClock(); }
}
const prototypeEx = ex;
ex = function() { prototypeEx(); syncSetTimer(); };
const prototypeSummary = selectionSummary;
selectionSummary = function(item) {
  if (S.selection.kind === 'warm' || !item.timed) return prototypeSummary(item);
  return `${item.sets.length} SERIES · ${fmt(item.sets[0]?.[0] || 0)}`;
};
const prototypeChooseWarm = chooseWarm;
chooseWarm = function(choice) { S.warmPaused = false; S.warmDeadline = null; prototypeChooseWarm(choice); };
const prototypeMark = markSet;
markSet = function(index) {
  prototypeMark(index); syncSetTimer();
  const rows = S.sets[S.exIdx];
  haptic(!rows[index].done ? 'tick' : rows.every(row => row.done) ? 'success' : 'confirm');
  sessionCheckpoint();
};
const prototypeBump = bump;
bump = function(index, key, delta) {
  const row = S.sets[S.exIdx][index];
  if (key === 'kg' && row.kg + delta > 500 || key === 'r' && row.r + delta > 86400) return;
  prototypeBump(index, key, delta); sessionCheckpoint();
};
const prototypeDelSet = delSet;
delSet = function(index) { prototypeDelSet(index); haptic('warn'); syncSetTimer(); };
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
/** One exercise over time: each session a bar made of its series, red where it beat the mark. */
function openDash() {
  const e = exerciseAt(S.exIdx), panel = $('pDash'), logs = allLogs().filter(row => row.exerciseId === e.id);
  const dates = [...new Set(logs.map(row => row.date))].slice(-20), { best, records } = personalBests(logs), top = best.get(e.id);
  const score = row => row.r * (row.kg || 1);
  H.chart = { selected: -1, columns: dates.map(date => {
    const rows = logs.filter(row => row.date === date);
    return { label: dayLabel(date), short: dayLabel(date), total: rows.reduce((sum, row) => sum + score(row), 0), marks: records.has(date) ? 1 : 0,
      stacks: rows.map((row, i) => ({ color: SHADES[i % SHADES.length], value: score(row) })), rows: rows.map((row, i) => [SHADES[i % SHADES.length], String(i + 1), markLine(e, row)]), headline: '' };
  }) };
  panel.innerHTML = `<div class="row"><h3>${htmlText(e.name)}</h3><button class="tool" aria-label="Cerrar progreso" onclick="$('pDash').classList.remove('open')">${icon('close', 20)}</button></div>
    ${top ? `<div class="goal-total"><strong class="num">${htmlText(markLine(e, top))}</strong></div>
    <div class="chart" id="chart">${chartHtml()}</div>
    <div class="chart-ends"><span>${H.chart.columns[0].short}</span><span>${H.chart.columns.at(-1).short}</span></div>
    <div class="week-rows">${[...dates].reverse().map(date => {
      const rows = logs.filter(row => row.date === date), mark = rows.reduce((a, b) => beats(b, a) ? b : a);
      return `<div class="week-row"><span class="week-text"><strong>${dayLabel(date)}</strong><small>${htmlText(markLine(e, mark))}</small></span><span class="week-dots">${rows.map(() => '<i class="on"></i>').join('')}</span></div>`;
    }).join('')}</div>` : ''}`;
  panel.classList.add('open'); swipe(panel, { right: () => panel.classList.remove('open') });
}
