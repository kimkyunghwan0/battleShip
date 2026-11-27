package org.example.battleship.web;

import java.util.List;
import java.util.Map;
import java.util.Random;

import jakarta.servlet.http.HttpSession;
import org.example.battleship.game.Board;
import org.example.battleship.game.Game;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class GameController {

    private static final String GAME_KEY = "game";

    public record FireRequest(int x, int y) {
    }

    @GetMapping("/fleet/random")
    public List<Board.Placement> randomFleet() {
        return Board.random(new Random()).placements();
    }

    @PostMapping("/game")
    public Map<String, Boolean> start(@RequestBody List<Board.Placement> fleet, HttpSession session) {
        try {
            session.setAttribute(GAME_KEY, new Game(fleet));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return Map.of("started", true);
    }

    @PostMapping("/game/fire")
    public Game.Turn fire(@RequestBody FireRequest req, HttpSession session) {
        Game game = (Game) session.getAttribute(GAME_KEY);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "진행 중인 게임이 없습니다.");
        }
        try {
            return game.fire(req.x(), req.y());
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
