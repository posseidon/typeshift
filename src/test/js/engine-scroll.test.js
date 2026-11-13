const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const enginePaths = [
  'src/main/resources/static/js/engine.js',
  'typeshift-backend/src/main/resources/static/js/engine.js'
];

function initializeProseEngine(enginePath) {
  const elements = new Map();
  const documentListeners = {};
  const inputListeners = {};
  const containerListeners = {};
  const cursorScrollOptions = [];

  function element(id) {
    const result = {
      id,
      dataset: id === 'typing-docket' ? { mode: 'prose', target: 'a' } : {},
      style: {},
      children: [],
      addEventListener(name, listener) {
        (id === 'practice-input' ? inputListeners : containerListeners)[name] = listener;
      },
      focus(options) {
        result.focusOptions = options;
      },
      appendChild(child) {
        result.children.push(child);
      },
      querySelector(selector) {
        return result.children.find(child => `.${child.className}` === selector) || null;
      }
    };

    Object.defineProperty(result, 'textContent', {
      get() {
        return result._textContent || '';
      },
      set(value) {
        result._textContent = value;
        if (id === 'prose-display') {
          result.children = [];
        }
      }
    });
    elements.set(id, result);
    return result;
  }

  [
    'typing-docket', 'typing-container', 'live-wpm', 'live-acc', 'live-timer',
    'gauge-bar', 'gauge-label', 'retry-count', 'gross-count', 'practice-input',
    'prose-display', 'exercise-submission', 'submitted-text', 'elapsed-seconds',
    'submit-result'
  ].forEach(element);

  const document = {
    getElementById(id) {
      return elements.get(id) || null;
    },
    createElement() {
      return {
        className: '',
        textContent: '',
        scrollIntoView(options) {
          cursorScrollOptions.push(options);
        }
      };
    },
    addEventListener(name, listener) {
      documentListeners[name] = listener;
    }
  };
  const window = { addEventListener() {} };

  vm.runInNewContext(readFileSync(path.resolve(enginePath), 'utf8'), {
    document,
    window,
    setInterval() {
      return 1;
    }
  });

  return {
    input: elements.get('practice-input'),
    clickContainer: () => containerListeners.click(),
    typeOneCharacter: () => inputListeners.input({ target: { value: 'a' } }),
    cursorScrollOptions,
    documentListeners
  };
}

for (const enginePath of enginePaths) {
  test(`${enginePath} focuses without scrolling the page`, () => {
    const engine = initializeProseEngine(enginePath);

    engine.clickContainer();

    assert.equal(engine.input.focusOptions?.preventScroll, true);
  });

  test(`${enginePath} does not animate cursor scrolling per keystroke`, () => {
    const engine = initializeProseEngine(enginePath);

    engine.typeOneCharacter();

    assert.equal(engine.cursorScrollOptions.at(-1).behavior, 'auto');
  });
}
