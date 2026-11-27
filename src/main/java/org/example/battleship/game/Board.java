package org.example.battleship.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** 10x10 보드. 함선 배치와 발사 결과를 관리한다. */
public class Board {

    public static final int SIZE = 10;
    private static final int[] FLEET_SIZES = {5, 4, 3, 3, 2};
    private static final String[] FLEET_NAMES = {"항공모함", "전함", "순양함", "잠수함", "구축함"};

    public record Placement(int x, int y, int size, boolean horizontal) {
    }

    public record ShotResult(int x, int y, boolean hit, String sunk, List<int[]> sunkCells) {
    }

    private static final class Ship {
        final String name;
        final List<int[]> cells = new ArrayList<>();
        int hits;

        Ship(String name) {
            this.name = name;
        }

        boolean sunk() {
            return hits == cells.size();
        }
    }

    private final Ship[][] grid = new Ship[SIZE][SIZE];
    private final boolean[][] shot = new boolean[SIZE][SIZE];
    private final List<Ship> ships = new ArrayList<>();
    private final List<Placement> placements = new ArrayList<>();

    /** 함대 구성(5,4,3,3,2)과 겹침/범위를 검증하며 배치한다. */
    public static Board of(List<Placement> requested) {
        if (requested == null || requested.size() != FLEET_SIZES.length) {
            throw new IllegalArgumentException("함선은 정확히 " + FLEET_SIZES.length + "척이어야 합니다.");
        }
        List<Placement> sorted = new ArrayList<>(requested);
        sorted.sort(Comparator.comparingInt(Placement::size).reversed());
        int[] sizes = sorted.stream().mapToInt(Placement::size).toArray();
        if (!Arrays.equals(sizes, FLEET_SIZES)) {
            throw new IllegalArgumentException("함선 크기 구성이 올바르지 않습니다.");
        }

        Board board = new Board();
        for (int i = 0; i < sorted.size(); i++) {
            board.place(sorted.get(i), FLEET_NAMES[i]);
        }
        return board;
    }

    public static Board random(Random random) {
        Board board = new Board();
        for (int i = 0; i < FLEET_SIZES.length; i++) {
            while (true) {
                Placement p = new Placement(random.nextInt(SIZE), random.nextInt(SIZE),
                        FLEET_SIZES[i], random.nextBoolean());
                try {
                    board.place(p, FLEET_NAMES[i]);
                    break;
                } catch (IllegalArgumentException ignored) {
                    // 다시 시도
                }
            }
        }
        return board;
    }

    private void place(Placement p, String name) {
        int dx = p.horizontal() ? 1 : 0;
        int dy = p.horizontal() ? 0 : 1;
        int endX = p.x() + dx * (p.size() - 1);
        int endY = p.y() + dy * (p.size() - 1);
        if (!inRange(p.x(), p.y()) || !inRange(endX, endY)) {
            throw new IllegalArgumentException("함선이 보드를 벗어납니다.");
        }
        Ship ship = new Ship(name);
        for (int i = 0; i < p.size(); i++) {
            int x = p.x() + dx * i;
            int y = p.y() + dy * i;
            if (grid[y][x] != null) {
                throw new IllegalArgumentException("함선이 서로 겹칩니다.");
            }
            ship.cells.add(new int[]{x, y});
        }
        for (int[] c : ship.cells) {
            grid[c[1]][c[0]] = ship;
        }
        ships.add(ship);
        placements.add(p);
    }

    public ShotResult fire(int x, int y) {
        if (!inRange(x, y)) {
            throw new IllegalArgumentException("좌표가 범위를 벗어납니다.");
        }
        if (shot[y][x]) {
            throw new IllegalArgumentException("이미 쏜 칸입니다.");
        }
        shot[y][x] = true;
        Ship ship = grid[y][x];
        if (ship == null) {
            return new ShotResult(x, y, false, null, List.of());
        }
        ship.hits++;
        if (ship.sunk()) {
            return new ShotResult(x, y, true, ship.name, ship.cells);
        }
        return new ShotResult(x, y, true, null, List.of());
    }

    public boolean allSunk() {
        return ships.stream().allMatch(Ship::sunk);
    }

    public boolean isShot(int x, int y) {
        return shot[y][x];
    }

    public List<Placement> placements() {
        return List.copyOf(placements);
    }

    public static boolean inRange(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }
}
