package org.example.battleship.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 사냥(체커보드) / 추적(명중 주변) 방식의 봇. */
public class BotAi {

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final Random random;
    private final boolean[][] tried = new boolean[Board.SIZE][Board.SIZE];
    /** 명중했지만 아직 침몰시키지 못한 칸. */
    private final List<int[]> openHits = new ArrayList<>();

    public BotAi(Random random) {
        this.random = random;
    }

    public int[] nextShot() {
        List<int[]> best = new ArrayList<>();
        int bestScore = 0;
        for (int[] hit : openHits) {
            for (int[] d : DIRS) {
                int x = hit[0] + d[0];
                int y = hit[1] + d[1];
                if (!Board.inRange(x, y) || tried[y][x]) {
                    continue;
                }
                // 반대편도 명중이면 일직선 연장이므로 우선순위를 높인다.
                int score = isOpenHit(hit[0] - d[0], hit[1] - d[1]) ? 10 : 1;
                if (score > bestScore) {
                    bestScore = score;
                    best.clear();
                }
                if (score == bestScore) {
                    best.add(new int[]{x, y});
                }
            }
        }
        if (!best.isEmpty()) {
            return best.get(random.nextInt(best.size()));
        }
        return randomHunt();
    }

    public void report(Board.ShotResult result) {
        tried[result.y()][result.x()] = true;
        if (!result.hit()) {
            return;
        }
        openHits.add(new int[]{result.x(), result.y()});
        if (result.sunk() != null) {
            for (int[] c : result.sunkCells()) {
                openHits.removeIf(h -> h[0] == c[0] && h[1] == c[1]);
            }
        }
    }

    private int[] randomHunt() {
        List<int[]> parity = new ArrayList<>();
        List<int[]> any = new ArrayList<>();
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (tried[y][x]) {
                    continue;
                }
                any.add(new int[]{x, y});
                if ((x + y) % 2 == 0) {
                    parity.add(new int[]{x, y});
                }
            }
        }
        List<int[]> pool = parity.isEmpty() ? any : parity;
        return pool.get(random.nextInt(pool.size()));
    }

    private boolean isOpenHit(int x, int y) {
        return openHits.stream().anyMatch(h -> h[0] == x && h[1] == y);
    }
}
