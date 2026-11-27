package org.example.battleship.game;

import java.util.List;
import java.util.Random;

/** 플레이어 vs 봇 한 판의 상태. */
public class Game {

    public enum Winner { PLAYER, BOT }

    public record Turn(Board.ShotResult player, Board.ShotResult bot, Winner winner,
                       List<Board.Placement> botFleet) {
    }

    private final Board playerBoard;
    private final Board botBoard;
    private final BotAi bot;
    private Winner winner;

    public Game(List<Board.Placement> playerFleet) {
        Random random = new Random();
        this.playerBoard = Board.of(playerFleet);
        this.botBoard = Board.random(random);
        this.bot = new BotAi(random);
    }

    public synchronized Turn fire(int x, int y) {
        if (winner != null) {
            throw new IllegalStateException("이미 끝난 게임입니다.");
        }
        Board.ShotResult mine = botBoard.fire(x, y);
        if (botBoard.allSunk()) {
            winner = Winner.PLAYER;
            return new Turn(mine, null, winner, botBoard.placements());
        }

        int[] target = bot.nextShot();
        Board.ShotResult theirs = playerBoard.fire(target[0], target[1]);
        bot.report(theirs);
        if (playerBoard.allSunk()) {
            winner = Winner.BOT;
            return new Turn(mine, theirs, winner, botBoard.placements());
        }
        return new Turn(mine, theirs, null, null);
    }
}
