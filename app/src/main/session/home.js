// Hoy, Tu rutina and Terminaste (from the EjercicioHoyRutinaVisual concept), on the session's engine and person.
// Everything here is local to the bundled page; plans are saved through the Android bridge.
ICONS.today = { main: 'M12 3v2m0 14v2M3 12h2m14 0h2M5.6 5.6 7 7m10 10 1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4M16 12a4 4 0 1 1-8 0a4 4 0 0 1 8 0' };
ICONS.week = { main: 'M5 5h14v15H5zM8 3v4m8-4v4M5 10h14M8 14h2m4 0h2m-8 3h2' };
ICONS.goal = { main: 'M5 21V4h13l-3 4 3 4H5' };
ICONS.arrow = { main: 'M5 12h14M13 6l6 6-6 6' };
ICONS.back = { main: 'M19 12H5m6-6-6 6 6 6' };
ICONS.skip = { main: 'm6 6 6 6-6 6m6-12 6 6-6 6' };
ICONS.plus = { main: 'M12 5v14M5 12h14' };
ICONS.minus = { main: 'M5 12h14' };
ICONS.edit = { main: 'm5 15 10-10 4 4L9 19l-5 1 1-5m8-8 4 4' };
ICONS.clock = { main: 'M12 7v5l3 2M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0' };
ICONS.fire = { main: 'M12 3c1 4 6 6 6 11a6 6 0 1 1-12 0c0-3 2-4 3-6c1 2 2 2 3-5' };
const PLAY_ICON = '<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path fill="#FFF" d="M8 5l11 7-11 7z"/></svg>';
const homeIcon = (name, size = 22) => icon(name, size, 'currentColor', false);
const frameLight = () => '<svg class="border-runner" aria-hidden="true"><line x1="0" x2="100%" y1="0" y2="0" pathLength="100"/></svg>';
const edgeRun = new Map();
function animateEdge(svg) {
  if (!svg.isConnected || document.hidden || motionPreference.matches) return;
  const line = svg.querySelector('line'), direction = Math.random() > .5 ? 1 : -1, start = direction > 0 ? -115 : 20, end = direction > 0 ? 20 : -115;
  svg.style.top = Math.random() > .5 ? '-1px' : 'calc(100% + 1px)';
  line.style.strokeDasharray = `${9 + Math.random() * 12} 185`;
  const glide = .28 + Math.random() * .28, run = line.animate([
    { strokeDashoffset: start, opacity: 0, offset: 0 }, { strokeDashoffset: start + (end - start) * .12, opacity: 1, offset: .12 },
    { strokeDashoffset: start + (end - start) * (.48 + Math.random() * .25), opacity: 1, offset: glide }, { strokeDashoffset: end, opacity: 0, offset: .82 }, { strokeDashoffset: end, opacity: 0, offset: 1 },
  ], { duration: 2800 + Math.random() * 4300, delay: Math.random() * 1300, easing: 'ease-in-out' });
  edgeRun.set(svg, run);
  run.onfinish = () => { edgeRun.set(svg, null); if (svg.isConnected) animateEdge(svg); else edgeRun.delete(svg); };
}
function syncEdgeLights() {
  edgeRun.forEach((run, svg) => { if (!svg.isConnected) { run?.cancel(); edgeRun.delete(svg); } });
  document.querySelectorAll('.border-runner').forEach(svg => { if (!edgeRun.has(svg)) animateEdge(svg); });
}
document.addEventListener('visibilitychange', () => { if (!document.hidden) { edgeRun.clear(); syncEdgeLights(); } });

const PHASES = {
  morning: { label: 'MAÑANA', title: 'Empieza a tu ritmo.', note: 'Estírate.', mood: 'morning' },
  commute: { label: 'EN CAMINO', title: 'Ya estás en marcha.', note: 'Sacúdete.', mood: 'commute' },
  work: { label: 'TRABAJO', title: 'Haz una pausa.', note: 'Muévete.', mood: 'work' },
  home: { label: 'EN CASA', title: 'Ya llegaste.', note: 'Suelta el día.', mood: 'home' },
  training: { label: 'HORA DE ENTRENAR', title: 'Este momento es tuyo.', note: 'Vamos.', mood: 'training' },
  night: { label: 'NOCHE', title: 'Baja el ritmo.', note: 'Descansa.', mood: 'night' },
};
const GROUP_NAMES = { all: 'Todos', ...Object.fromEntries(HOST.muscles) };
const groupIcon = g => g === 'all' ? 'grid' : g;
const routineGroups = plan => [...new Set([...(plan.groups || []), ...plan.items.map(item => item.group)])].filter(g => g !== 'all' && GROUP_NAMES[g]);
const planName = plan => plan.groups.map(g => GROUP_NAMES[g]).join(' y ') || 'Descanso';
const clone = value => JSON.parse(JSON.stringify(value));
const H = { startTime: HOST.startTime, day: HOST.todayIndex, group: 'all', drafts: clone(HOST.week), chosen: clone(HOST.chosen), detail: null, detailDay: null, lastDeleted: null, sheetView: null, picker: null, nextId: 1, phase: HOST.phase };

function saveHome(chosen) {
  try { AndroidSession.savePlans(JSON.stringify({ days: H.drafts, chosen: chosen || null })); } catch (_) {}
}
/** A short answer from the person to what was just touched. */
function answer(kind, el) {
  const figure = $('fig'); if (!figure) return;
  if (el) { const r = el.getBoundingClientRect(); figure._fig?.lookAtClient(r.left + r.width / 2, r.top + r.height / 2, 1600); }
  react(figure, kind);
}
const DOCK = [['today', 'Hoy'], ['week', 'Semana'], ['goal', 'Metas'], ['gear', 'Ajustes']];
/** A session under way is a dot on Hoy; Semana counts the days left to train. */
function dockBadge(key) {
  if (key === 'today' && S.startedAt && !S.finished) return '<i class="dock-badge dot"></i>';
  if (key !== 'week') return '';
  const left = H.drafts.filter((_, i) => ['now', 'later'].includes(weekState(i))).length;
  return left ? `<i class="dock-badge">${left}</i>` : '';
}
function nav(active = 'today') {
  return `<div class="dock" id="dock" data-stunt="climb">${DOCK.map(([key, label]) => `<button class="dock-btn ${key === active ? 'active' : ''}" data-dock="${key}"><span class="dock-tile">${homeIcon(key, 24)}${dockBadge(key)}</span><span class="dock-label">${label}</span></button>`).join('')}</div>`;
}
function homeBar(title) {
  return `<div class="topbar"><h2 class="shiny-text">${htmlText(title)}</h2><button class="tool" data-nav="gear" data-stunt="top point" aria-label="Ajustes">${icon('gear', 22)}</button></div>`;
}

// ---------- Hoy ----------
general = function() {
  if (S.finished) { S.stage = 'end'; end(); return; }
  const phase = PHASES[H.phase] || PHASES.morning, plan = H.chosen, started = !!S.startedAt, count = plan.items.length, canTrain = started || count > 0;
  const facts = [plan.minutes && plan.minutes + ' min', count && (count === 1 ? '1 ejercicio' : count + ' ejercicios')].filter(Boolean).join(' · ');
  $('view').className = 'hoy';
  $('view').innerHTML = `${homeBar(HOST.dateLabel)}
    <h1 class="phase-title shiny-text">${phase.title}</h1>
    ${started ? `<div class="today-box resume-note"><strong>${htmlText(resumeLabel())}</strong><span class="num" id="resumeClock"></span></div>` : count ? `<button class="today-box today-time" data-action="edit-time" aria-label="Cambiar la hora"><span class="today-tile">${homeIcon('clock', 24)}</span><span><small>Entrenas a las</small><strong class="num">${H.startTime}</strong> <small>${timeLeft()}</small></span></button>` : ''}
    <section class="today-box today-card">
      <div class="today-head"><h2 class="today-name">${htmlText(plan.name)}</h2><button class="today-see" data-action="open-routine" data-stunt="point hang"><strong class="routine-title">Ver rutina</strong>${homeIcon('arrow', 17)}</button></div>${facts ? `<p class="today-facts">${facts}</p>` : ''}
      ${canTrain ? `<div class="today-go">${exerciseMarks(false)}<button class="today-start" data-action="plain-routine" data-stunt="climb point" aria-label="${started ? 'Seguir tu rutina' : 'Iniciar tu rutina'}">${PLAY_ICON.replace(/16/g, '22')}</button></div>` : ''}</section>
    ${canTrain ? `<button class="today-box today-guided" data-action="guided-routine" data-stunt="point hang"><span class="guided-art">${fig(64, phase.mood, 'fig')}</span><strong>${started ? 'Seguir rutina guiada' : 'Rutina guiada'}</strong></button>` : ''}
    ${excessRow()}
    ${canTrain ? '<div class="today-rest"></div>' : `<div class="stage" id="stage">${fig(150, phase.mood, 'fig', true)}</div><div class="phase-note">${phase.note}</div>`}
    ${nav()}<div class="scrim" id="scrim" hidden></div>`;
  mountFigs();
  if (!canTrain) $('fig').addEventListener('click', () => react($('fig')));
  startGestureCleanup?.(); startGestureCleanup = null;
  paintResumeClock();
  syncEdgeLights();
};
/** How long until the training time, while it is still ahead today. */
function timeLeft() {
  const [hours, minutes] = H.startTime.split(':').map(Number), now = new Date(), left = hours * 60 + minutes - now.getHours() * 60 - now.getMinutes();
  return left <= 0 ? '' : left < 60 ? `Faltan ${left} min` : `Faltan ~${Math.round(left / 60)} h`;
}
/** One mark per exercise, like the phases of Rutinas: white done, red now, gray to come. Its dots are the sets. */
function exerciseMarks(pickable) {
  const now = S.restDeadline ? S.next : S.exIdx, tag = pickable ? 'button' : 'span';
  return `<div class="ex-marks" id="exMarks">${S.choices.map((item, i) => {
    const rows = S.sets[i], state = rows.length && rows.every(row => row.done) ? 'done' : S.startedAt && i === now ? 'now' : '';
    return `<${tag} class="ex-mark ${state}" ${pickable ? `data-jump="${i}" aria-label="${htmlText(item.name)}"` : ''}>${homeIcon(GROUP_NAMES[item.zone] ? groupIcon(item.zone) : 'grid', 22)}<span class="week-dots">${rows.map(row => `<i class="${row.done ? 'on' : ''}"></i>`).join('')}</span></${tag}>`;
  }).join('')}</div>`;
}
function editStartTime() {
  plainSheet('Hora de entrenar', `<form id="timeForm"><label class="field">Empiezas a las<input name="time" type="time" required value="${H.startTime}"></label><button type="submit" class="save-button" style="margin-top:16px">Guardar</button></form>`);
  $('timeForm').onsubmit = e => {
    e.preventDefault(); const value = String(new FormData(e.currentTarget).get('time'));
    try { AndroidSession.saveStartTime(value); } catch (_) {}
    H.startTime = value; closeHomeSheet(true); general(); answer('thumbOnce');
  };
}
function excessRow() {
  const list = HOST.excess || []; if (!list.length) return '';
  const item = list[H.excess = (H.excess || 0) % list.length];
  return `<button class="routine-link shine-frame excess-link" data-action="next-excess" aria-label="Otra opción">${frameLight()}<span class="num">+${item.over}</span><span style="flex:1"><strong>${htmlText(item.name)}</strong><small>${item.minutes} min · ≈ ${item.kcal} kcal</small></span>${homeIcon('fire', 20)}</button>`;
}

