(function () {
  'use strict';

  var translations = {
    'Список задач': 'Task Planner',
    'Добро пожаловать, ': 'Welcome, ',
    'Выход': 'Log out',
    'Вход': 'Login',
    'Команда 1': 'Team 1',
    'Команда 2': 'Team 2',
    'Понедельник': 'Monday',
    'Вторник': 'Tuesday',
    'Среда': 'Wednesday',
    'Четверг': 'Thursday',
    'Пятница': 'Friday',
    'Новая задача': 'New task',
    'Добавить задачу': 'Add task',
    'Удалить': 'Delete',
    'Начать новую неделю': 'Start new week',
    'Очистить списки задач': 'Clear task lists',
    'Архивировать списки задач': 'Archive task lists',
    'Начать': 'Start',
    'Добавить нового пользователя': 'Add new user',
    'Имя пользователя': 'Username',
    'Пароль': 'Password',
    'Добавить пользователя': 'Add user',
    'Список пользователей': 'User list',
    'Показать': 'Show',
    'Остановить сервер': 'Stop server',
    'Остановить': 'Stop',
    'Просмотр': 'Preview',
    'Пользователи': 'Users',
    'Назад': 'Back',
    'Страница не найдена!': 'Page not found!',
    'Поиск задач…': 'Search tasks…',
    'Поиск задач': 'Search tasks',
    'Войти': 'Sign in'
  };

  var reverse = {};
  Object.keys(translations).forEach(function (ru) { reverse[translations[ru]] = ru; });

  function getRu(text) {
    var value = String(text || '').trim();
    if (translations[value]) return value;
    if (reverse[value]) return reverse[value];
    return null;
  }

  var originalText = new WeakMap();

  function translateTextNode(node, english) {
    if (!node || !node.parentElement) return;
    if (node.parentElement.closest('[data-ui-ignore], script, style, [data-action="language"], [data-action="theme"]')) return;

    var current = originalText.has(node) ? originalText.get(node) : (node.nodeValue || '');
    if (!originalText.has(node)) originalText.set(node, current);

    var leading = (current.match(/^\s*/) || [''])[0];
    var trailing = (current.match(/\s*$/) || [''])[0];
    var core = current.slice(leading.length, current.length - trailing.length || undefined);
    var ru = getRu(core);

    if (ru) {
      node.nodeValue = leading + (english ? translations[ru] : ru) + trailing;
      return;
    }

    var welcome = core.match(/^(Добро пожаловать, |Welcome, )(.+?)(!)?$/);
    if (welcome) {
      var username = welcome[2];
      node.nodeValue = leading + (english ? 'Welcome, ' : 'Добро пожаловать, ') + username + (welcome[3] || '') + trailing;
      return;
    }

    var weekday = core.match(/^(Понедельник|Вторник|Среда|Четверг|Пятница)(\s+.*)?$/);
    if (weekday) {
      var translatedDay = translations[weekday[1]] || weekday[1];
      node.nodeValue = leading + translatedDay + (weekday[2] || '') + trailing;
    }
  }

  function translatePage(english) {
    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    var node;
    while ((node = walker.nextNode())) translateTextNode(node, english);

    document.querySelectorAll('input[placeholder]').forEach(function (input) {
      var current = input.getAttribute('placeholder') || '';
      var ru = input.getAttribute('data-placeholder-ru') || getRu(current);
      if (ru) {
        input.setAttribute('data-placeholder-ru', ru);
        input.setAttribute('placeholder', english ? translations[ru] : ru);
      }
    });

    document.querySelectorAll('input[aria-label]').forEach(function (input) {
      var current = input.getAttribute('aria-label') || '';
      var ru = input.getAttribute('data-aria-ru') || getRu(current);
      if (ru) {
        input.setAttribute('data-aria-ru', ru);
        input.setAttribute('aria-label', english ? translations[ru] : ru);
      }
    });

    document.documentElement.setAttribute('lang', english ? 'en' : 'ru');

    document.querySelectorAll('[data-action="language"]').forEach(function (button) {
      button.textContent = english ? '🇷🇺 Русский' : '🇬🇧 English';
      button.title = english ? 'Switch to Russian' : 'Switch to English';
    });

    document.querySelectorAll('[data-action="theme"]').forEach(function (button) {
      var dark = document.body.classList.contains('theme-dark');
      button.textContent = dark
        ? (english ? '☀️ Light' : '☀️ Светлая')
        : (english ? '🌙 Dark' : '🌙 Тёмная');
    });
  }

  function applyTheme(dark) {
    document.body.classList.toggle('theme-dark', !!dark);
    try { localStorage.setItem('todo-theme', dark ? 'dark' : 'light'); } catch (_) {}
    var english = false;
    try { english = localStorage.getItem('todo-language') === 'en'; } catch (_) {}
    document.querySelectorAll('[data-action="theme"]').forEach(function (button) {
      button.textContent = dark
        ? (english ? '☀️ Light' : '☀️ Светлая')
        : (english ? '🌙 Dark' : '🌙 Тёмная');
    });
  }

  function toggleLanguage(event) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    var english = false;
    try { english = localStorage.getItem('todo-language') === 'en'; } catch (_) {}
    var next = !english;
    try { localStorage.setItem('todo-language', next ? 'en' : 'ru'); } catch (_) {}
    translatePage(next);
  }

  function toggleTheme(event) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    var dark = document.body.classList.contains('theme-dark');
    applyTheme(!dark);
  }

  function addRipple(button, event) {
    if (!button || !button.classList.contains('btn-animated') && !button.matches('[data-action]')) return;
    var rect = button.getBoundingClientRect();
    var ripple = document.createElement('span');
    ripple.className = 'ripple';
    ripple.style.left = ((event.clientX || rect.left + rect.width / 2) - rect.left) + 'px';
    ripple.style.top = ((event.clientY || rect.top + rect.height / 2) - rect.top) + 'px';
    button.appendChild(ripple);
    window.setTimeout(function () { if (ripple.parentNode) ripple.parentNode.removeChild(ripple); }, 650);
  }

  function setupSearch() {
    var search = document.getElementById('task-search');
    if (!search || search.__todoReady) return;
    search.__todoReady = true;
    search.addEventListener('input', function () {
      var q = (search.value || '').toLowerCase().trim();
      document.querySelectorAll('.weekday li').forEach(function (li) {
        li.classList.toggle('task-hidden', !!q && li.textContent.toLowerCase().indexOf(q) === -1);
      });
    });
  }

  function init() {
    if (window.__todoUIReady) return;
    window.__todoUIReady = true;

    window.todoToggleLanguage = toggleLanguage;
    window.todoToggleTheme = toggleTheme;

    document.querySelectorAll('[data-action="language"]').forEach(function (button) {
      button.addEventListener('click', toggleLanguage, false);
    });
    document.querySelectorAll('[data-action="theme"]').forEach(function (button) {
      button.addEventListener('click', toggleTheme, false);
    });

    document.addEventListener('click', function (event) {
      var button = event.target && event.target.closest ? event.target.closest('button, a') : null;
      if (button) addRipple(button, event);
    }, false);

    var dark = false;
    var english = false;
    try {
      dark = localStorage.getItem('todo-theme') === 'dark';
      english = localStorage.getItem('todo-language') === 'en';
    } catch (_) {}

    applyTheme(dark);
    translatePage(english);
    setupSearch();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();
