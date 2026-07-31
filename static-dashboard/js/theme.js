/*
============================================================================
TICKET-ADV100 — Persistent light/dark theme
============================================================================
*/

(() => {
  "use strict";

  const STORAGE_KEY = "reconx-theme";
  const root = document.documentElement;
  const toggle = document.getElementById("theme-toggle");
  const icon = document.getElementById("theme-toggle-icon");

  function getPreferredTheme() {
    const savedTheme = localStorage.getItem(STORAGE_KEY);

    if (savedTheme === "light" || savedTheme === "dark") {
      return savedTheme;
    }

    return window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  }

  function applyTheme(theme) {
    const isDark = theme === "dark";

    root.dataset.theme = theme;

    if (toggle) {
      toggle.setAttribute("aria-pressed", String(isDark));
      toggle.setAttribute(
        "aria-label",
        `Switch to ${isDark ? "light" : "dark"} theme`
      );
    }

    if (icon) {
      icon.textContent = isDark ? "☀" : "☾";
    }
  }

  applyTheme(getPreferredTheme());

  if (!toggle) {
    return;
  }

  toggle.addEventListener("click", () => {
    const nextTheme =
      root.dataset.theme === "dark" ? "light" : "dark";

    localStorage.setItem(STORAGE_KEY, nextTheme);
    applyTheme(nextTheme);
  });
})();
