package org.example.battleship.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class BotAiTest {

    @Test
    void botSinksRandomFleetWithoutRepeatingShots() {
        for (int seed = 0; seed < 50; seed++) {
            Random random = new Random(seed);
            Board board = Board.random(random);
            BotAi bot = new BotAi(random);
            int shots = 0;
            while (!board.allSunk()) {
                int[] t = bot.nextShot();
                bot.report(board.fire(t[0], t[1])); // 중복 발사면 예외
                shots++;
                assertTrue(shots <= 100);
            }
        }
    }

    @Test
    void rejectsOverlappingOrWrongFleet() {
        List<Board.Placement> overlap = List.of(
                new Board.Placement(0, 0, 5, true), new Board.Placement(0, 0, 4, true),
                new Board.Placement(0, 5, 3, true), new Board.Placement(0, 6, 3, true),
                new Board.Placement(0, 7, 2, true));
        assertThrows(IllegalArgumentException.class, () -> Board.of(overlap));

        List<Board.Placement> outOfRange = List.of(
                new Board.Placement(8, 0, 5, true), new Board.Placement(0, 1, 4, true),
                new Board.Placement(0, 2, 3, true), new Board.Placement(0, 3, 3, true),
                new Board.Placement(0, 4, 2, true));
        assertThrows(IllegalArgumentException.class, () -> Board.of(outOfRange));
    }

    @Test
    void reportsSinkOnLastHit() {
        List<Board.Placement> fleet = List.of(
                new Board.Placement(0, 0, 5, true), new Board.Placement(0, 1, 4, true),
                new Board.Placement(0, 2, 3, true), new Board.Placement(0, 3, 3, true),
                new Board.Placement(0, 4, 2, true));
        Board board = Board.of(fleet);
        board.fire(0, 4);
        Board.ShotResult last = board.fire(1, 4);
        assertEquals("구축함", last.sunk());
        assertEquals(2, last.sunkCells().size());
    }
}
