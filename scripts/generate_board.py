"""
Regenerates board/board.html from the ticket JSON files in board/tickets/.

Run this after ANY ticket change (status move, edit, new ticket, history
append) so the HTML board stays in sync with the source-of-truth JSON.
All four persona skills (Product Owner, Developer, Tester, User) call this
script after they finish touching tickets.

Usage: python scripts/generate_board.py
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TICKETS_DIR = os.path.join(ROOT, "board", "tickets")
OUT_PATH = os.path.join(ROOT, "board", "board.html")

STATUSES = ["Todo", "In Progress", "Testing", "Done", "Delivered", "Won't Do"]


def ticket_sort_key(t):
    m = re.search(r"(\d+)$", t["id"])
    return int(m.group(1)) if m else 0


def load_tickets():
    tickets = []
    for name in os.listdir(TICKETS_DIR):
        if name.endswith(".json"):
            with open(os.path.join(TICKETS_DIR, name), encoding="utf-8") as f:
                tickets.append(json.load(f))
    tickets.sort(key=ticket_sort_key)
    return tickets


HTML_TEMPLATE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Lodgy — Build Board</title>
<style>
:root {
  --bg: #f4f5f7; --panel: #ffffff; --text: #1b1f24; --muted: #5c6470;
  --border: #dfe2e6; --accent: #3b6fd6; --shadow: 0 1px 2px rgba(0,0,0,.08);
  --todo: #8a94a6; --inprogress: #d68b3b; --testing: #a15fd6;
  --done: #3ba15f; --delivered: #2f855a; --wontdo: #6b7280;
}
@media (prefers-color-scheme: dark) {
  :root {
    --bg: #14161a; --panel: #1e2126; --text: #eceef1; --muted: #9aa3af;
    --border: #2b2f36; --accent: #6d9bef; --shadow: 0 1px 3px rgba(0,0,0,.4);
  }
}
* { box-sizing: border-box; }
body {
  margin: 0; font-family: -apple-system, Segoe UI, Roboto, sans-serif;
  background: var(--bg); color: var(--text);
}
header {
  padding: 20px 24px 8px; display: flex; flex-direction: column; gap: 14px;
}
h1 { margin: 0; font-size: 1.4rem; }
.subtitle { color: var(--muted); font-size: .85rem; margin-top: 2px; }

.stats {
  display: flex; flex-wrap: wrap; gap: 12px;
}
.stat-tile {
  background: var(--panel); border: 1px solid var(--border); border-radius: 10px;
  padding: 10px 16px; min-width: 110px; box-shadow: var(--shadow);
}
.stat-tile .num { font-size: 1.4rem; font-weight: 600; }
.stat-tile .label { font-size: .72rem; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; }

.progress-bar {
  height: 10px; border-radius: 6px; overflow: hidden; display: flex;
  border: 1px solid var(--border); background: var(--panel);
}
.progress-bar > div { height: 100%; }

.controls {
  display: flex; gap: 10px; flex-wrap: wrap; align-items: center;
}
.controls input, .controls select {
  padding: 6px 10px; border-radius: 8px; border: 1px solid var(--border);
  background: var(--panel); color: var(--text); font-size: .85rem;
}

.board {
  display: grid; grid-template-columns: repeat(5, minmax(220px, 1fr));
  gap: 14px; padding: 12px 24px 32px; overflow-x: auto;
}
.column { display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.column-header {
  display: flex; justify-content: space-between; align-items: center;
  font-weight: 600; font-size: .85rem; padding: 6px 4px;
  border-bottom: 3px solid var(--col-color, var(--border));
}
.column-header .count {
  background: var(--col-color, var(--muted)); color: white; border-radius: 999px;
  padding: 1px 8px; font-size: .72rem;
}
.cards { display: flex; flex-direction: column; gap: 8px; min-height: 40px; }

.card {
  background: var(--panel); border: 1px solid var(--border); border-radius: 10px;
  padding: 10px 12px; box-shadow: var(--shadow); cursor: pointer;
}
.card:hover { border-color: var(--accent); }
.card .id { font-size: .7rem; color: var(--muted); }
.card .title { font-size: .88rem; margin: 3px 0 6px; }
.card .meta { display: flex; gap: 6px; flex-wrap: wrap; }
.badge {
  font-size: .68rem; padding: 2px 7px; border-radius: 999px; border: 1px solid var(--border);
  color: var(--muted);
}
.badge.priority-High { color: #c0392b; border-color: #c0392b55; }
.badge.priority-Medium { color: #b8860b; border-color: #b8860b55; }
.badge.priority-Low { color: var(--muted); }

.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,.5); display: none;
  align-items: flex-start; justify-content: center; padding: 5vh 16px; overflow-y: auto;
}
.overlay.open { display: flex; }
.modal {
  background: var(--panel); border-radius: 12px; padding: 22px 24px; max-width: 640px;
  width: 100%; box-shadow: var(--shadow); border: 1px solid var(--border);
}
.modal h2 { margin: 0 0 4px; font-size: 1.1rem; }
.modal .modal-id { color: var(--muted); font-size: .8rem; }
.modal .section { margin-top: 16px; }
.modal .section h3 { font-size: .78rem; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); margin: 0 0 6px; }
.modal ul { margin: 0; padding-left: 20px; font-size: .88rem; }
.modal .close-btn {
  float: right; background: none; border: none; font-size: 1.1rem; cursor: pointer; color: var(--muted);
}
.history-item {
  border-left: 2px solid var(--border); padding: 4px 0 4px 12px; margin-bottom: 8px; font-size: .82rem;
}
.history-item .h-meta { color: var(--muted); font-size: .72rem; }
</style>
</head>
<body>

<header>
  <div>
    <h1>Lodgy — Build Board</h1>
    <div class="subtitle">Generated by scripts/generate_board.py — source of truth is board/tickets/*.json</div>
  </div>

  <div class="stats" id="stats"></div>
  <div class="progress-bar" id="progressBar"></div>

  <div class="controls">
    <input id="search" type="search" placeholder="Search tickets…">
    <select id="epicFilter"></select>
  </div>
</header>

<main class="board" id="board"></main>

<div class="overlay" id="overlay">
  <div class="modal" id="modal"></div>
</div>

<script>
const TICKETS = __TICKETS_JSON__;
const STATUSES = __STATUSES_JSON__;
const COLORS = {
  "Todo": "var(--todo)", "In Progress": "var(--inprogress)",
  "Testing": "var(--testing)", "Done": "var(--done)", "Delivered": "var(--delivered)",
  "Won't Do": "var(--wontdo)"
};

function renderStats() {
  const statsEl = document.getElementById('stats');
  const total = TICKETS.length;
  statsEl.innerHTML = '';
  const totalTile = document.createElement('div');
  totalTile.className = 'stat-tile';
  totalTile.innerHTML = `<div class="num">${total}</div><div class="label">Total tickets</div>`;
  statsEl.appendChild(totalTile);

  STATUSES.forEach(s => {
    const count = TICKETS.filter(t => t.status === s).length;
    const tile = document.createElement('div');
    tile.className = 'stat-tile';
    tile.innerHTML = `<div class="num" style="color:${COLORS[s]}">${count}</div><div class="label">${s}</div>`;
    statsEl.appendChild(tile);
  });

  const bar = document.getElementById('progressBar');
  bar.innerHTML = '';
  STATUSES.forEach(s => {
    const count = TICKETS.filter(t => t.status === s).length;
    if (count === 0) return;
    const seg = document.createElement('div');
    seg.style.width = (count / total * 100) + '%';
    seg.style.background = COLORS[s];
    seg.title = `${s}: ${count}`;
    bar.appendChild(seg);
  });
}

function populateEpicFilter() {
  const sel = document.getElementById('epicFilter');
  const epics = [...new Set(TICKETS.map(t => t.epic))];
  sel.innerHTML = '<option value="">All epics</option>' +
    epics.map(e => `<option value="${e}">${e}</option>`).join('');
}

function renderBoard() {
  const query = document.getElementById('search').value.toLowerCase();
  const epic = document.getElementById('epicFilter').value;
  const boardEl = document.getElementById('board');
  boardEl.innerHTML = '';

  STATUSES.forEach(status => {
    const col = document.createElement('div');
    col.className = 'column';
    const items = TICKETS.filter(t => t.status === status)
      .filter(t => !epic || t.epic === epic)
      .filter(t => !query || (t.title + t.id + t.epic).toLowerCase().includes(query));

    col.innerHTML = `
      <div class="column-header" style="--col-color:${COLORS[status]}">
        <span>${status}</span><span class="count">${items.length}</span>
      </div>
      <div class="cards"></div>`;
    const cardsEl = col.querySelector('.cards');

    items.forEach(t => {
      const card = document.createElement('div');
      card.className = 'card';
      card.innerHTML = `
        <div class="id">${t.id} · ${t.epic}</div>
        <div class="title">${t.title}</div>
        <div class="meta">
          <span class="badge priority-${t.priority}">${t.priority}</span>
          ${t.assigneeRole ? `<span class="badge">${t.assigneeRole}</span>` : ''}
        </div>`;
      card.addEventListener('click', () => openModal(t.id));
      cardsEl.appendChild(card);
    });

    boardEl.appendChild(col);
  });
}

function openModal(id) {
  const t = TICKETS.find(x => x.id === id);
  const modal = document.getElementById('modal');
  modal.innerHTML = `
    <button class="close-btn" id="closeBtn">✕</button>
    <div class="modal-id">${t.id} · ${t.epic} · ${t.status}</div>
    <h2>${t.title}</h2>
    <div class="section"><h3>Description</h3><div>${t.description}</div></div>
    <div class="section"><h3>Acceptance Criteria</h3>
      <ul>${t.acceptanceCriteria.map(a => `<li>${a}</li>`).join('')}</ul>
    </div>
    <div class="section"><h3>History Trail</h3>
      ${t.history.slice().reverse().map(h => `
        <div class="history-item">
          <div>${h.action}${h.from ? ` (${h.from} → ${h.to})` : ''}${h.note ? ' — ' + h.note : ''}</div>
          <div class="h-meta">${h.actor} · ${new Date(h.timestamp).toLocaleString()}</div>
        </div>`).join('')}
    </div>`;
  document.getElementById('closeBtn').addEventListener('click', closeModal);
  document.getElementById('overlay').classList.add('open');
}
function closeModal() { document.getElementById('overlay').classList.remove('open'); }
document.getElementById('overlay').addEventListener('click', (e) => {
  if (e.target.id === 'overlay') closeModal();
});

document.getElementById('search').addEventListener('input', renderBoard);
document.getElementById('epicFilter').addEventListener('change', renderBoard);

renderStats();
populateEpicFilter();
renderBoard();
</script>
</body>
</html>
"""


def main():
    tickets = load_tickets()
    html = HTML_TEMPLATE.replace("__TICKETS_JSON__", json.dumps(tickets))
    html = html.replace("__STATUSES_JSON__", json.dumps(STATUSES))
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"Wrote {OUT_PATH} with {len(tickets)} tickets.")


if __name__ == "__main__":
    main()
