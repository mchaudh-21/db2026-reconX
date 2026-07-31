/*
============================================================================
TICKET-ADV101 — Animated live-trade feed
TICKET-ADV104 — SSE subscription to /api/v1/trades/stream
TICKET-ADV105 — Safe DOM rendering and 50-card feed limit
============================================================================
*/

(() => {
  "use strict";

  const STREAM_URL = "/api/v1/trades/stream";
  const MAX_CARDS = 50;

  let sse = null;
  let connectionStatus = "connecting";

  const feed = document.getElementById("trade-feed");
  const statusBadge = document.getElementById("sse-status");

  if (!feed || !statusBadge) {
    return;
  }

  class TradeFeed {
    constructor(container, limit = MAX_CARDS) {
      this.container = container;
      this.limit = limit;
    }

    addTrade(trade) {
      const emptyState = document.getElementById("trade-feed-empty");

      if (emptyState) {
        emptyState.remove();
      }

      const card = this.createTradeCard(trade);
      this.container.prepend(card);
      this.trim();
    }

    createTradeCard(trade) {
      const status = this.normaliseStatus(trade.status);
      const card = document.createElement("article");

      card.className =
        `trade-card trade-card--${status.toLowerCase()} slide-in`;

      const identity = document.createElement("div");
      identity.className = "trade-card__identity";

      const reference = document.createElement("strong");
      reference.className = "trade-card__reference";
      reference.textContent = trade.tradeRef ?? "Unknown trade";

      const symbol = document.createElement("span");
      symbol.className = "trade-card__symbol";
      symbol.textContent = trade.instrumentSymbol ?? "Unknown instrument";

      identity.append(reference, symbol);

      const details = document.createElement("div");
      details.className = "trade-card__details";

      const quantity = document.createElement("span");
      quantity.textContent =
        `Quantity: ${this.formatNumber(trade.quantity)}`;

      const price = document.createElement("span");
      price.textContent = `Price: ${this.formatPrice(trade.price)}`;

      const statusElement = document.createElement("span");
      statusElement.className = "trade-card__status";
      statusElement.textContent = status;

      details.append(quantity, price, statusElement);
      card.append(identity, details);

      return card;
    }

    trim() {
      while (this.container.children.length > this.limit) {
        this.container.lastElementChild.remove();
      }
    }

    normaliseStatus(status) {
      const value = String(status ?? "PENDING")
        .trim()
        .toUpperCase();

      return value || "PENDING";
    }

    formatNumber(value) {
      const number = Number(value);

      return Number.isFinite(number)
        ? new Intl.NumberFormat().format(number)
        : "—";
    }

    formatPrice(value) {
      const number = Number(value);

      return Number.isFinite(number)
        ? new Intl.NumberFormat(undefined, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
          }).format(number)
        : "—";
    }
  }

  const tradeFeed = new TradeFeed(feed);

  function updateConnectionStatus(nextStatus) {
    connectionStatus = nextStatus;

    const labels = {
      connecting: "Connecting…",
      live: "Live",
      reconnecting: "Reconnecting…",
      error: "Connection error"
    };

    statusBadge.textContent = labels[connectionStatus] ?? "Connecting…";
    statusBadge.className =
      `status-badge status-badge--${connectionStatus}`;
  }

  function parseTrade(rawData) {
    try {
      return JSON.parse(rawData);
    } catch (error) {
      console.error("Unable to parse trade stream event.", error);
      return null;
    }
  }

  function connect() {
    updateConnectionStatus("connecting");

    sse = new EventSource(STREAM_URL);

    sse.onopen = () => {
      updateConnectionStatus("live");
    };

    sse.onmessage = (event) => {
      const trade = parseTrade(event.data);

      if (trade) {
        tradeFeed.addTrade(trade);
      }
    };

    sse.onerror = () => {
      updateConnectionStatus("reconnecting");
    };
  }

  window.addEventListener("beforeunload", () => {
    sse?.close();
  });

  connect();
})();
