/*
============================================================================
TICKET-ADV106 — Sortable, resizable table with sticky headers
============================================================================
*/

(() => {
  "use strict";

  const TRADES_URL = "/api/v1/trades";
  const MIN_COLUMN_WIDTH = 96;

  const tableBody = document.getElementById("trade-table-body");
  const tableStatus = document.getElementById("table-status");
  const sortButtons = Array.from(
    document.querySelectorAll(".trade-table__sort")
  );
  const resizeHandles = Array.from(
    document.querySelectorAll(".resize-handle")
  );

  let trades = [];
  let sortState = {
    key: null,
    direction: "ascending"
  };

  if (!tableBody || !tableStatus) {
    return;
  }

  function normaliseResponse(payload) {
    if (Array.isArray(payload)) {
      return payload;
    }

    if (Array.isArray(payload?.content)) {
      return payload.content;
    }

    if (Array.isArray(payload?.items)) {
      return payload.items;
    }

    return [];
  }

  function compareValues(firstValue, secondValue, key) {
    if (key === "quantity" || key === "price") {
      const firstNumber = Number(firstValue);
      const secondNumber = Number(secondValue);

      if (!Number.isFinite(firstNumber) && !Number.isFinite(secondNumber)) {
        return 0;
      }

      if (!Number.isFinite(firstNumber)) {
        return 1;
      }

      if (!Number.isFinite(secondNumber)) {
        return -1;
      }

      return firstNumber - secondNumber;
    }

    if (key === "tradeDate") {
      const firstDate = Date.parse(firstValue);
      const secondDate = Date.parse(secondValue);

      if (Number.isNaN(firstDate) && Number.isNaN(secondDate)) {
        return 0;
      }

      if (Number.isNaN(firstDate)) {
        return 1;
      }

      if (Number.isNaN(secondDate)) {
        return -1;
      }

      return firstDate - secondDate;
    }

    return String(firstValue ?? "").localeCompare(
      String(secondValue ?? ""),
      undefined,
      {
        numeric: true,
        sensitivity: "base"
      }
    );
  }

  function getSortedTrades() {
    if (!sortState.key) {
      return [...trades];
    }

    const directionMultiplier =
      sortState.direction === "ascending" ? 1 : -1;

    return [...trades].sort((firstTrade, secondTrade) => {
      return (
        compareValues(
          firstTrade?.[sortState.key],
          secondTrade?.[sortState.key],
          sortState.key
        ) * directionMultiplier
      );
    });
  }

  function formatNumber(value) {
    const number = Number(value);

    return Number.isFinite(number)
      ? new Intl.NumberFormat().format(number)
      : "—";
  }

  function formatPrice(value) {
    const number = Number(value);

    return Number.isFinite(number)
      ? new Intl.NumberFormat(undefined, {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2
        }).format(number)
      : "—";
  }

  function formatDate(value) {
    if (!value) {
      return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return String(value);
    }

    return new Intl.DateTimeFormat(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric"
    }).format(date);
  }

  function normaliseStatus(value) {
    const status = String(value ?? "PENDING")
      .trim()
      .toUpperCase();

    return status || "PENDING";
  }

  function createCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value;
    cell.title = value;
    return cell;
  }

  function createStatusCell(value) {
    const status = normaliseStatus(value);
    const cell = document.createElement("td");
    const pill = document.createElement("span");

    pill.className =
      `status-pill status-pill--${status.toLowerCase()}`;
    pill.textContent = status;

    cell.append(pill);
    return cell;
  }

  function createTradeRow(trade) {
    const row = document.createElement("tr");

    row.append(
      createCell(String(trade.tradeRef ?? "—")),
      createCell(String(trade.instrumentSymbol ?? "—")),
      createCell(formatNumber(trade.quantity)),
      createCell(formatPrice(trade.price)),
      createCell(formatDate(trade.tradeDate)),
      createStatusCell(trade.status)
    );

    return row;
  }

  function renderTrades() {
    const sortedTrades = getSortedTrades();

    tableBody.replaceChildren();

    if (sortedTrades.length === 0) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");

      cell.colSpan = 6;
      cell.textContent = "No trades found.";

      row.append(cell);
      tableBody.append(row);
      tableStatus.textContent = "No trades found.";
      return;
    }

    const fragment = document.createDocumentFragment();

    sortedTrades.forEach((trade) => {
      fragment.append(createTradeRow(trade));
    });

    tableBody.append(fragment);

    tableStatus.textContent =
      `${sortedTrades.length} trade${sortedTrades.length === 1 ? "" : "s"}`;
  }

  function updateSortIndicators() {
    sortButtons.forEach((button) => {
      const isActive = button.dataset.sortKey === sortState.key;

      button.setAttribute(
        "aria-sort",
        isActive ? sortState.direction : "none"
      );

      const heading = button.closest("th");

      if (heading) {
        heading.setAttribute(
          "aria-sort",
          isActive ? sortState.direction : "none"
        );
      }
    });
  }

  function sortBy(key) {
    if (sortState.key === key) {
      sortState.direction =
        sortState.direction === "ascending"
          ? "descending"
          : "ascending";
    } else {
      sortState.key = key;
      sortState.direction = "ascending";
    }

    updateSortIndicators();
    renderTrades();
  }

  function enableSorting() {
    sortButtons.forEach((button) => {
      button.addEventListener("click", () => {
        sortBy(button.dataset.sortKey);
      });
    });
  }

  function resizeColumn(heading, width) {
    const safeWidth = Math.max(MIN_COLUMN_WIDTH, width);
    heading.style.width = `${safeWidth}px`;
  }

  function enableColumnResizing() {
    resizeHandles.forEach((handle) => {
      const heading = handle.closest("th");

      if (!heading) {
        return;
      }

      handle.addEventListener("pointerdown", (event) => {
        event.preventDefault();

        const startingX = event.clientX;
        const startingWidth = heading.getBoundingClientRect().width;

        handle.setPointerCapture(event.pointerId);

        function handlePointerMove(moveEvent) {
          const nextWidth =
            startingWidth + moveEvent.clientX - startingX;

          resizeColumn(heading, nextWidth);
        }

        function stopResizing(endEvent) {
          handle.releasePointerCapture(endEvent.pointerId);
          handle.removeEventListener(
            "pointermove",
            handlePointerMove
          );
          handle.removeEventListener("pointerup", stopResizing);
          handle.removeEventListener(
            "pointercancel",
            stopResizing
          );
        }

        handle.addEventListener(
          "pointermove",
          handlePointerMove
        );
        handle.addEventListener("pointerup", stopResizing);
        handle.addEventListener(
          "pointercancel",
          stopResizing
        );
      });

      handle.addEventListener("keydown", (event) => {
        const currentWidth =
          heading.getBoundingClientRect().width;

        if (event.key === "ArrowLeft") {
          event.preventDefault();
          resizeColumn(heading, currentWidth - 10);
        }

        if (event.key === "ArrowRight") {
          event.preventDefault();
          resizeColumn(heading, currentWidth + 10);
        }
      });
    });
  }

  async function loadTrades() {
    tableStatus.textContent = "Loading trades…";

    try {
      const response = await fetch(TRADES_URL, {
        headers: {
          Accept: "application/json"
        }
      });

      if (!response.ok) {
        throw new Error(
          `Trade request failed with status ${response.status}`
        );
      }

      const payload = await response.json();
      trades = normaliseResponse(payload);
      renderTrades();
    } catch (error) {
      console.error("Unable to load trades.", error);

      tableBody.replaceChildren();

      const row = document.createElement("tr");
      const cell = document.createElement("td");

      cell.colSpan = 6;
      cell.textContent =
        "Unable to load trades. Confirm that the backend is running.";

      row.append(cell);
      tableBody.append(row);

      tableStatus.textContent = "Unable to load trades.";
    }
  }

  enableSorting();
  enableColumnResizing();
  loadTrades();
})();
