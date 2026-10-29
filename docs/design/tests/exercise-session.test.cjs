// Offline regression checks for the prototype's actual motion engine. No browser or dependencies.
// Run: node --test docs/design/tests/exercise-session.test.cjs
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const { test } = require('node:test');

const html = readFileSync(join(__dirname, '../concept/EjercicioSesionVisual.html'), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const core = script.slice(script.indexOf('const BONE'), script.indexOf('// The shared pencil'));

function engine(seed = 123) {
  let now = 0;
  const preference = { matches: false };
  const math = Object.create(Math);
  math.random = () => ((seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0) / 4294967296);
  const context = vm.createContext({ Math: math, performance: { now: () => now }, matchMedia: () => preference });
  const api = vm.runInContext(core + '\n({ Figure, B, MOODS, ANIMS, behaviorPool, poseOf, lerpPose, solve, depthScale });', context);
  return { ...api, context, preference, advance: (f, ms = 1000 / 60) => { now += ms; f.tick(now); } };
}

class Node {
  constructor() {
    this.attrs = {}; this.style = {}; this.classes = new Set();
    this.classList = { contains: c => this.classes.has(c), toggle: (c, on) => on ? this.classes.add(c) : this.classes.delete(c) };
  }
  setAttribute(name, value) {
    assert.doesNotMatch(String(value), /NaN|undefined|Infinity/, `${name}: ${value}`);
    this.attrs[name] = String(value);
  }
  append(...nodes) { nodes.forEach(n => n.parentNode = this); }
  prepend(...nodes) { this.append(...nodes); }
}
function svg(mode = 'prep', roam = true, width = 342, height = 500) {
  const s = new Node();
  s.dataset = { h: '150', anim: mode }; s.clientWidth = width; s.clientHeight = height;
  if (roam) s.classes.add('roam');
  const nodes = new Map();
  s.querySelector = key => { if (!nodes.has(key)) nodes.set(key, new Node()); return nodes.get(key); };
  s.querySelector('.front').append(s.querySelector('.aL'), s.querySelector('.hL'));
  return s;
}

test('the full inline script parses', () => { new vm.Script(script); });

test('mood pools exclude dances during recovery and delay impatience / sleep', () => {
  const e = engine();
  const names = (m, t) => e.behaviorPool(m, t).map(([name]) => name);
  for (const mood of ['rest', 'waiting', 'stretch', 'ready', 'prep', 'coach']) {
    assert.ok(!names(mood, 60000).some(n => ['defaultDance', 'passinho', 'salsa', 'freeDance', 'hips'].includes(n)));
  }
  assert.ok(!names('prep', 0).includes('impatient'));
  assert.ok(names('prep', 16000).includes('impatient'));
  assert.ok(!names('waiting', 0).includes('crossNap'));
  assert.ok(names('waiting', 16000).includes('crossNap'));
});

test('memory suppresses repetition and exertion favors recovery without scripting it', () => {
  const e = engine();
  const fresh = new Map(e.behaviorPool('prep', 20000));
  const tired = new Map(e.behaviorPool('prep', 20000, ['box', 'walk'], 1));
  assert.ok(!tired.has('walk'));
  assert.ok(tired.get('box') < fresh.get('box'));
  assert.ok(tired.get('drink') > fresh.get('drink'));
  const seen = new Set();
  const f = new e.Figure(svg());
  for (let i = 0; i < 100; i++) { const last = f.lastB; f.nextBehavior(); assert.notEqual(f.lastB, last); seen.add(f.lastB); }
  assert.ok(seen.size >= 10, `Only ${seen.size} behaviors sampled`);
});

test('all behaviors finish with finite joints and SVG transforms at 60 fps', () => {
  const e = engine();
  for (const name of Object.keys(e.B)) {
    const f = new e.Figure(svg());
    f.pos = { x: 0, z: .7 }; f.mood = null;
    f.loopSeg = { loop: e.ANIMS.idle.f, dur: Infinity };
    f.queue = e.B[name](f); f.begin(f.queue.shift(), true);
    let count = 0;
    while (f.seg.dur !== Infinity && count++ < 9000) {
      e.advance(f);
      for (const joint of Object.values(f.J)) for (const coordinate of joint) assert.ok(Number.isFinite(coordinate), name);
      assert.ok(f.pos.x >= -f.vb.xMax && f.pos.x <= f.vb.xMax, name + ' horizontal bounds');
      assert.ok(f.pos.z >= -1 && f.pos.z <= 1, name + ' depth bounds');
    }
    assert.ok(count < 9000, `${name} did not finish`);
  }
});

test('floor and chair reactions stand up before celebrating; mood switches recover too', () => {
  const e = engine();
  for (const [pose, prop] of [['sleepBack', 'mat'], ['sitChair', 'chair'], ['sleepyCross', 'mat']]) {
    const f = new e.Figure(svg());
    f.pose = e.poseOf(pose); f.seg.prop = prop;
    f.react('cheer');
    assert.ok(f.seg.seq.some(([name]) => name === 'stand'));
    assert.equal(f.queue[0].seq, e.ANIMS.cheer.f);
    f.pose = e.poseOf(pose); f.seg.prop = prop; f.play('ready');
    assert.ok(f.seg.seq.some(([name]) => name === 'stand'));
  }
});

test('short joint arcs, depth ordering, paused detail and motion preference changes stay stable', () => {
  const e = engine();
  const mid = e.lerpPose(e.poseOf({ aR: [170, 170] }), e.poseOf({ aR: [-170, -170] }), .5);
  assert.equal(mid.aR[0], 180);
  assert.ok(e.depthScale(-1) < e.depthScale(0) && e.depthScale(0) < e.depthScale(1));
  const f = new e.Figure(svg()); e.advance(f);
  f.paused = true; const before = f.seg.t0; e.advance(f, 5000); assert.equal(f.seg.t0 - before, 5000);
  f.paused = false; e.preference.matches = true; e.advance(f);
  e.preference.matches = false;
  for (let i = 0; i < 120; i++) e.advance(f, 50);
});

test('small stages and miniature coaching figures remain stable', () => {
  const e = engine();
  for (const [width, height, roam, mode] of [[240, 180, true, 'greet'], [52, 60, false, 'coach']]) {
    const f = new e.Figure(svg(mode, roam, width, height));
    for (let i = 0; i < 1800; i++) e.advance(f);
  }
});

test('rebinding a sheet gesture replaces its listener and leaves button clicks alone', () => {
  const source = script.slice(script.indexOf('function swipe('), script.indexOf('/** Calls fn after'));
  const bind = vm.runInNewContext(source + '\nswipe', { AbortController });
  const surface = new EventTarget();
  let captures = 0, oldCalls = 0, newCalls = 0;
  surface.closest = () => null;
  surface.setPointerCapture = () => captures++;
  bind(surface, { up: () => oldCalls++ });
  bind(surface, { up: () => newCalls++ });
  function pointer(type, y, button = false) {
    const event = new Event(type);
    Object.assign(event, { clientX: 20, clientY: y, pointerId: 1 });
    if (button) Object.defineProperty(event, 'target', { value: { closest: () => true } });
    surface.dispatchEvent(event);
  }
  pointer('pointerdown', 180); pointer('pointerup', 60);
  assert.equal(oldCalls, 0); assert.equal(newCalls, 1); assert.equal(captures, 1);
  pointer('pointerdown', 180, true); pointer('pointerup', 60, true);
  assert.equal(captures, 1); assert.equal(newCalls, 1);
  pointer('pointerdown', 180); pointer('pointercancel', 180); pointer('pointerup', 60);
  assert.equal(newCalls, 1);
});

test('peeking rises, glances and stops when closed or reduced motion is requested', () => {
  const source = script.slice(script.indexOf('function tickPeek('), script.indexOf('function openOthers('));
  const figure = svg(); let open = true;
  figure.closest = () => open;
  figure._peek = { start: 0, x: 0, y: .5, tx: 0, ty: .5, pointerAt: -Infinity };
  const preference = { matches: false };
  const tick = vm.runInNewContext(source + '\ntickPeek', {
    document: { getElementById: () => figure }, motionPreference: preference,
    clamp: (v, lo, hi) => Math.max(lo, Math.min(hi, v)),
  });
  tick(0); const hidden = figure.querySelector('.peekHead').attrs.transform;
  tick(1200); assert.notEqual(figure.querySelector('.peekHead').attrs.transform, hidden);
  for (let t = 1300; t < 20000; t += 17) tick(t);
  assert.notEqual(figure._peek.x, 0);
  open = false;
  const stopped = figure.querySelector('.peekHead').attrs.transform;
  tick(21000); assert.equal(figure.querySelector('.peekHead').attrs.transform, stopped);
  open = true; preference.matches = true;
  tick(22000); const reduced = figure.querySelector('.peekHead').attrs.transform;
  tick(24000); assert.equal(figure.querySelector('.peekHead').attrs.transform, reduced);
});

function flow() {
  const e = engine();
  const element = { classList: { remove() {} } };
  e.context.document = { getElementById: () => element };
  const source = script.slice(script.indexOf('// ---------- Today'), script.indexOf('// ---------- Clock'));
  return vm.runInContext(source + `
    // Rendering is isolated here so routing and recorded data can be checked without a browser.
    renderSelection = () => {}; ex = () => {}; rest = () => {}; warmRun = () => {};
    general = () => {}; start = () => {}; end = () => {};
    let othersOpened = 0; openOthers = () => othersOpened++;
    ({ S, EXS, WARMUPS, CUSTOM, freshSets, go, warm, endRest, beginExerciseSelection,
       selectionItem, selectionCatalog, confirmSelection, cancelSelection, previewOther,
       createCustomItem, movementFor, thumbnailFor, htmlText,
       othersOpened: () => othersOpened })`, e.context);
}

test('every rest exit opens selection, and only confirmation starts the selected exercise', () => {
  const f = flow();
  for (let slot = 0; slot < f.EXS.length; slot++) {
    f.S.stage = 'rest'; f.S.next = slot;
    const previous = f.S.exIdx;
    f.endRest();
    assert.equal(f.S.stage, 'pickExercise'); assert.equal(f.S.exIdx, previous);
    assert.equal(f.S.selection.slot, slot);
    f.S.selection.index = 1;
    const candidate = f.selectionItem();
    f.confirmSelection();
    assert.equal(f.S.stage, 'ex'); assert.equal(f.S.exIdx, slot);
    assert.equal(f.S.choices[slot].id, candidate.id);
  }
});

test('Others never silently starts the first item; browsing and canceling preserve recorded sets', () => {
  const f = flow();
  f.S.sets[0][0] = { r: 7, kg: 72.5, done: true };
  f.beginExerciseSelection(0);
  f.S.selection.index = f.S.selection.options.length;
  f.confirmSelection();
  assert.equal(f.othersOpened(), 1); assert.equal(f.S.stage, 'pickExercise');
  f.previewOther(2);
  assert.equal(f.selectionItem().id, f.EXS[2].id);
  assert.equal(f.S.choices[0].id, f.EXS[0].id);
  f.cancelSelection();
  assert.equal(f.S.stage, 'ex'); assert.equal(f.S.sets[0][0].kg, 72.5);
  f.beginExerciseSelection(0); f.S.selection.index = 1; f.confirmSelection();
  assert.equal(f.S.sets[0][0].done, false);
  f.beginExerciseSelection(0); f.previewOther(0); f.confirmSelection();
  assert.equal(f.S.sets[0][0].kg, 72.5); assert.equal(f.S.sets[0][0].r, 7); assert.equal(f.S.sets[0][0].done, true);
  assert.equal(f.S.sets[1][0].kg, 14);
});

test('canceling selection returns to the same rest, including overtime', () => {
  const f = flow();
  f.S.stage = 'rest'; f.S.next = 1; f.S.restLeft = 0; f.S.restOver = 23; f.S.restTotal = 300;
  f.endRest(); f.cancelSelection();
  assert.equal(f.S.stage, 'rest'); assert.equal(f.S.restLeft, 0);
  assert.equal(f.S.restOver, 23); assert.equal(f.S.restTotal, 300);
});

test('new warm-ups and exercises can be previewed and started without dedicated animations', () => {
  const f = flow();
  const warm = f.createCustomItem('warm', { name: 'Mi movilidad', notes: 'Suave', min: '5' });
  f.CUSTOM.warm.push(warm); f.go('warm');
  f.previewOther(f.selectionCatalog('warm').length - 1);
  assert.equal(f.S.stage, 'warm'); assert.equal(f.movementFor(warm), 'greet');
  f.confirmSelection(); assert.equal(f.S.stage, 'warmRun'); assert.equal(f.S.warmLeft, 300);
  assert.equal(f.movementFor(warm, true), 'coach');
  const exercise = f.createCustomItem('exercise', { name: '<Mi ejercicio>', notes: '', count: '2', reps: '9', kg: '12.5' }, 'pecho');
  f.CUSTOM.exercise.push(exercise); f.beginExerciseSelection(1);
  f.previewOther(f.selectionCatalog('exercise').length - 1); f.confirmSelection();
  assert.equal(f.S.choices[1], exercise); assert.equal(f.S.sets[1].length, 2);
  assert.equal(f.S.sets[1][0].kg, 12.5); assert.equal(f.thumbnailFor(exercise), '=stand');
  assert.equal(f.movementFor({ anim: 'not-in-library' }), 'greet');
  assert.equal(f.htmlText(exercise.name), '&lt;Mi ejercicio&gt;');
  assert.throws(() => f.createCustomItem('warm', { name: 'A', notes: '', min: '0' }));
  assert.throws(() => f.createCustomItem('exercise', { name: 'A', notes: '', count: '2', reps: '9', kg: 'NaN' }, 'pecho'));
});

test('unknown animation identifiers fall back without crashing the character', () => {
  const e = engine(), f = new e.Figure(svg('not-in-library'));
  assert.equal(f.mode, 'idle'); e.advance(f);
  f.play('=missingPose'); e.advance(f); assert.equal(f.mode, 'idle');
});

function liquidControl(scale = 1) {
  let now = 0, id = 0, completed = 0;
  const frames = new Map(), timers = new Map(), reactions = [];
  function element() {
    const el = new EventTarget(), classes = new Set();
    el.dataset = {}; el.style = { setProperty(name, value) { this[name] = value; } };
    el.classList = { add: (...xs) => xs.forEach(x => classes.add(x)), remove: (...xs) => xs.forEach(x => classes.delete(x)), toggle: (x, on) => on ? classes.add(x) : classes.delete(x), contains: x => classes.has(x) };
    el.setPointerCapture = () => {};
    return el;
  }
  const play = element(), track = element();
  const outline = new Node();
  play.querySelector = () => outline;
  play.offsetLeft = 0; play.offsetWidth = 72;
  track.offsetWidth = 240; track.getBoundingClientRect = () => ({ width: 240 * scale });
  track.querySelector = () => ({ offsetLeft: 208, offsetWidth: 32 });
  const source = script.slice(script.indexOf('function bindLiquidStart('), script.indexOf('function warm()'));
  const bind = vm.runInNewContext(source + '\nbindLiquidStart', {
    AbortController, performance: { now: () => now }, motionPreference: { matches: false },
    requestAnimationFrame: fn => { frames.set(++id, fn); return id; }, cancelAnimationFrame: key => frames.delete(key),
    setTimeout: fn => { timers.set(++id, fn); return id; }, clearTimeout: key => timers.delete(key),
  });
  const dispose = bind(play, track, () => completed++, kind => reactions.push(kind));
  return {
    play, track, outline, reactions, dispose, frames, timers, completed: () => completed,
    step(ms) { now += ms; const pending = [...frames.values()]; frames.clear(); pending.forEach(fn => fn(now)); },
    finish() { const pending = [...timers.values()]; timers.clear(); pending.forEach(fn => fn()); },
    pointer(type, x, pointerId = 1) {
      const e = new Event(type); Object.assign(e, { pointerId, clientX: x, button: 0, isPrimary: true }); play.dispatchEvent(e);
    },
  };
}

test('liquid start rejects a tap, early drag, cancellation, and release outside the target', () => {
  const c = liquidControl();
  const idleShape = c.outline.attrs.d;
  c.pointer('pointerdown', 0); c.step(200); c.pointer('pointermove', 188); c.pointer('pointerup', 188);
  c.finish(); assert.equal(c.completed(), 0); assert.equal(c.outline.attrs.d, idleShape); assert.equal(c.frames.size, 0);
  c.pointer('pointerdown', 0); c.step(800); c.pointer('pointermove', 70); c.pointer('pointerup', 70);
  assert.equal(c.timers.size, 0);
  c.pointer('pointerdown', 0); c.step(800); c.pointer('pointercancel', 0);
  assert.equal(c.frames.size, 0); assert.equal(c.track.classList.contains('armed'), false);
  c.pointer('pointerdown', 0); c.step(800); c.pointer('lostpointercapture', 0);
  assert.equal(c.frames.size, 0); c.dispose();
});

test('the blot grows toward the destination on hold and changes shape with drag inertia', () => {
  const c = liquidControl();
  // The first spline point is the forward tip: holding should extend it to the right.
  const tip = () => Number(c.outline.attrs.d.match(/^M([\d.-]+)/)[1]);
  const restingTip = tip();
  c.pointer('pointerdown', 0); c.step(800);
  assert.ok(tip() > restingTip + 10);
  const heldShape = c.outline.attrs.d;
  c.pointer('pointermove', 90); c.step(16);
  assert.notEqual(c.outline.attrs.d, heldShape);
  assert.ok(c.outline.attrs.d.endsWith('Z'));
  c.pointer('pointercancel', 90); assert.equal(tip(), restingTip); c.dispose();
});

test('liquid start docks at the scaled target and confirms exactly once with valid reactions', () => {
  const c = liquidControl(2), e = engine();
  c.pointer('pointerdown', 100); c.step(800);
  c.pointer('pointermove', 476, 2); assert.equal(c.track.classList.contains('near'), false);
  c.pointer('pointermove', 476); assert.equal(c.track.classList.contains('near'), true);
  c.pointer('pointerup', 476); c.pointer('pointerup', 476); c.pointer('lostpointercapture', 476);
  assert.equal(c.play.style.transform, 'translateX(188px) scale(1)');
  assert.equal(c.track.classList.contains('docked'), true); assert.equal(c.play.disabled, true);
  assert.equal(c.frames.size, 0); assert.equal(c.timers.size, 1);
  for (const kind of c.reactions) assert.ok(e.ANIMS[kind]?.f, kind);
  c.finish(); c.finish(); assert.equal(c.completed(), 1); c.dispose();
});

test('leaving the start screen cancels both filling and pending navigation', () => {
  const filling = liquidControl(); filling.pointer('pointerdown', 0); filling.dispose();
  assert.equal(filling.frames.size, 0);
  const docked = liquidControl(); docked.pointer('pointerdown', 0); docked.step(800);
  docked.pointer('pointermove', 188); docked.pointer('pointerup', 188); docked.dispose(); docked.finish();
  assert.equal(docked.completed(), 0); assert.equal(docked.timers.size, 0);
});