// ---------- Tu rutina without the guide: one card that changes in place ----------
/** What the card's button does now; the routine ends on the last set left. */
function plainStep(sets, index) {
  const left = sets.flat().filter(row => !row.done).length, here = sets[index].some(row => !row.done);
  return here ? (left === 1 ? 'end' : 'rest') : left ? 'next' : 'end';
}
function enterPlain() {
  if (!S.startedAt) { S.startedAt = Date.now(); S.endedAt = null; S.seenAt = null; }
  S.warmDeadline = null; S.warmPaused = false; S.selection = null;
  const pending = nextPending(-1);
  if (pending < 0) { go('end', true); return; }
  if (!S.restDeadline && S.sets[S.exIdx].every(row => row.done)) S.exIdx = pending;
  go('plain');
}
/** Both ways share the session: guided picks up the same exercise or the same rest. */
function enterGuided() {
  if (!S.startedAt) { go('start'); return; }
  const target = S.stage === 'plain' ? 'plain' : resumeTarget();
  go(target !== 'plain' ? target : S.restDeadline ? 'rest' : 'ex', true);
}
function plainSets() {
  const rows = S.sets[S.exIdx], current = rows.findIndex(row => !row.done), timed = exerciseAt(S.exIdx).timed;
  return rows.map((row, i) => `<div class="setwrap" data-i="${i}"><div class="swipeRow"><div class="trash" onclick="delSet(${i})">${icon('trash', 22, '#F5F5F5', false)}</div><div class="set"><span class="sn ${i === current ? 'cur' : ''}">${i + 1}</span>
    <button class="val" data-step="${i}">${row.r}<small>${timed ? 'SEG' : 'REPS'}</small></button><button class="val" data-step="${i}">${row.kg}<small>KG</small></button>
    <button class="chk ${row.done ? 'done' : i === current ? 'cur' : ''}" data-mark="${i}" aria-label="Marcar serie ${i + 1}">${row.done ? icon('check', 14, '#000', false) : ''}</button></div></div>
    <div class="stepper ${H.plainStep === i ? 'open' : ''}"><div class="step"><button data-bump="${i}:r:-1">−</button><span>${row.r} ${timed ? 's' : 'reps'}</span><button data-bump="${i}:r:1">+</button></div>
    <div class="step"><button data-bump="${i}:kg:-2.5">−</button><span>${row.kg} kg</span><button data-bump="${i}:kg:2.5">+</button></div></div></div>`).join('') + '<button class="add" data-add-set aria-label="Agregar serie">+</button>';
}
const guidedLink = '<button class="text-action" data-action="guided-routine">Rutina guiada</button>';
function plainWork() {
  const item = exerciseAt(S.exIdx), info = [['POSTURA', item.posture], ['TIPS', item.tips]].filter(([, text]) => text);
  return `<div class="pickCard restCard" id="restCard" data-stunt="climb point"><div class="restHandle"></div>
    <div class="plain-head" id="plainHead"><span class="week-art">${homeIcon(GROUP_NAMES[item.zone] ? groupIcon(item.zone) : 'grid', 24)}</span><h3 class="shiny-text">${htmlText(item.name)}</h3><button class="tool" data-action="plain-change" aria-label="Cambiar ejercicio">${homeIcon('swap')}</button></div>
    <div class="sets plain-sets" id="plainSets">${plainSets()}</div><div id="area"></div>
    <div class="plain-more" id="plainMore">${info.map(([label, text]) => `<div class="sec">${label}</div><p>${htmlText(text)}</p>`).join('')}</div>
    <div class="restSkip">${slidePill({ rest: 'Descanso', next: 'Continuar', end: 'Terminar' }[plainStep(S.sets, S.exIdx)])}${guidedLink}</div></div>`;
}
/** The card stands [height] taller over the stage, and the person rides its edge. */
function stretchPlain(height) {
  const more = $('plainMore');
  $('stage').style.transform = height ? `translateY(${-height}px)` : '';
  if (!more) return;
  more.style.height = `${height}px`; $('restCard').style.marginTop = `${-height}px`;
}
/** The information opens under the sets: it never goes past the person, and what does not fit scrolls. */
const plainRoom = () => Math.min($('plainMore').scrollHeight, Math.max($('stage').offsetHeight - 130, 120));
function bindPlainPull(card) {
  let origin = null, start = 0, room = 0, height = 0, pulling = false;
  card.addEventListener('pointerdown', e => {
    if (e.target.closest('.skipSlide, .timerControls, .trash, .scrolls')) return;
    room = plainRoom(); start = height = H.plainOpen ? room : 0; pulling = false;
    origin = { x: e.clientX, y: e.clientY, id: e.pointerId };
  });
  card.addEventListener('pointermove', e => {
    if (!origin || e.pointerId !== origin.id) return;
    const dx = e.clientX - origin.x, dy = e.clientY - origin.y;
    if (!pulling) {
      if (Math.abs(dx) > 12) { origin = null; return; }
      if (Math.abs(dy) < 12 || !room) return;
      pulling = true; origin.y = e.clientY; card.setPointerCapture(e.pointerId); card.classList.add('dragging'); haptic('tick');
    }
    height = clamp(start + origin.y - e.clientY, 0, room); stretchPlain(height);
  });
  const release = () => {
    origin = null; if (!pulling) return;
    pulling = false; daySwipeUntil = Date.now() + 350; card.classList.remove('dragging');
    const open = height > room * (start ? .75 : .25);
    if (open !== H.plainOpen) { H.plainOpen = open; haptic('tick'); react($('fig'), open ? 'lookOnce' : 'nod'); }
    stretchPlain(open ? room : 0); fitPlain();
  };
  card.addEventListener('pointerup', release); card.addEventListener('pointercancel', release);
}
/** A list that overflows scrolls with the finger; anywhere else the finger pulls the card. */
function fitPlain() {
  const sets = $('plainSets'), more = $('plainMore');
  sets.classList.toggle('scrolls', sets.scrollHeight > sets.clientHeight + 1);
  more.classList.toggle('scrolls', !!H.plainOpen && more.scrollHeight > plainRoom());
}
function paintPlain(fresh = false) {
  const host = $('plainCard'); if (!host) return;
  const resting = !!S.restDeadline, picking = H.plainPick && !resting;
  if (fresh) H.plainOpen = false;
  if (resting) restSelection();
  $('exMarks').outerHTML = exerciseMarks(true);
  host.innerHTML = resting ? `<div class="huge" id="restClock" data-stunt="hang">${fmt(Math.ceil(S.restLeft))}</div>${pickCardHtml(selectionSummary, slidePill('Saltar descanso') + guidedLink)}`
    : picking ? pickCardHtml(selectionSummary, '<button class="btn" id="selectContinue" onclick="confirmSelection()">Seguir</button><button class="text-action" data-action="plain-cancel">Cancelar</button>')
    : plainWork();
  if (resting) { restFigure(); bindPickCard(false); bindSkipSlide(host.querySelector('.skipSlide'), () => endRest()); }
  else if (picking) bindPickCard(false, () => setFig($('fig'), movementFor(selectionItem(), true)));
  else {
    syncSetTimer(); setFig($('fig'), 'coach');
    host.querySelectorAll('.setwrap').forEach(bindRowSwipe);
    bindSkipSlide(host.querySelector('.skipSlide'), plainGo);
    bindPlainPull($('restCard'));
    swipe($('plainHead'), { right: openNext, left: openDash });
    stretchPlain(H.plainOpen ? plainRoom() : 0); fitPlain();
  }
  if (resting || picking) stretchPlain(0);
  if (fresh && !motionPreference.matches) $('restCard').animate([{ opacity: 0, transform: 'translateY(10px)' }, { opacity: 1, transform: 'none' }], { duration: 220, easing: 'ease-out' });
  syncEdgeLights(); sessionCheckpoint();
}
function plain() {
  H.plainStep = null; H.plainPick = false; H.plainOpen = false;
  $('view').className = 'hoy';
  $('view').innerHTML = `<div class="topbar"><button class="tool" data-nav="today" aria-label="Volver a Hoy">${homeIcon('back')}</button><span class="big js-clock plain-clock">${fmt(elapsed())}</span><button class="tool" data-nav="gear" data-stunt="point" aria-label="Ajustes">${icon('gear', 22)}</button></div>
    ${exerciseMarks(true)}<div class="stage" id="stage"><span class="chev" style="left:0">‹</span><span class="chev" style="right:0">›</span>${fig(120, 'coach', 'fig', true)}</div>
    <section class="plain-card" id="plainCard"></section>
    <div class="panel left" id="pNext"></div><div class="panel right" id="pDash"></div>`;
  swipe($('stage'), { right: openNext, left: openDash, tap: () => react($('fig'), 'nod') });
  paintPlain();
}
function plainRest() {
  S.restLeft = S.restTotal = library.restSeconds; S.restOver = 0; S.restDeadline = Date.now() + S.restLeft * 1000;
  S.exerciseDeadline = null; S.exercisePaused = false; S.exerciseLeft = 0; S.timerKey = null;
}
function plainGo() {
  H.plainStep = null;
  const rows = S.sets[S.exIdx], step = plainStep(S.sets, S.exIdx), current = rows.find(row => !row.done);
  if (current) current.done = true;
  haptic(step === 'rest' ? 'confirm' : 'success');
  if (step === 'end') { go('end', true); return; }
  if (step === 'next') S.exIdx = nextPending(S.exIdx);
  else { S.next = rows.some(row => !row.done) ? S.exIdx : nextPending(S.exIdx); plainRest(); }
  paintPlain(true); react($('fig'), step === 'next' ? 'thumbOnce' : 'cheer');
}
function plainMark(index) {
  const row = S.sets[S.exIdx][index]; row.done = !row.done;
  haptic(row.done ? 'confirm' : 'tick'); paintPlain(); react($('fig'), row.done ? 'cheer' : 'lookOnce');
}
function plainBump(index, key, delta) {
  const row = S.sets[S.exIdx][index];
  if (key === 'kg' && row.kg + delta > 500 || key === 'r' && row.r + delta > 86400) return;
  row[key] = Math.max(0, +(row[key] + delta).toFixed(1)); H.plainStep = index;
  paintPlain(); react($('fig'), key === 'kg' && delta > 0 ? 'flexOnce' : 'nod');
}
function plainAddSet() {
  const rows = S.sets[S.exIdx], last = rows.at(-1) || { r: 10, kg: 0 };
  rows.push({ r: last.r, kg: last.kg, done: false });
  paintPlain(); react($('fig'), 'thumbOnce');
}
const guidedDelSet = delSet;
delSet = function(index) {
  if (S.stage !== 'plain') { guidedDelSet(index); return; }
  S.sets[S.exIdx].splice(index, 1); H.plainStep = null;
  haptic('warn'); paintPlain(); react($('fig'), 'shockOnce');
};
function plainJump(index) {
  if (S.restDeadline) S.next = index; else S.exIdx = index;
  H.plainStep = null; H.plainPick = false; paintPlain(true);
}
/** Others in the exercise's group, one swipe at a time; the last card opens every group and exercise. */
function plainChange() {
  const current = exerciseAt(S.exIdx), others = selectionCatalog('exercise').filter(item => item.zone === current.zone && item.id !== current.id);
  S.selection = { kind: 'exercise', slot: S.exIdx, options: [current, ...others], index: 1, returnStage: 'plain' };
  H.otherGroup = current.zone; H.plainPick = true; H.plainStep = null; paintPlain(true);
}
function plainCancel() { S.selection = null; H.plainPick = false; paintPlain(true); }
function plainChosen(item) {
  if (!item?.id) item = selectionItem();
  if (!item) { openOthers(); return; }
  closeDetail();
  const slot = S.selection.slot;
  replaceExercise(slot, item);
  S.exIdx = slot; S.restDeadline = null; S.restOver = 0; S.selection = null; H.plainPick = false;
  paintPlain(true); react($('fig'), 'nod');
}

