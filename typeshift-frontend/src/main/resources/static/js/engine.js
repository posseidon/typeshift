// TypeShift typing engine (prose + Stage 3 numpad exercises, ADR-006).
// Adapted from typeshift-frontend/mockup/prose_exercise_stage_4_accents/code.html.
// Prose mode renders the passage per keystroke from a hidden textarea; numeric
// mode captures numpad codes only and evaluates the typed tally. No-ops on
// pages without the exercise canvas.
(function () {
  'use strict';

  var docket = document.getElementById('typing-docket');
  var container = document.getElementById('typing-container');
  if (!docket || !container) {
    return;
  }

  var liveWpm = document.getElementById('live-wpm');
  var liveAcc = document.getElementById('live-acc');
  var liveTimer = document.getElementById('live-timer');
  var gaugeBar = document.getElementById('gauge-bar');
  var gaugeLabel = document.getElementById('gauge-label');
  var retryCount = document.getElementById('retry-count');
  var grossCount = document.getElementById('gross-count');

  var secondsElapsed = 0;
  var timerHandle = null;
  var pad = function (n) { return String(n).padStart(2, '0'); };

  function startTimer(onTick) {
    if (timerHandle !== null) {
      return;
    }
    timerHandle = setInterval(function () {
      secondsElapsed++;
      liveTimer.textContent = pad(Math.floor(secondsElapsed / 60)) + ':' + pad(secondsElapsed % 60);
      onTick();
    }, 1000);
  }

  function span(className, text) {
    var el = document.createElement('span');
    el.className = className;
    el.textContent = text;
    return el;
  }

  function initProse() {
    var input = document.getElementById('practice-input');
    var display = document.getElementById('prose-display');
    var targetText = docket.dataset.target;
    var typedText = '';

    function renderText() {
      var correctCount = 0;
      var errorCount = 0;
      display.textContent = '';
      for (var i = 0; i < typedText.length; i++) {
        if (typedText[i] === targetText[i]) {
          correctCount++;
          display.appendChild(span('ch-ok', typedText[i]));
        } else {
          errorCount++;
          display.appendChild(span('ch-bad', typedText[i]));
        }
      }
      display.appendChild(span('cursor-block', ''));
      if (typedText.length < targetText.length) {
        display.appendChild(span('ch-todo', targetText.slice(typedText.length)));
      }

      var accuracy =
        typedText.length > 0 ? ((correctCount / typedText.length) * 100).toFixed(1) : '100.0';
      var wpm =
        secondsElapsed > 0 ? Math.round((correctCount / 5) / (secondsElapsed / 60)) : 0;
      var progress = Math.min(100, Math.round((typedText.length / targetText.length) * 100));

      liveAcc.textContent = accuracy;
      liveWpm.textContent = String(wpm);
      gaugeBar.style.width = progress + '%';
      gaugeLabel.textContent = progress + '%';
      retryCount.textContent = pad(errorCount);
      grossCount.textContent = String(typedText.length);
    }

    input.addEventListener('input', function (e) {
      typedText = e.target.value.slice(0, targetText.length);
      startTimer(renderText);
      renderText();
    });

    container.addEventListener('click', function () {
      input.focus();
    });

    renderText();
  }

  // ADR-006: KeyboardEvent.code, so numpad digits stay distinct from Digit codes.
  var NUMERIC_CHARS = {
    Numpad0: '0', Numpad1: '1', Numpad2: '2', Numpad3: '3', Numpad4: '4',
    Numpad5: '5', Numpad6: '6', Numpad7: '7', Numpad8: '8', Numpad9: '9',
    NumpadDecimal: ',',
    NumpadAdd: '+',
    NumpadSubtract: '\u2212',
    NumpadMultiply: '\u00d7',
    NumpadDivide: '\u00f7'
  };

  function initNumeric() {
    var padValue = document.getElementById('practice-pad');
    var verdict = document.getElementById('math-verdict');
    var keypad = document.getElementById('keypad');
    var expected = docket.dataset.expected || '';
    var maxLength = expected.length + 2;
    var typed = '';
    var misses = 0;

    function renderPad() {
      padValue.textContent = typed;
      retryCount.textContent = pad(misses);
      grossCount.textContent = String(typed.length);
    }

    function evaluate() {
      var accepted = typed.trim() === expected;
      verdict.hidden = false;
      verdict.className = accepted ? 'verdict-ok' : 'verdict-bad';
      verdict.textContent = accepted
        ? 'Ledger reconciled \u2014 answer accepted'
        : 'Reconciliation failed \u2014 recorded answer does not match the ledger (expected: '
          + expected + ')';
    }

    function press(code) {
      if (code === 'NumpadEnter' || code === 'Enter') {
        evaluate();
        return;
      }
      if (/^Digit[0-9]$/.test(code)) {
        misses++;
        startTimer(renderPad);
        renderPad();
        return;
      }
      if (NUMERIC_CHARS[code] !== undefined && typed.length < maxLength) {
        typed += NUMERIC_CHARS[code];
        verdict.hidden = true;
        startTimer(renderPad);
        renderPad();
      }
    }

    document.addEventListener('keydown', function (e) {
      if (e.code === 'Enter' || e.code === 'NumpadEnter'
          || /^Digit[0-9]$/.test(e.code) || NUMERIC_CHARS[e.code] !== undefined) {
        e.preventDefault();
        press(e.code);
      }
    });

    // Keypad buttons only announce their code; capture stays in one place.
    document.addEventListener('typepad', function (e) {
      press(e.detail.code);
    });

    keypad.addEventListener('click', function (e) {
      var button = e.target.closest('button[data-code]');
      if (button) {
        document.dispatchEvent(new CustomEvent('typepad', { detail: { code: button.dataset.code } }));
      }
    });

    renderPad();
  }

  if (docket.dataset.mode === 'numeric') {
    initNumeric();
  } else {
    initProse();
  }

  window.addEventListener('keydown', function (e) {
    if (e.key === 'Escape'
        && window.confirm('Civil Service Bureau: Abandon this transcription shift docket?')) {
      window.location.href = '/stage-map';
    }
  });
})();
