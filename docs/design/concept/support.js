// Offline runtime for the concept screens (*.dc.html) exported from the design canvas.
// It understands just what those files use: {{holes}}, <sc-for>, <sc-if>, <helmet>,
// event handlers bound with onClick/onChange="{{fn}}" and a `class Component extends DCLogic`.
// Open any .dc.html in a browser to see and click through the screen; links go between screens.
(function () {
  const HOLE = /\{\{\s*([^}]+?)\s*\}\}/g;
  const EVENTS = { onclick: 'click', onchange: 'input', oninput: 'input' };
  let template = null;
  let mount = null;
  let component = null;
  let queued = false;

  class DCLogic {
    constructor(props) {
      this.props = props || {};
      this.state = {};
    }
    setState(patch) {
      const next = typeof patch === 'function' ? patch(this.state) : patch;
      this.state = Object.assign({}, this.state, next);
      schedule();
    }
    forceUpdate() { schedule(); }
  }
  window.DCLogic = DCLogic;

  function schedule() {
    if (queued) return;
    queued = true;
    requestAnimationFrame(() => { queued = false; paint(); });
  }

  function lookup(path, scope) {
    const p = (path || '').trim();
    if (p === 'true') return true;
    if (p === 'false') return false;
    if (/^-?\d+(\.\d+)?$/.test(p)) return Number(p);
    return p.split('.').reduce((value, key) => (value == null ? undefined : value[key]), scope);
  }

  function wholeHole(text) {
    const match = /^\s*\{\{\s*([^}]+?)\s*\}\}\s*$/.exec(text || '');
    return match ? match[1] : null;
  }

  function interpolate(text, scope) {
    return text.replace(HOLE, (_, path) => {
      const value = lookup(path, scope);
      return value == null ? '' : String(value);
    });
  }

  function renderChildren(source, out, scope) {
    source.childNodes.forEach((child) => renderNode(child, out, scope));
  }

  function renderNode(node, out, scope) {
    if (node.nodeType === Node.TEXT_NODE) {
      out.appendChild(document.createTextNode(interpolate(node.nodeValue, scope)));
      return;
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return;
    const tag = node.localName;
    if (tag === 'sc-for') {
      const list = lookup(wholeHole(node.getAttribute('list')), scope) || [];
      const as = node.getAttribute('as') || 'item';
      list.forEach((item, index) => renderChildren(node, out, Object.assign({}, scope, { [as]: item, $index: index })));
      return;
    }
    if (tag === 'sc-if') {
      if (lookup(wholeHole(node.getAttribute('value')), scope)) renderChildren(node, out, scope);
      return;
    }
    const element = node.namespaceURI === 'http://www.w3.org/1999/xhtml'
      ? document.createElement(tag)
      : document.createElementNS(node.namespaceURI, tag);
    for (const attr of node.attributes) {
      const name = attr.name.toLowerCase();
      if (name.startsWith('hint-')) continue;
      if (EVENTS[name]) {
        const handler = lookup(wholeHole(attr.value), scope);
        if (typeof handler === 'function') element.addEventListener(EVENTS[name], handler);
        continue;
      }
      element.setAttribute(attr.name, interpolate(attr.value, scope));
    }
    if (tag === 'input' || tag === 'textarea') element.value = element.getAttribute('value') || '';
    renderChildren(node, element, scope);
    out.appendChild(element);
  }

  function paint() {
    // Re-rendering replaces the inputs, so keep the caret where the user was typing.
    const fields = mount ? [...mount.querySelectorAll('input, textarea')] : [];
    const focused = fields.indexOf(document.activeElement);
    const caret = focused >= 0 ? document.activeElement.selectionStart : null;

    const fragment = document.createDocumentFragment();
    renderChildren(template, fragment, component.renderVals());
    mount.replaceChildren(fragment);

    if (focused >= 0) {
      const field = mount.querySelectorAll('input, textarea')[focused];
      if (field) {
        field.focus();
        if (caret != null) field.setSelectionRange(caret, caret);
      }
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    template = document.querySelector('x-dc');
    const script = document.querySelector('script[data-dc-script]');
    if (!template || !script) return;
    const helmet = template.querySelector('helmet');
    if (helmet) {
      [...helmet.children].forEach((child) => document.head.appendChild(child));
      helmet.remove();
    }
    // Detach the template so its onClick="{{...}}" attributes never run as inline handlers.
    mount = document.createElement('div');
    template.replaceWith(mount);
    const Component = new Function('DCLogic', `${script.textContent}\nreturn Component;`)(DCLogic);
    component = new Component({});
    paint();
    if (typeof component.componentDidMount === 'function') component.componentDidMount();
  });
})();
