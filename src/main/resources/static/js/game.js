(() => {
    const SIZE = 10;
    const FLEET = [
        { size: 5, name: '항공모함' },
        { size: 4, name: '전함' },
        { size: 3, name: '순양함' },
        { size: 3, name: '잠수함' },
        { size: 2, name: '구축함' },
    ];

    const $ = (id) => document.getElementById(id);
    const myBoardEl = $('myBoard');
    const enemyBoardEl = $('enemyBoard');
    const statusEl = $('status');
    const dockEl = $('dock');

    let placed;      // index -> {x, y, size, horizontal}
    let selected;    // 선택된 함선 index
    let horizontal;
    let playing;
    let busy;

    const myCells = buildBoard(myBoardEl);
    const enemyCells = buildBoard(enemyBoardEl);

    function buildBoard(el) {
        const cells = [];
        for (let y = 0; y < SIZE; y++) {
            for (let x = 0; x < SIZE; x++) {
                const c = document.createElement('div');
                c.className = 'cell';
                c.dataset.x = x;
                c.dataset.y = y;
                el.appendChild(c);
                cells.push(c);
            }
        }
        return cells;
    }

    const at = (cells, x, y) => cells[y * SIZE + x];

    function shipCells(p) {
        const out = [];
        for (let i = 0; i < p.size; i++) {
            out.push([p.x + (p.horizontal ? i : 0), p.y + (p.horizontal ? 0 : i)]);
        }
        return out;
    }

    function canPlace(p, ignoreIndex) {
        const occupied = new Set();
        placed.forEach((q, i) => {
            if (q && i !== ignoreIndex) shipCells(q).forEach(([x, y]) => occupied.add(x + ',' + y));
        });
        return shipCells(p).every(([x, y]) =>
            x >= 0 && x < SIZE && y >= 0 && y < SIZE && !occupied.has(x + ',' + y));
    }

    function renderSetup() {
        myCells.forEach((c) => (c.className = 'cell'));
        placed.forEach((p) => p && shipCells(p).forEach(([x, y]) => at(myCells, x, y).classList.add('ship')));

        dockEl.innerHTML = '';
        FLEET.forEach((f, i) => {
            const li = document.createElement('li');
            li.textContent = `${f.name} (${f.size})`;
            if (placed[i]) li.classList.add('placed');
            if (i === selected) li.classList.add('selected');
            li.onclick = () => {
                if (playing) return;
                placed[i] = null;
                selected = i;
                renderSetup();
            };
            dockEl.appendChild(li);
        });
        $('start').disabled = placed.some((p) => !p);
    }

    function showPreview(x, y) {
        renderSetup();
        if (selected == null) return;
        const p = { x, y, size: FLEET[selected].size, horizontal };
        const ok = canPlace(p, selected);
        shipCells(p).forEach(([cx, cy]) => {
            if (cx < SIZE && cy < SIZE) {
                at(myCells, cx, cy).classList.add('preview', ...(ok ? [] : ['bad']));
            }
        });
    }

    myBoardEl.addEventListener('mousemove', (e) => {
        if (playing || !e.target.classList.contains('cell')) return;
        showPreview(+e.target.dataset.x, +e.target.dataset.y);
    });
    myBoardEl.addEventListener('mouseleave', () => !playing && renderSetup());
    myBoardEl.addEventListener('click', (e) => {
        if (playing || selected == null || !e.target.classList.contains('cell')) return;
        const p = { x: +e.target.dataset.x, y: +e.target.dataset.y, size: FLEET[selected].size, horizontal };
        if (!canPlace(p, selected)) return;
        placed[selected] = p;
        selected = placed.findIndex((q) => !q);
        if (selected < 0) selected = null;
        renderSetup();
    });

    function rotate() { horizontal = !horizontal; }
    $('rotate').onclick = rotate;
    document.addEventListener('keydown', (e) => {
        if (e.key.toLowerCase() === 'r' && !playing) rotate();
    });

    $('reset').onclick = () => initSetup();

    $('random').onclick = async () => {
        const fleet = await (await fetch('/api/fleet/random')).json();
        // 서버 응답은 크기 내림차순이므로 FLEET 순서와 그대로 대응한다.
        placed = fleet.map((p) => ({ x: p.x, y: p.y, size: p.size, horizontal: p.horizontal }));
        selected = null;
        renderSetup();
    };

    $('start').onclick = async () => {
        const res = await fetch('/api/game', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(placed),
        });
        if (!res.ok) {
            statusEl.textContent = (await res.json()).message || '게임을 시작하지 못했습니다.';
            return;
        }
        playing = true;
        $('setup').hidden = true;
        $('enemyWrap').hidden = false;
        enemyBoardEl.classList.remove('locked');
        statusEl.textContent = '적 해역의 칸을 눌러 발사하세요.';
    };

    enemyBoardEl.addEventListener('click', async (e) => {
        if (!playing || busy || !e.target.classList.contains('cell')) return;
        const cell = e.target;
        if (cell.classList.contains('hit') || cell.classList.contains('miss')) return;
        busy = true;
        try {
            const res = await fetch('/api/game/fire', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ x: +cell.dataset.x, y: +cell.dataset.y }),
            });
            if (!res.ok) {
                statusEl.textContent = (await res.json()).message || '발사에 실패했습니다.';
                return;
            }
            applyTurn(await res.json());
        } finally {
            busy = false;
        }
    });

    function mark(cells, shot) {
        const c = at(cells, shot.x, shot.y);
        c.classList.add(shot.hit ? 'hit' : 'miss');
        (shot.sunkCells || []).forEach(([x, y]) => at(cells, x, y).classList.add('sunk'));
    }

    function describe(who, shot) {
        if (shot.sunk) return `${who}: ${shot.sunk} 격침!`;
        return `${who}: ${shot.hit ? '명중' : '빗나감'}`;
    }

    function applyTurn(turn) {
        mark(enemyCells, turn.player);
        let msg = describe('나', turn.player);
        if (turn.bot) {
            mark(myCells, turn.bot);
            msg += ' / ' + describe('적', turn.bot);
        }
        statusEl.textContent = msg;

        if (turn.winner) {
            playing = false;
            enemyBoardEl.classList.add('locked');
            (turn.botFleet || []).forEach((p) =>
                shipCells(p).forEach(([x, y]) => {
                    const c = at(enemyCells, x, y);
                    if (!c.classList.contains('hit')) c.classList.add('ship');
                }));
            $('resultText').textContent = turn.winner === 'PLAYER' ? '🎉 승리했습니다!' : '💥 패배했습니다.';
            $('result').hidden = false;
        }
    }

    $('again').onclick = () => initSetup();

    function initSetup() {
        placed = FLEET.map(() => null);
        selected = 0;
        horizontal = true;
        playing = false;
        busy = false;
        $('setup').hidden = false;
        $('enemyWrap').hidden = true;
        $('result').hidden = true;
        enemyCells.forEach((c) => (c.className = 'cell'));
        statusEl.textContent = '함선을 선택해 내 보드에 배치하세요.';
        renderSetup();
    }

    initSetup();
})();
