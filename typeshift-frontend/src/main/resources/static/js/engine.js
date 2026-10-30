// TypeShift typing engine (prose exercise, story 5.6 demo wiring).
// Adapted from typeshift-frontend/mockup/prose_exercise_stage_4_accents/code.html.
// Binds a hidden input and renders the passage per keystroke; no-ops on pages
// without the exercise canvas.
(function () {
  'use strict';

  var container = document.getElementById('typing-container');
  if (!container) {
    return;
  }

  var input = document.getElementById('practice-input');
  var display = document.getElementById('prose-display');
  var liveWpm = document.getElementById('live-wpm');
  var liveAcc = document.getElementById('live-acc');
  var liveTimer = document.getElementById('live-timer');
  var gaugeBar = document.getElementById('gauge-bar');
  var gaugeLabel = document.getElementById('gauge-label');
  var retryCount = document.getElementById('retry-count');
  var grossCount = document.getElementById('gross-count');

  var docket = document.getElementById('typing-docket');
  var targetText = docket && docket.dataset.target;

  var typedText = '';
  var secondsElapsed = 0;
  var timerHandle = null;
  var pad = function (n) { return String(n).padStart(2, '0'); };

  function span(className, text) {
    var el = document.createElement('span');
    el.className = className;
    el.textContent = text;
    return el;
  }

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

  function startTimer() {
    if (timerHandle !== null) {
      return;
    }
    timerHandle = setInterval(function () {
      secondsElapsed++;
      liveTimer.textContent = pad(Math.floor(secondsElapsed / 60)) + ':' + pad(secondsElapsed % 60);
      renderText();
    }, 1000);
  }

  input.addEventListener('input', function (e) {
    typedText = e.target.value.slice(0, targetText.length);
    startTimer();
    renderText();
  });

  container.addEventListener('click', function () {
    input.focus();
  });

  window.addEventListener('keydown', function (e) {
    if (e.key === 'Escape'
        && window.confirm('Civil Service Bureau: Abandon this transcription shift docket?')) {
      window.location.href = '/stage-map';
    }
  });

  renderText();
})();