// ---------- Terminaste ----------
end = function() {
  S.endedAt ||= Date.now(); S.finished = true;
  const completed = S.sets.reduce((total, rows) => total + rows.filter(s => s.done).length, 0), minutes = Math.round(elapsed() / 60);
  $('view').className = 'hoy';
  $('view').innerHTML = `${homeBar(HOST.dateLabel)}
    <h1 class="phase-title shiny-text">${htmlText(S.closingPhrase)}</h1>
    <div class="stage" id="stage">${fig(176, 'party', 'fig', true)}</div>
    <div class="done-card shine-frame" data-stunt="climb point">${frameLight()}
      <div class="stats"><div class="stat"><strong>${elapsed() ? minutes : '—'}<span> min</span></strong><small>Tu tiempo</small></div><div class="stat"><strong>${completed}</strong><small>Series registradas</small></div></div>
      <button class="done-goals" data-nav="goal"><span>Ver mis metas</span>${homeIcon('arrow', 17)}</button></div>
    <button class="reset-pill" data-hold="reset">Reiniciar rutina</button>
    ${nav()}`;
  $('fig').addEventListener('click', () => react($('fig')));
  syncEdgeLights();
};

// ---------- Semana ----------
ANIMS.weekCheer = { f: [['excited', 240], ['excitedUp', 240]] };
ANIMS.weekWait = { f: [['stand', 900], ['lookDown', 700], ['standC', 900]] };
ANIMS.weekSleep = { f: [['sleepBack', 2000], ['sleepBackB', 2000]] };
ANIMS.weekRest = { f: [['sleepyCross', 2000], ['sleepyCrossB', 2400]] };
ANIMS.weekMissed = { f: [['lookDown', 1200], ['stand', 1200]] };
const WEEK_MOODS = { done: 'weekCheer', now: 'weekWait', later: 'weekSleep', rest: 'weekRest', missed: 'weekMissed' };
const weekPlan = i => i === HOST.todayIndex ? H.chosen : H.drafts[i];
/** What the day is to the person: it celebrated, it waits for you, it sleeps until its turn. */
function weekState(i) {
  const today = HOST.todayIndex, trained = i === today ? S.finished || HOST.completed : !!HOST.weekDone?.[i];
  if (trained) return 'done';
  if (!weekPlan(i).items.length) return 'rest';
  return i === today ? 'now' : i < today ? 'missed' : 'later';
}
function weekLine(info, reps, kg) {
  const amount = info.timed ? fmt(reps) : `${reps} reps`;
  return kg ? `${amount} · ${kg} kg` : amount;
}
/** One row per exercise. Dots are its series: white the ones done. */
function weekRows(i) {
  const plan = weekPlan(i);
  if (i === HOST.todayIndex) return S.choices.map((item, slot) => {
    const rows = S.sets[slot];
    return { name: item.name, group: item.zone, line: rows[0] ? weekLine(item, rows[0].r, rows[0].kg) : '', sets: rows.map(row => row.done) };
  });
  const logged = (HOST.history || []).filter(row => row.date === HOST.weekDates?.[i]);
  if (logged.length) {
    const known = [...plan.items.map(item => ({ ...item, zone: item.group })), ...selectionCatalog('exercise')];
    return [...new Set(logged.map(row => row.exerciseId))].map(id => {
      const rows = logged.filter(row => row.exerciseId === id), info = known.find(item => item.id === id) || { name: id, zone: 'all' }, best = rows.reduce((a, b) => b.kg > a.kg ? b : a);
      return { name: info.name, group: info.zone, line: weekLine(info, best.r, best.kg), sets: rows.map(() => true) };
    });
  }
  return plan.items.map(item => ({ name: item.name, group: item.group, line: item.group === 'cardio' ? `${item.reps} min` : weekLine(item, item.reps, item.kg), sets: Array(item.sets).fill(false) }));
}
function weekDetail() {
  const i = H.weekDay, plan = weekPlan(i), state = weekState(i), rows = weekRows(i), day = H.drafts[i];
  return `<div class="week-head"><div><h3 class="shiny-text">${htmlText(plan.items.length || state === 'done' ? plan.name : 'Descanso')}</h3><small>${htmlText(day.day)}, ${day.date.toLowerCase()}</small></div>${plan.minutes && state !== 'done' ? `<span class="num">${plan.minutes} min</span>` : ''}</div>
    <div class="week-rows ${state === 'missed' ? 'dim' : ''}">${rows.map(row => `<div class="week-row"><span class="week-art">${homeIcon(GROUP_NAMES[row.group] ? groupIcon(row.group) : 'grid', 22)}</span><span class="week-text"><strong>${htmlText(row.name)}</strong><small>${htmlText(row.line)}</small></span><span class="week-dots">${row.sets.map(done => `<i class="${done ? 'on' : ''}"></i>`).join('')}</span></div>`).join('')}</div>
    ${i >= HOST.todayIndex && state !== 'done' ? `<button class="routine-link shine-frame" data-action="week-routine">${frameLight()}<strong class="routine-title">Ver tu rutina</strong>${homeIcon('arrow', 17)}</button>` : ''}`;
}
/** Only the chosen day's person moves; the others hold still. */
function syncWeekFigures() {
  document.querySelectorAll('.week-day .rig').forEach(svg => { if (svg._fig) svg._fig.paused = +svg.dataset.day !== H.weekDay; });
}
function pickWeekDay(index) {
  const i = clamp(index, 0, 6), same = i === H.weekDay; H.weekDay = i;
  if (same) { react(document.querySelector(`.week-day [data-day="${i}"]`), weekState(i) === 'done' ? 'cheer' : 'nod'); return; }
  document.querySelectorAll('.week-day').forEach((el, n) => el.setAttribute('aria-pressed', n === i));
  haptic('tick'); $('weekDetail').innerHTML = weekDetail(); syncWeekFigures(); syncEdgeLights();
}
function weekView() {
  H.weekDay ??= HOST.todayIndex;
  $('view').className = 'hoy';
  $('view').innerHTML = `${homeBar('Semana')}
    <div class="week-grid">${H.drafts.map((day, i) => `<button class="week-day ${i === HOST.todayIndex ? 'today' : ''}" data-week="${i}" aria-pressed="${i === H.weekDay}" aria-label="${htmlText(day.day)}"><span>${['LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB', 'DOM'][i]}</span>${fig(50, WEEK_MOODS[weekState(i)]).replace('class="rig"', `class="rig" data-day="${i}"`)}</button>`).join('')}</div>
    <div class="week-detail" id="weekDetail">${weekDetail()}</div>
    ${nav('week')}`;
  mountFigs();
  document.querySelectorAll('.week-day .rig').forEach(svg => {
    const figure = svg._fig; if (!figure) return;
    if (weekState(+svg.dataset.day) === 'later') { figure.loopSeg.zzz = true; figure.seg.zzz = true; }
    figure.tick(performance.now());
  });
  syncWeekFigures();
  bindDaySwipe($('weekDetail'), step => pickWeekDay(H.weekDay + step));
  syncEdgeLights();
}

// ---------- Metas: series by day or week, and the best mark of each exercise ----------
const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const SHADES = ['#F5F5F5', '#C4C4C4', '#9A9A9A', '#767676', '#5C5C5C', '#474747', '#363636', '#2A2A2A'];
const dateOf = iso => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d); };
const dayLabel = iso => { const date = dateOf(iso); return `${date.getDate()} ${MONTHS[date.getMonth()]}`; };
function shiftDay(iso, days) {
  const date = dateOf(iso); date.setDate(date.getDate() + days);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
const mondayOf = iso => shiftDay(iso, -((dateOf(iso).getDay() + 6) % 7));
const beats = (a, b) => a.kg > b.kg || a.kg === b.kg && a.r > b.r;
/** Everything logged, oldest first, with today's marked sets on the end. */
function allLogs() {
  const today = S.choices.flatMap((item, slot) => S.sets[slot].filter(row => row.done).map(row => ({ exerciseId: item.id, date: HOST.date, r: row.r, kg: row.kg })));
  return [...(HOST.history || [])].sort((a, b) => a.date.localeCompare(b.date)).concat(today);
}
/** The best mark per exercise, and which exercises beat theirs on each date. The first time is not a record. */
function personalBests(logs) {
  const best = new Map(), records = new Map();
  for (const row of logs) {
    const mark = best.get(row.exerciseId);
    if (mark && beats(row, mark)) { if (!records.has(row.date)) records.set(row.date, new Set()); records.get(row.date).add(row.exerciseId); }
    if (!mark || beats(row, mark)) best.set(row.exerciseId, row);
  }
  return { best, records };
}
function exerciseInfo(id) {
  const planned = H.drafts.flatMap(day => day.items).find(item => item.id === id);
  return planned ? { ...planned, zone: planned.group } : selectionCatalog('exercise').find(item => item.id === id) || S.choices.find(item => item.id === id) || { name: id, zone: 'all' };
}
function goalColumns(range) {
  const logs = allLogs(), { records } = personalBests(logs), weeks = range === 'week';
  const last = weeks ? mondayOf(HOST.date) : HOST.date, count = weeks ? 12 : 20, step = weeks ? 7 : 1;
  const keys = Array.from({ length: count }, (_, i) => shiftDay(last, (i - count + 1) * step));
  const groups = [...HOST.muscles.map(([key]) => key), 'all'];
  return keys.map(key => {
    const rows = logs.filter(row => (weeks ? mondayOf(row.date) : row.date) === key);
    const stacks = groups.map((group, i) => ({ name: GROUP_NAMES[group], color: SHADES[i], value: rows.filter(row => (GROUP_NAMES[exerciseInfo(row.exerciseId).zone] ? exerciseInfo(row.exerciseId).zone : 'all') === group).length })).filter(stack => stack.value);
    const marks = [...records].filter(([date]) => (weeks ? mondayOf(date) : date) === key).reduce((sum, [, ids]) => sum + ids.size, 0);
    return { label: weeks ? `Semana del ${dayLabel(key)}` : dayLabel(key), short: dayLabel(key), total: rows.length, stacks, marks, rows: stacks.map(stack => [stack.color, stack.name, stack.value]) };
  });
}
function chartHtml() {
  const { columns, selected } = H.chart, unit = 132 / Math.max(1, ...columns.map(column => column.total)), picked = columns[selected];
  const side = selected >= columns.length / 2 ? `right:${100 - (selected + .5) * 100 / columns.length}%` : `left:${(selected + .5) * 100 / columns.length}%`;
  return `<div class="chart-bars ${picked ? 'picking' : ''}">${columns.map((column, i) => `<button class="bar ${i === selected ? 'on' : ''}" data-bar="${i}" aria-label="${htmlText(column.label)}: ${column.total}"><span class="bar-up">${column.stacks.map(stack => `<i style="height:${Math.max(4, stack.value * unit).toFixed(1)}px;background:${stack.color}"></i>`).join('')}</span><span class="bar-down">${column.marks ? `<i style="height:${Math.min(26, 6 + column.marks * 6)}px"></i>` : ''}</span></button>`).join('')}</div>
    ${picked ? `<div class="chart-tip" style="${side}"><div class="tip-head"><span>${htmlText(picked.label)}</span><strong class="num">${picked.headline ?? picked.total}</strong></div>${picked.rows.map(([color, name, value]) => `<div class="tip-row"><i style="background:${color}"></i><span>${htmlText(name)}</span><strong class="num">${htmlText(String(value))}</strong></div>`).join('')}${picked.marks ? `<div class="tip-row"><i class="red"></i><span>Récords</span><strong class="num">${picked.marks}</strong></div>` : ''}</div>` : ''}`;
}
function pickBar(index) { H.chart.selected = H.chart.selected === index ? -1 : index; $('chart').innerHTML = chartHtml(); }
const markLine = (info, row) => row.kg ? `${info.timed ? fmt(row.r) : row.r + ' reps'} · ${row.kg} kg` : info.timed ? fmt(row.r) : `${row.r} reps`;
function goalsView() {
  const range = H.range ||= 'day', columns = goalColumns(range), logs = allLogs(), { best } = personalBests(logs);
  H.chart = { columns, selected: -1 };
  const marks = [...best.values()].map(row => ({ row, info: exerciseInfo(row.exerciseId) })).sort((a, b) => b.row.date.localeCompare(a.row.date));
  $('view').className = 'hoy';
  $('view').innerHTML = `${homeBar('Metas')}
    <div class="goal-head"><div class="goal-total"><strong class="num">${columns.reduce((sum, column) => sum + column.total, 0).toLocaleString('es-MX')}</strong><span>series</span></div><div class="range">${[['day', 'Día'], ['week', 'Semana']].map(([key, label]) => `<button class="${key === range ? 'on' : ''}" data-range="${key}">${label}</button>`).join('')}</div></div>
    <div class="chart" id="chart">${chartHtml()}</div>
    <div class="chart-ends"><span>${columns[0].short}</span><span>${columns.at(-1).short}</span></div>
    <div class="week-rows goal-rows">${marks.map(({ row, info }) => `<div class="week-row"><span class="week-art">${homeIcon(GROUP_NAMES[info.zone] ? groupIcon(info.zone) : 'grid', 22)}</span><span class="week-text"><strong>${htmlText(info.name)}</strong><small>${htmlText(markLine(info, row))}</small></span><span class="goal-date">${dayLabel(row.date)}</span></div>`).join('')}</div>
    <button class="routine-link shine-frame body-link" data-action="open-body">${frameLight()}<strong>Peso y medidas</strong>${homeIcon('arrow', 17)}</button>
    ${nav('goal')}`;
  syncEdgeLights();
}

// ---------- Tu rutina ----------
let galleryCleanup = null, daySwipeUntil = 0;
function routine() {
  closePicker(); galleryCleanup?.();
  const plan = H.drafts[H.day], groups = routineGroups(plan);
  if (H.group !== 'all' && !groups.includes(H.group)) H.group = 'all';
  const cards = ['all', ...groups];
  $('view').className = 'hoy';
  $('view').innerHTML = `<div class="topbar"><button class="tool" data-action="back" aria-label="Volver a Hoy">${homeIcon('back')}</button><h2 class="shiny-text">Tu rutina</h2><button class="tool" data-nav="gear" data-stunt="point" aria-label="Ajustes">${icon('gear', 22)}</button></div>
    <div class="day-strip shine-frame" id="dayStrip" data-stunt="hang point">${frameLight()}<div class="calendar-week">${H.drafts.map((day, i) => `<button class="calendar-day ${i === HOST.todayIndex ? 'today' : ''}" data-day="${i}" aria-pressed="${i === H.day}" aria-label="${htmlText(day.day)}"><span class="day-initial">${'LMMJVSD'[i]}</span><span class="day-number">${Number(day.date.split(' ')[0])}</span></button>`).join('')}</div>
      <div class="day-title"><strong>${htmlText(plan.day)}, ${plan.date.toLowerCase()}</strong><small>${plan.minutes ? plan.minutes + ' MIN' : groups.length ? 'RUTINA LIBRE' : 'DESCANSO'}</small></div></div>
    <div class="overview-stage" id="overview"><div class="gallery-layout"><div class="group-tools"><button class="group-chip" data-action="add-groups" aria-label="Agregar grupos musculares" aria-expanded="false">${homeIcon('plus', 18)}</button><button class="group-chip" data-action="remove-groups" aria-label="Quitar grupos musculares" aria-expanded="false">${homeIcon('minus', 18)}</button></div>
      <div class="muscle-stack" id="gallery">${cards.map(g => `<button class="muscle-card" data-group="${g}" aria-label="${GROUP_NAMES[g]}. Ver ejercicios."><span class="gallery-art">${homeIcon(groupIcon(g), 44)}</span><span>${GROUP_NAMES[g]}</span></button>`).join('')}</div></div>
      <div class="overview-summary"><h3 class="shiny-text">${htmlText(plan.name)}</h3><p>${groups.length} grupos · ${plan.items.length} ejercicios</p></div>
      <div class="routine-walk">${fig(84, 'plan', 'fig', true).replace('class="rig roam"', 'class="rig roam" data-flat')}</div></div>
    <div class="action-footer"><button class="save-button" data-action="save-routine" data-stunt="climb point">Guardar</button></div><div class="scrim" id="scrim" hidden></div>`;
  $('fig').style.pointerEvents = 'none';
  galleryCleanup = bindGallery($('gallery'));
  bindDaySwipe($('dayStrip'), step => selectDay(H.day + step));
  syncEdgeLights();
}
function selectDay(index) { H.day = clamp(index, 0, H.drafts.length - 1); H.group = 'all'; haptic('tick'); routine(); answer('nod'); }
function bindDaySwipe(surface, step) {
  let origin = null;
  surface.addEventListener('pointerdown', e => { origin = { x: e.clientX, y: e.clientY, id: e.pointerId }; });
  surface.addEventListener('pointerup', e => {
    if (!origin || origin.id !== e.pointerId) return;
    const dx = e.clientX - origin.x, dy = e.clientY - origin.y; origin = null;
    if (Math.abs(dx) > 35 && Math.abs(dx) > Math.abs(dy) * 1.3) { daySwipeUntil = Date.now() + 350; step(dx < 0 ? 1 : -1); }
  });
  surface.addEventListener('pointercancel', () => { origin = null; });
}
function bindGallery(gallery) {
  const controller = new AbortController(), options = { signal: controller.signal }, cards = [...gallery.querySelectorAll('.muscle-card')], count = cards.length;
  let selected = Math.max(0, cards.findIndex(c => c.dataset.group === H.group)), position = selected, drag = null, suppressClick = false;
  const wrap = n => ((n % count) + count) % count, step = () => Math.min(58, gallery.clientWidth * .2);
  const paint = () => cards.forEach((card, i) => {
    const offset = wrap(i - position + count / 2) - count / 2, distance = Math.abs(offset);
    card.style.transform = `translateX(calc(-50% + ${offset * step()}px)) translateY(${distance * distance * 10}px) rotate(${offset * 10}deg) scale(${Math.max(.72, 1 - distance * .09)})`;
    card.style.opacity = Math.max(.15, 1 - Math.max(0, distance - 1) * .65);
    card.style.zIndex = String(10 - Math.round(distance * 2));
    card.classList.toggle('active', i === selected);
  });
  const choose = index => { selected = wrap(index); position = selected; paint(); H.group = cards[selected].dataset.group; };
  gallery.addEventListener('pointerdown', e => { suppressClick = false; drag = { id: e.pointerId, x: e.clientX, y: e.clientY, position, locked: false }; }, options);
  gallery.addEventListener('pointermove', e => {
    if (!drag || e.pointerId !== drag.id) return;
    const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
    if (!drag.locked) {
      if (Math.abs(dy) > 10 && Math.abs(dy) > Math.abs(dx)) { drag = null; return; }
      if (Math.abs(dx) < 7) return;
      drag.locked = true; gallery.setPointerCapture(e.pointerId); gallery.classList.add('dragging');
    }
    position = drag.position - dx / step(); paint();
  }, options);
  const finish = (e, cancel) => {
    if (!drag || e.pointerId !== drag.id) return;
    const previous = drag; drag = null; suppressClick = previous.locked; gallery.classList.remove('dragging');
    if (previous.locked && !cancel) { haptic('tick'); answer('nod'); }
    choose(cancel ? selected : Math.round(position));
  };
  gallery.addEventListener('pointerup', e => finish(e, false), options);
  gallery.addEventListener('pointercancel', e => finish(e, true), options);
  gallery.addEventListener('click', e => {
    if (suppressClick) { suppressClick = false; e.preventDefault(); return; }
    const index = cards.indexOf(e.target.closest('.muscle-card')); if (index < 0) return;
    choose(index); answer('thumbOnce', cards[index]); openGroupSheet(cards[index].dataset.group);
  }, options);
  paint();
  return () => controller.abort();
}

// ---------- Add or remove muscle groups ----------
function closePicker() {
  H.picker = null; $('groupPicker')?.remove();
  document.querySelectorAll('.group-chip').forEach(button => button.setAttribute('aria-expanded', 'false'));
}
function openPicker(mode) {
  const wasOpen = H.picker?.mode === mode; closePicker(); if (wasOpen) return;
  const tools = document.querySelector('.group-tools'), trigger = tools.querySelector(`[data-action=${mode === 'add' ? 'add-groups' : 'remove-groups'}]`);
  const current = routineGroups(H.drafts[H.day]), choices = Object.keys(GROUP_NAMES).filter(g => g !== 'all' && (mode === 'add' ? !current.includes(g) : current.includes(g)));
  H.picker = { mode, selected: new Set(), choices }; trigger.setAttribute('aria-expanded', 'true');
  tools.insertAdjacentHTML('beforeend', `<section class="group-picker" id="groupPicker" data-mode="${mode}"><div class="picker-heading"><strong>${mode === 'add' ? 'Agregar grupos' : 'Quitar grupos'}</strong><button class="tool" data-action="cancel-groups" aria-label="Cancelar">${homeIcon('close', 16)}</button></div>
    <div class="picker-list"><div class="picker-highlight" ${choices.length ? '' : 'hidden'}></div>${choices.map(g => `<button class="picker-option" data-pick="${g}" role="checkbox" aria-checked="false">${homeIcon(g, 20)}<span>${GROUP_NAMES[g]}</span><span class="pick-check">${homeIcon('check', 16)}</span></button>`).join('') || `<p class="picker-note">${mode === 'add' ? 'Ya están todos los grupos.' : 'No hay grupos para quitar.'}</p>`}</div>
    ${mode === 'remove' && choices.length ? '<p class="picker-note">También se quitarán sus ejercicios.</p>' : ''}<button class="picker-confirm" data-action="confirm-groups" disabled>Confirmar</button></section>`);
  answer(mode === 'add' ? 'thumbOnce' : 'lookOnce', trigger);
}
function togglePick(group) {
  const p = H.picker; if (!p?.choices.includes(group)) return;
  p.selected.has(group) ? p.selected.delete(group) : p.selected.add(group);
  const picker = $('groupPicker'), row = picker.querySelector(`[data-pick=${group}]`);
  row.setAttribute('aria-checked', String(p.selected.has(group)));
  picker.querySelector('.picker-highlight').style.transform = `translateY(${row.offsetTop}px)`;
  const confirm = picker.querySelector('[data-action=confirm-groups]'); confirm.disabled = !p.selected.size;
}
function confirmPicker() {
  const p = H.picker; if (!p?.selected.size) return;
  const plan = H.drafts[H.day], groups = routineGroups(plan), { mode, selected } = p;
  if (mode === 'add') plan.groups = [...new Set([...groups, ...selected])];
  else { plan.groups = groups.filter(g => !selected.has(g)); plan.items = plan.items.filter(item => !selected.has(item.group)); }
  plan.name = planName(plan); plan.minutes = plan.groups.length ? null : 0;
  H.group = mode === 'add' ? [...selected][0] : 'all';
  routine(); saveHome(); haptic('confirm');
  answer(mode === 'add' ? 'cheer' : 'shockOnce');
}

// ---------- Sheets, with the person spying over each ----------
const currentPlan = () => H.detail || H.drafts[H.day];
function openScrim(html) {
  const scrim = $('scrim'); scrim.hidden = false; scrim.innerHTML = html;
  const sheet = scrim.querySelector('.h-sheet'); spyOn(sheet);
  scrim.onclick = e => { if (e.target === scrim) closeHomeSheet(); };
}
function closeHomeSheet(force = false) {
  if (H.detail && !force && H.sheetView !== 'groups') { renderGroupSheet(); return; }
  H.detail = null; H.detailDay = null; H.sheetView = null; H.lastDeleted = null; $('scrim').hidden = true; $('scrim').innerHTML = '';
}
function openGroupSheet(group) { H.detailDay = H.day; H.detail = clone(H.drafts[H.day]); H.group = group; H.lastDeleted = null; renderGroupSheet(); }
function renderGroupSheet() {
  const groups = ['all', ...routineGroups(currentPlan())];
  if (!groups.includes(H.group)) H.group = 'all';
  H.sheetView = 'groups';
  openScrim(`<section class="h-sheet group-sheet"><div class="handle"></div><div class="group-header"><button class="group-toggle" data-action="toggle-groups">${homeIcon(groupIcon(H.group), 26)}<span class="shiny-text">${GROUP_NAMES[H.group]}</span><span class="chevron">⌄</span></button><button class="tool" data-action="close-sheet" aria-label="Cerrar sin guardar">${homeIcon('close', 20)}</button>
    <div class="group-options" id="groupOptions" hidden>${groups.map(g => `<button data-detail-group="${g}" aria-pressed="${H.group === g}">${homeIcon(groupIcon(g), 22)}${GROUP_NAMES[g]}</button>`).join('')}</div></div>
    <div class="exercise-list" id="exerciseList"></div>${H.lastDeleted ? '<button class="text-action" data-action="undo-delete">Deshacer eliminación</button>' : ''}<button class="save-button" style="margin-top:14px;flex:none" data-action="save-group">Guardar</button></section>`);
  renderExerciseList();
}
function renderExerciseList() {
  const list = $('exerciseList'), visible = currentPlan().items.filter(item => H.group === 'all' || item.group === H.group);
  list.innerHTML = `<div class="list-highlight"></div>` + (visible.map(item => `<div class="exercise-item"><div class="exercise-actions"><button data-edit="${item.id}">${homeIcon('edit', 18)}<span>Editar</span></button><button class="delete" data-delete="${item.id}">${homeIcon('trash', 18)}<span>Borrar</span></button></div>
    <div class="exercise-row" role="button" tabindex="0"><span class="exercise-symbol">${homeIcon(groupIcon(item.group), 22)}</span><div class="exercise-copy"><strong>${htmlText(item.name)}</strong><small>${item.sets} series · ${item.reps} ${item.group === 'cardio' ? 'min' : 'reps'}${item.kg ? ' · ' + item.kg + ' kg' : ''}</small></div></div></div>`).join('') || '<div class="empty">Sin ejercicios.</div>')
    + `<button class="add-exercise" data-action="add-exercise" aria-label="Agregar ejercicio">${homeIcon('plus', 24)}</button>`;
  bindRows(list);
}
/** A row follows the finger: its highlight glides there; a left swipe opens edit and delete. */
function bindRows(list) {
  const highlight = list.querySelector('.list-highlight');
  const setOpen = (row, open) => { row.closest('.exercise-item').classList.toggle('open', open); row.style.transform = `translateX(${open ? -116 : 0}px)`; };
  const glide = row => { highlight.style.height = row.offsetHeight + 'px'; highlight.style.transform = `translateY(${row.closest('.exercise-item').offsetTop}px)`; highlight.style.opacity = '1'; };
  let drag = null, suppress = false;
  list.addEventListener('pointerdown', e => {
    const row = e.target.closest('.exercise-row'); if (!row) return;
    suppress = false; glide(row); list.querySelectorAll('.exercise-row').forEach(other => { if (other !== row) setOpen(other, false); });
    drag = { row, id: e.pointerId, x: e.clientX, y: e.clientY, initial: row.closest('.exercise-item').classList.contains('open') ? -116 : 0, offset: 0, locked: false };
  });
  list.addEventListener('pointermove', e => {
    if (!drag || e.pointerId !== drag.id) return;
    const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
    if (!drag.locked) {
      if (Math.abs(dy) > 10 && Math.abs(dy) > Math.abs(dx)) { drag = null; return; }
      if (Math.abs(dx) < 7) return;
      drag.locked = true; drag.row.setPointerCapture(e.pointerId); drag.row.classList.add('dragging'); drag.row.closest('.exercise-item').classList.add('open');
    }
    drag.offset = clamp(drag.initial + dx, -116, 0); drag.row.style.transform = `translateX(${drag.offset}px)`;
  });
  const finish = e => {
    if (!drag || e.pointerId !== drag.id) return;
    const previous = drag; drag = null; suppress = previous.locked; previous.row.classList.remove('dragging');
    const open = previous.locked ? previous.offset < -45 : previous.initial < 0;
    if (open !== previous.initial < 0) haptic('tick');
    setOpen(previous.row, open);
  };
  list.addEventListener('pointerup', finish); list.addEventListener('pointercancel', finish);
  list.addEventListener('click', e => {
    if (suppress) { suppress = false; return; }
    const row = e.target.closest('.exercise-row'); if (row) setOpen(row, !row.closest('.exercise-item').classList.contains('open'));
  });
  const rows = [...list.querySelectorAll('.exercise-item')];
  if (!motionPreference.matches) rows.forEach((item, i) => item.animate([{ transform: 'translateY(22px) scale(.97)', opacity: 0 }, { transform: 'translateY(-4px) scale(1)', opacity: 1, offset: .6 }, { transform: 'translateY(2px)', opacity: 1, offset: .8 }, { transform: 'translateY(0)', opacity: 1 }], { duration: 520, delay: i * 45, fill: 'backwards', easing: 'cubic-bezier(.2,.65,.3,1)' }));
}
function deleteExercise(id) {
  const plan = currentPlan(), index = plan.items.findIndex(item => item.id === id); if (index < 0) return;
  H.lastDeleted = { index, item: plan.items.splice(index, 1)[0] }; renderGroupSheet();
}
function undoDelete() { if (!H.lastDeleted) return; const { index, item } = H.lastDeleted; H.lastDeleted = null; currentPlan().items.splice(index, 0, item); renderGroupSheet(); }
function plainSheet(title, body) {
  H.sheetView = 'editor';
  openScrim(`<section class="h-sheet"><div class="handle"></div><div class="row" style="margin-bottom:12px"><h3 class="shiny-text" style="margin:0">${title}</h3><button class="tool" data-action="close-sheet" aria-label="Cerrar">${homeIcon('close', 20)}</button></div><div class="h-body">${body}</div></section>`);
}
function editExercise(id) {
  const item = currentPlan().items.find(x => x.id === id); if (!item) return;
  plainSheet('Ajusta tu ejercicio', `<p>${htmlText(item.name)}</p><form id="editForm"><label class="field">Series<input name="sets" type="number" min="1" max="12" required value="${item.sets}"></label><label class="field">${item.group === 'cardio' ? 'Minutos' : 'Repeticiones'}<input name="reps" type="number" min="1" max="180" required value="${item.reps}"></label><label class="field">Peso · kg<input name="kg" type="number" min="0" max="500" step="0.5" required value="${item.kg}"></label><button class="text-action" style="display:block" type="button" data-replace="${id}">Cambiar por otro ejercicio</button><button type="submit" class="save-button" style="margin-top:14px">Guardar ajuste</button></form>`);
  $('editForm').onsubmit = e => { e.preventDefault(); const data = new FormData(e.currentTarget); Object.assign(item, { sets: +data.get('sets'), reps: +data.get('reps'), kg: +data.get('kg') }); renderGroupSheet(); };
}
const catalogItems = () => [...new Map([...HOST.catalog.map(c => ({ name: c.name, group: c.zone, sets: c.sets.length, reps: c.timed ? Math.max(1, Math.round((c.sets[0]?.[0] || 60) / 60)) : c.sets[0]?.[0] || 10, kg: c.sets[0]?.[1] || 0, how: c.posture || '', care: c.tips || '', timed: !!c.timed })), ...H.drafts.flatMap(day => day.items)].map(item => [item.name, item])).values()];
function replaceItem(id) {
  const item = currentPlan().items.find(x => x.id === id); if (!item) return;
  const alternatives = catalogItems().filter(c => c.group === item.group && c.name !== item.name);
  plainSheet('Cambiar ejercicio', `<p>${GROUP_NAMES[item.group]}</p><div class="alternatives">${alternatives.map((c, i) => `<button class="alternative" data-option="${i}">${homeIcon(item.group, 24)}<span><strong>${htmlText(c.name)}</strong></span></button>`).join('') || '<div class="empty">Sin otras opciones.</div>'}</div>`);
  $('scrim').querySelectorAll('[data-option]').forEach(button => button.onclick = () => { const c = alternatives[+button.dataset.option]; Object.assign(item, { name: c.name, how: c.how, care: c.care, kg: 0 }); editExercise(id); });
}
function appendExercise(item) {
  const plan = currentPlan(), added = { ...clone(item), id: 'added-' + Date.now() + '-' + H.nextId++ };
  plan.items.push(added); plan.groups = [...new Set([...(plan.groups || []), added.group])]; plan.name = planName(plan); plan.minutes = plan.minutes || null;
  H.group = added.group; renderGroupSheet(); return added;
}
function openAddExercise() {
  const list = catalogItems().filter(c => H.group === 'all' || c.group === H.group);
  plainSheet('Agregar ejercicio', `<button class="secondary" data-action="create-exercise">${homeIcon('plus', 17)}Crear ejercicio</button><div class="alternatives">${list.map((c, i) => `<button class="alternative" data-add="${i}">${homeIcon(groupIcon(c.group), 24)}<span><strong>${htmlText(c.name)}</strong><small>${GROUP_NAMES[c.group] || ''}</small></span></button>`).join('')}</div>`);
  $('scrim').querySelectorAll('[data-add]').forEach(button => button.onclick = () => editExercise(appendExercise(list[+button.dataset.add]).id));
}
function createExerciseForm() {
  plainSheet('Nuevo ejercicio', `<form id="createForm"><label class="field">Nombre<input type="text" name="name" maxlength="80" required autocomplete="off"></label><label class="field">Grupo<select name="group">${Object.entries(GROUP_NAMES).filter(([k]) => k !== 'all').map(([k, n]) => `<option value="${k}" ${k === H.group ? 'selected' : ''}>${n}</option>`).join('')}</select></label><label class="field">Series<input name="sets" type="number" min="1" max="12" value="3" required></label><label class="field">Repeticiones<input name="reps" type="number" min="1" max="180" value="12" required></label><label class="field">Peso · kg<input name="kg" type="number" min="0" max="500" step="0.5" value="0" required></label><button class="save-button" style="margin-top:14px" type="submit">Agregar</button></form>`);
  $('createForm').onsubmit = e => {
    e.preventDefault(); const data = new FormData(e.currentTarget), name = String(data.get('name')).trim(); if (!name) return;
    const group = String(data.get('group'));
    appendExercise({ name, group, sets: +data.get('sets'), reps: +data.get('reps'), kg: +data.get('kg'), how: '', care: '', timed: group === 'cardio' });
  };
}
function saveGroupSheet() { H.drafts[H.detailDay] = clone(H.detail); closeHomeSheet(true); routine(); saveHome(); haptic('confirm'); answer('thumbOnce'); }
/** Today's routine changes only here; it reloads the scene so the session runs the new plan. */
function applyRoutine() {
  // Another day's plan, reached from Semana: today stays as it is.
  if (H.routineFrom === 'week' && H.day !== HOST.todayIndex) { saveHome(); haptic('confirm'); go('week'); return; }
  H.chosen = clone(H.drafts[H.day]);
  booted = false;
  saveHome(H.chosen);
  try { AndroidSession.reloadInto('general'); location.reload(); } catch (_) { booted = true; go('general'); }
}

// ---------- Others: the same list as the routine ----------
openOthers = function() {
  const o = $('others'), kind = S.selection.kind, all = selectionCatalog(kind), groups = kind === 'exercise' ? ['all', ...new Set(all.map(item => item.zone).filter(g => GROUP_NAMES[g]))] : [];
  H.otherGroup = groups.includes(H.otherGroup) ? H.otherGroup : 'all';
  const shown = all.map((item, i) => [item, i]).filter(([item]) => H.otherGroup === 'all' || item.zone === H.otherGroup);
  o.innerHTML = `<div class="group-header">${groups.length ? `<button class="group-toggle" onclick="$('otherOptions').hidden=!$('otherOptions').hidden">${homeIcon(groupIcon(H.otherGroup), 26)}<span class="shiny-text">${GROUP_NAMES[H.otherGroup]}</span><span class="chevron">⌄</span></button>` : '<div class="group-toggle"><span class="shiny-text">Calentamientos</span></div>'}<button class="tool" aria-label="Cerrar" onclick="$('others').classList.remove('open')">${homeIcon('close', 20)}</button>
    ${groups.length ? `<div class="group-options" id="otherOptions" hidden>${groups.map(g => `<button aria-pressed="${H.otherGroup === g}" onclick="pickOtherGroup('${g}')">${homeIcon(groupIcon(g), 22)}${GROUP_NAMES[g]}</button>`).join('')}</div>` : ''}</div>
    <div class="exercise-list otherList"><div class="list-highlight"></div>${shown.map(([item, i]) => `<div class="exercise-item"><button class="exercise-row" onclick="previewOther(${i})"><span class="exercise-symbol">${homeIcon(item.zone || 'hiit', 22)}</span><span class="exercise-copy"><strong>${htmlText(item.name)}</strong><small>${selectionSummary(item)}</small></span></button></div>`).join('')}
    <button class="add-exercise" aria-label="Agregar" onclick="openNewExercise()">${homeIcon('plus', 24)}</button></div>`;
  const list = o.querySelector('.otherList'), highlight = list.querySelector('.list-highlight');
  list.addEventListener('pointerdown', e => { const row = e.target.closest('.exercise-row'); if (row) { highlight.style.height = row.offsetHeight + 'px'; highlight.style.transform = `translateY(${row.parentElement.offsetTop}px)`; highlight.style.opacity = '1'; } });
  if (!motionPreference.matches) [...list.querySelectorAll('.exercise-item')].forEach((item, i) => item.animate([{ transform: 'translateY(22px) scale(.97)', opacity: 0 }, { transform: 'translateY(-4px) scale(1)', opacity: 1, offset: .6 }, { transform: 'translateY(0)', opacity: 1 }], { duration: 480, delay: i * 40, fill: 'backwards', easing: 'cubic-bezier(.2,.65,.3,1)' }));
  spyOn(o); o.classList.add('open');
  swipe(o, { down: () => o.classList.remove('open') });
};
function pickOtherGroup(group) { H.otherGroup = group; openOthers(); }

// ---------- Descanso: the next exercise changes right on its card ----------
function restSelection() {
  const sel = S.selection;
  if (sel?.kind === 'exercise' && sel.returnStage === 'rest' && sel.slot === S.next) return sel;
  return S.selection = { kind: 'exercise', slot: S.next, options: selectionOptions(S.next), index: 0, returnStage: 'rest' };
}
/** Choosing and its actions live on one card, pulled up for the information and swiped for other options. */
function pickCardHtml(subtitle, footer) {
  return `<div class="pickCard restCard" id="restCard" data-stunt="climb point"><div class="restHandle"></div>
    <div class="restRow"><div class="carousel" id="car"><div class="track" id="trk">${[...S.selection.options, null].map(w => `
      <div class="wcard"><div class="art">${icon(w ? w.zone || 'hiit' : 'grid', 26, '#F5F5F5')}</div><div class="wtext"><div class="wname">${w ? htmlText(w.name) : 'Otros'}</div>${w ? `<div class="num">${subtitle(w)}</div>` : ''}</div></div>`).join('')}</div></div>
      <button class="tool restNext red" id="restNext" aria-label="Ver otro">›</button></div>
    <div class="restSkip">${footer}</div></div>
    <div class="sheet" id="detail"></div><div class="sheet" id="others"></div>`;
}
function bindPickCard(firstShow, onShow = () => {}) {
  const sel = S.selection, card = $('restCard'), last = sel.options.length;
  const show = () => { $('trk').style.transform = `translateX(${-sel.index * 100}%)`; onShow(); };
  const forward = () => { haptic('tick'); if (sel.index < last) { sel.index++; show(); } else openOthers(); };
  show();
  $('restNext').onclick = forward;
  swipe(card, {
    left: forward,
    right: () => { if (sel.index > 0) { haptic('tick'); sel.index--; show(); } },
    up: () => { if (!selectionItem()) openOthers(); },
    tap: () => { if (!selectionItem()) openOthers(); },
  });
  bindPullUp(card);
  // The card rises once on arrival, so it reads as something to pull.
  if (firstShow && !motionPreference.matches) card.animate([{ transform: 'translateY(0)' }, { transform: 'translateY(-14px)', offset: .45 }, { transform: 'translateY(0)' }], { duration: 760, delay: 450, easing: 'ease-in-out' });
}
const prototypeRest = rest;
rest = function() {
  const firstShow = !$('restCard');
  prototypeRest();
  restSelection();
  document.querySelector('#view > button.btn').remove();
  document.querySelector('.nextCard').outerHTML = pickCardHtml(selectionSummary, slidePill('Saltar descanso'));
  bindPickCard(firstShow);
  bindSkipSlide(document.querySelector('.restSkip .skipSlide'), () => endRest());
  // The same swipe as in the exercise: from here too the routine can be seen, changed or ended.
  $('view').insertAdjacentHTML('beforeend', '<div class="panel left" id="pNext"></div>');
  swipe($('stage'), { right: openNext, tap: () => react($('fig'), 'nod') });
};
/** The information rises with the finger; a short pull lets it fall back. */
function bindPullUp(card) {
  let origin = null, sheet = null, height = 0, top = 0;
  card.addEventListener('pointerdown', e => { origin = e.target.closest('button') ? null : { x: e.clientX, y: e.clientY }; });
  card.addEventListener('pointermove', e => {
    if (!origin) return;
    const dx = e.clientX - origin.x, dy = e.clientY - origin.y;
    if (!sheet) {
      if (dy > -12 || Math.abs(dx) > Math.abs(dy) || !selectionItem()) return;
      openDetail(selectionItem()); haptic('tick');
      sheet = $('detail'); height = sheet.offsetHeight; origin.y = e.clientY; sheet.style.transition = 'none';
    }
    top = clamp(height + e.clientY - origin.y, 0, height); sheet.style.transform = `translateY(${top}px)`;
  });
  const release = () => {
    origin = null; if (!sheet) return;
    sheet.style.transition = ''; sheet.style.transform = '';
    if (top > height * .78) closeDetail();
    sheet = null;
  };
  card.addEventListener('pointerup', release); card.addEventListener('pointercancel', release);
}
/** The information follows the finger down and closes only when released far enough. */
function bindPullDown(sheet) {
  if (sheet._pullDown) return; sheet._pullDown = true;
  let origin = null, top = 0;
  sheet.addEventListener('pointerdown', e => { top = 0; origin = e.target.closest('button') ? null : { x: e.clientX, y: e.clientY }; });
  sheet.addEventListener('pointermove', e => {
    if (!origin) return;
    const dx = e.clientX - origin.x, dy = e.clientY - origin.y;
    if (!top && Math.abs(dx) > dy) return;
    top = Math.max(0, dy); sheet.style.transition = 'none'; sheet.style.transform = `translateY(${top}px)`;
  });
  const release = () => {
    origin = null; sheet.style.transition = ''; sheet.style.transform = '';
    if (top > sheet.offsetHeight * .25) closeDetail();
    top = 0;
  };
  sheet.addEventListener('pointerup', release); sheet.addEventListener('pointercancel', release);
}
const prototypeOpenDetail = openDetail;
openDetail = function(item) {
  prototypeOpenDetail(item);
  const sheet = $('detail');
  swipe(sheet, { right: () => confirmSelection(item) });
  bindPullDown(sheet);
};
function slidePill(label) {
  return `<div class="skipSlide"><div class="skipFill"></div><span>${label}</span><button class="skipThumb" aria-label="${label}">${homeIcon('skip')}</button></div>`;
}
/** Slide to the end to act: the handle goes from white to dark red on the way. [canFinish] may refuse, and it slides back. */
function bindSkipSlide(track, onDone, canFinish = () => true) {
  const thumb = track.querySelector('.skipThumb'), fill = track.querySelector('.skipFill'), label = track.querySelector('span');
  let originX = null, progress = 0, armed = false;
  const mix = (from, to) => from.map((value, i) => Math.round(value + (to[i] - value) * progress)).join(',');
  const paint = () => {
    const room = track.clientWidth - thumb.offsetWidth - 8, x = progress * room, color = `rgb(${mix([245, 245, 245], [114, 45, 40])})`;
    thumb.style.transform = `translateX(${x}px)`; thumb.style.background = color; thumb.style.color = `rgb(${mix([17, 17, 17], [114, 45, 40])})`;
    fill.style.width = `${x + thumb.offsetWidth}px`; label.style.opacity = 1 - progress;
  };
  thumb.addEventListener('pointerdown', e => { originX = e.clientX; armed = false; haptic('press'); thumb.setPointerCapture(e.pointerId); track.classList.add('dragging'); });
  thumb.addEventListener('pointermove', e => {
    if (originX === null) return;
    progress = clamp((e.clientX - originX) / (track.clientWidth - thumb.offsetWidth - 8), 0, 1); paint();
    if (progress >= .9 !== armed) { armed = !armed; haptic('tick'); }
  });
  const release = e => {
    if (originX === null) return;
    originX = null; track.classList.remove('dragging');
    if (e.type === 'pointerup' && progress >= .9 && canFinish()) { haptic('confirm'); onDone(); return; }
    progress = 0; paint();
  };
  thumb.addEventListener('pointerup', release); thumb.addEventListener('pointercancel', release);
  paint();
}
endRest = function() { confirmSelection(); };
renderSelection = function() {
  if (S.stage === 'rest') { rest(); return; }
  if (S.stage === 'plain') { paintPlain(true); return; }
  const sel = S.selection, firstShow = !$('restCard');
  $('view').innerHTML = `${topBar()}
    <div class="intro"><h1>${sel.kind === 'warm' ? 'Vamos entrando en calor.' : 'Lo que sigue.'}</h1></div>
    <div class="stage" id="stage">${fig(150, movementFor(selectionItem()), 'fig')}</div>
    ${pickCardHtml(selectionSummary, `<button class="btn" id="selectContinue" onclick="confirmSelection()">Seguir</button>`)}`;
  const figure = $('fig');
  figure.addEventListener('click', () => react(figure));
  swipe($('stage'), { right: confirmSelection });
  bindPickCard(firstShow, () => { setFig(figure, movementFor(selectionItem())); if (sel.kind === 'warm') S.warm = sel.index; });
};
const prototypeConfirmSelection = confirmSelection;
confirmSelection = function(item) {
  const fromRest = S.stage === 'rest';
  if (item?.id || selectionItem()) haptic('confirm');
  if (S.stage === 'plain') { plainChosen(item); return; }
  prototypeConfirmSelection(item);
  if (fromRest && S.stage === 'ex') S.selection = null;
};

// ---------- Dock: a tap opens, a hold offers more. ----------
const DOCK_HOLD_MS = 420;
let dockTouch = null;
function bounceDock(key = H.dockBounce) {
  H.dockBounce = null;
  const tile = key && document.querySelector(`[data-dock="${key}"] .dock-tile`);
  if (!tile || motionPreference.matches) return;
  tile.animate([0, -5, 0].map(y => ({ transform: `translateY(${y}px)` })), { duration: 220, easing: 'ease-out' });
}
function dockOpen(key) {
  if (document.querySelector(`[data-dock="${key}"]`).classList.contains('active')) { bounceDock(key); return; }
  if (key === 'gear') { bounceDock(key); leaveSessionTo('settings'); return; }
  H.dockBounce = key;
  go({ week: 'week', goal: 'goals' }[key] || 'general');
}
function dockMenuItems(key) {
  const today = HOST.todayIndex, openWeekDay = i => { H.weekDay = i; H.dockBounce = 'week'; go('week'); };
  if (key === 'today') return [['Ver tu rutina', () => { H.day = today; H.group = 'all'; H.routineFrom = null; go('routine'); }, 'routine-title'],
    ...(!S.startedAt && !S.finished && H.chosen.items.length ? [['Cambiar la hora', () => { go('general'); editStartTime(); }]] : [])];
  if (key === 'week') return [['Hoy', () => openWeekDay(today)], ...(today < 6 ? [['Mañana', () => openWeekDay(today + 1)]] : [])];
  if (key === 'gear' && (S.startedAt || S.finished)) return [['Reiniciar rutina', null]];
  return [];
}
function closeDockMenu() { $('dockMenu')?.remove(); H.dockMenu = null; }
function openDockMenu(button) {
  const items = dockMenuItems(button.dataset.dock); if (!items.length) return;
  H.dockHeld = true; haptic('press');
  H.dockMenu = items;
  const dock = $('dock');
  dock.insertAdjacentHTML('beforeend', `<div class="dock-menu" id="dockMenu">${items.map(([label, run, style], i) => `<button ${run ? `data-dock-item="${i}"` : 'data-hold="reset"'}><span class="${style || ''}">${label}</span></button>`).join('')}</div>`);
  const menu = $('dockMenu');
  menu.style.left = `${clamp(button.offsetLeft + button.offsetWidth / 2 - menu.offsetWidth / 2, 6, dock.clientWidth - menu.offsetWidth - 6)}px`;
}
function endDockTouch() { clearTimeout(dockTouch?.timer); dockTouch = null; }
$('phone').addEventListener('pointerdown', e => {
  if (!e.target.closest('#dockMenu')) closeDockMenu();
  const button = e.target.closest('.dock-btn'); if (!button) return;
  endDockTouch(); H.dockHeld = false;
  dockTouch = { x: e.clientX, y: e.clientY, timer: setTimeout(() => openDockMenu(button), DOCK_HOLD_MS) };
});
$('phone').addEventListener('pointermove', e => { if (dockTouch && Math.hypot(e.clientX - dockTouch.x, e.clientY - dockTouch.y) > 10) endDockTouch(); }, { passive: true });
for (const type of ['pointerup', 'pointercancel']) $('phone').addEventListener(type, endDockTouch, { passive: true });

// ---------- One place for the actions of these screens ----------
$('phone').addEventListener('click', e => {
  if (H.picker && !e.target.closest('.group-tools')) closePicker();
  if (Date.now() < daySwipeUntil) return;
  const button = e.target.closest('button'); if (!button || button.disabled) return;
  const d = button.dataset;
  if (d.dockItem !== undefined) { const run = H.dockMenu[+d.dockItem][1]; closeDockMenu(); run(); return; }
  if (d.dock) { if (H.dockHeld) H.dockHeld = false; else dockOpen(d.dock); return; }
  if (d.nav === 'today') { go('general'); return; }
  if (d.nav === 'week') { go('week'); return; }
  if (d.nav === 'goal') { H.dockBounce = 'goal'; go('goals'); return; }
  if (d.bar !== undefined) { haptic('tick'); pickBar(+d.bar); return; }
  if (d.range) { H.range = d.range; goalsView(); return; }
  if (d.week !== undefined) { pickWeekDay(+d.week); return; }
  if (d.nav) { const target = { gear: 'settings' }[d.nav]; if (d.nav === 'gear') answer('lookOnce', button); leaveSessionTo(target); return; }
  if (d.day !== undefined) { selectDay(+d.day); return; }
  if (d.pick) { togglePick(d.pick); return; }
  if (d.detailGroup) { H.group = d.detailGroup; renderGroupSheet(); return; }
  if (d.edit) { editExercise(d.edit); return; }
  if (d.delete) { deleteExercise(d.delete); return; }
  if (d.replace) { replaceItem(d.replace); return; }
  if (d.jump !== undefined) { plainJump(+d.jump); return; }
  if (d.mark !== undefined) { plainMark(+d.mark); return; }
  if (d.step !== undefined) { H.plainStep = H.plainStep === +d.step ? null : +d.step; paintPlain(); return; }
  if (d.bump) { const [index, key, delta] = d.bump.split(':'); plainBump(+index, key, +delta); return; }
  if (d.addSet !== undefined) { plainAddSet(); return; }
  ({
    'open-routine': () => { answer('thumbOnce', button); H.day = HOST.todayIndex; H.group = 'all'; H.routineFrom = null; go('routine'); },
    'plain-routine': enterPlain, 'guided-routine': enterGuided, 'plain-change': plainChange, 'plain-cancel': plainCancel,
    'week-routine': () => { H.day = H.weekDay; H.group = 'all'; H.routineFrom = 'week'; go('routine'); },
    'edit-time': editStartTime,
    'open-body': () => leaveSessionTo('goals'),
    back: () => go(H.routineFrom || 'general'),
    'next-excess': () => { H.excess = (H.excess || 0) + 1; general(); },
    'add-groups': () => openPicker('add'), 'remove-groups': () => openPicker('remove'),
    'cancel-groups': closePicker, 'confirm-groups': confirmPicker,
    'save-routine': applyRoutine, 'save-group': saveGroupSheet,
    'close-sheet': () => closeHomeSheet(H.sheetView === 'groups'),
    'toggle-groups': () => { const menu = $('groupOptions'); menu.hidden = !menu.hidden; },
    'undo-delete': undoDelete, 'add-exercise': openAddExercise, 'create-exercise': createExerciseForm,
  })[d.action]?.();
});
// ---------- Touch: every press is felt, and what cannot be undone asks for a hold. ----------
const HOLD_MS = 900;
let holding = null;
function stopHold() {
  if (!holding) return;
  clearTimeout(holding.done); holding.ticks.forEach(clearTimeout);
  holding.el.classList.remove('holding'); holding = null;
}
$('phone').addEventListener('pointerdown', e => {
  const target = e.target.closest('[data-hold]');
  if (target) {
    stopHold();
    target.classList.add('holding'); haptic('press');
    holding = { el: target, x: e.clientX, y: e.clientY, ticks: [300, 600].map(ms => setTimeout(() => haptic('tick'), ms)),
      done: setTimeout(() => { stopHold(); haptic('warn'); resetSession(); }, HOLD_MS) };
    return;
  }
  const button = e.target.closest('button, .exrow');
  if (button && !button.disabled && !button.matches('.chk, .skipThumb, .startTrack, #selectContinue')) haptic('tick');
}, { passive: true });
$('phone').addEventListener('pointermove', e => { if (holding && Math.hypot(e.clientX - holding.x, e.clientY - holding.y) > 16) stopHold(); }, { passive: true });
for (const type of ['pointerup', 'pointercancel']) $('phone').addEventListener(type, stopHold, { passive: true });

// The person keeps an eye on the finger, on any screen.
$('phone').addEventListener('pointerdown', e => { for (const f of FIGS) if (f.svg.id === 'fig' || f.svg.id === 'mini' || f.mood === 'spy') f.lookAtClient(e.clientX, e.clientY); }, { passive: true });
$('phone').addEventListener('pointermove', e => { if (e.buttons) for (const f of FIGS) if (f.mood === 'spy') f.lookAtClient(e.clientX, e.clientY, 900); }, { passive: true });

// ---------- Boot: finished days show the close, a session in progress resumes, anything else opens Hoy. ----------
if (HOST.completed) S.finished = true;
const inProgress = S.startedAt && LIVE_STAGES.includes(S.stage);
const initialStage = S.finished ? 'end' : HOST.boot || (inProgress ? S.stage : 'general');
absorbAbsence();
go(initialStage); restoring = false; booted = true; sessionResume();
if (!motionPreference.matches) $('view').animate([{ opacity: 0 }, { opacity: 1 }], { duration: 220, easing: 'ease-out' });
setInterval(updateSessionClock, 250);
setInterval(sessionCheckpoint, 1500);
document.addEventListener('visibilitychange', () => { updateSessionClock(); sessionCheckpoint(); });

