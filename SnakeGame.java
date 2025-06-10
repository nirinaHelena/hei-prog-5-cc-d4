// Lancement : javac SnakeGame.java && java SnakeGame

import java.util.*;
import java.util.concurrent.*;

public class SnakeGame {
    public static void main(String[] args) {
        new Game().start();
    }
}

class Point {
    final int x, y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    Point move(Direction d) {
        return new Point(x + d.dx, y + d.dy);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return p.x == x && p.y == y;
    }

    public int hashCode() {
        return Objects.hash(x, y);
    }
}

enum Direction {
    UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1);

    final int dx, dy;
    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }
}

interface MoveStrategy {
    Direction getNextDirection(Direction current);
}

class KeyboardMoveStrategy implements MoveStrategy {
    private volatile Direction nextDirection = null;
    private final Map<String, Direction> keyMap = Map.of(
            "Z", Direction.UP,
            "S", Direction.DOWN,
            "Q", Direction.LEFT,
            "D", Direction.RIGHT
    );

    public KeyboardMoveStrategy() {
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine().toUpperCase();
                if (keyMap.containsKey(input)) {
                    nextDirection = keyMap.get(input);
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }

    public Direction getNextDirection(Direction current) {
        if (nextDirection != null && !isOpposite(current, nextDirection)) {
            Direction chosen = nextDirection;
            nextDirection = null;
            return chosen;
        }
        return current;
    }

    private boolean isOpposite(Direction d1, Direction d2) {
        return (d1 == Direction.UP && d2 == Direction.DOWN)
                || (d1 == Direction.DOWN && d2 == Direction.UP)
                || (d1 == Direction.LEFT && d2 == Direction.RIGHT)
                || (d1 == Direction.RIGHT && d2 == Direction.LEFT);
    }
}

class Snake {
    private final LinkedList<Point> body = new LinkedList<>();
    private Direction direction;
    private boolean grow = false;

    Snake(List<Point> initial, Direction dir) {
        body.addAll(initial);
        direction = dir;
    }

    Point head() {
        return body.getFirst();
    }

    List<Point> getBody() {
        return Collections.unmodifiableList(body);
    }

    void setDirection(Direction d) {
        direction = d;
    }

    Direction getDirection() {
        return direction;
    }

    void grow() {
        grow = true;
    }

    boolean move() {
        Point next = head().move(direction);
        if (body.contains(next)) return false;
        body.addFirst(next);
        if (!grow) body.removeLast();
        grow = false;
        return true;
    }

    boolean isOutOfBounds(int width, int height) {
        Point h = head();
        return h.x < 0 || h.y < 0 || h.x >= height || h.y >= width;
    }

    boolean isEating(Point food) {
        return head().equals(food);
    }
}

class SnakeBuilder {
    static Snake buildDefaultSnake() {
        return new Snake(
                List.of(new Point(5, 5), new Point(5, 4), new Point(5, 3)),
                Direction.RIGHT
        );
    }
}

class FoodFactory {
    static Point generateFood(int width, int height, List<Point> snakeBody) {
        Random rand = new Random();
        Point food;
        do {
            food = new Point(rand.nextInt(height), rand.nextInt(width));
        } while (snakeBody.contains(food));
        return food;
    }
}

interface GameState {
    GameState next(Game game);
}

class MenuState implements GameState {
    public GameState next(Game game) {
        game.strategy = new KeyboardMoveStrategy();
        System.out.println("Mode manuel (clavier) activé !");
        System.out.println("Appuyez sur Entrée pour commencer...");
        new Scanner(System.in).nextLine();
        return new RunningState();
    }
}

class RunningState implements GameState {
    public GameState next(Game game) {
        Direction newDir = game.strategy.getNextDirection(game.snake.getDirection());
        game.snake.setDirection(newDir);

        if (!game.snake.move() || game.snake.isOutOfBounds(game.WIDTH, game.HEIGHT)) {
            return new GameOverState();
        }

        if (game.snake.isEating(game.food)) {
            game.snake.grow();
            game.food = FoodFactory.generateFood(game.WIDTH, game.HEIGHT, game.snake.getBody());
            game.score++;
        }

        game.render();
        return this;
    }
}

class GameOverState implements GameState {
    public GameState next(Game game) {
        System.out.println("Game Over! Score: " + game.score);
        return null;
    }
}

class Game {
    final int WIDTH = 10;
    final int HEIGHT = 10;
    Snake snake;
    Point food;
    int score = 0;
    GameState state;
    MoveStrategy strategy;

    void start() {
        snake = SnakeBuilder.buildDefaultSnake();
        food = FoodFactory.generateFood(WIDTH, HEIGHT, snake.getBody());
        state = new MenuState();

        state = state.next(this);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (state != null) {
                if (state instanceof RunningState || state instanceof GameOverState) {
                    state = state.next(this);
                }
            } else {
                executor.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    void render() {
        char[][] grid = new char[HEIGHT][WIDTH];
        for (char[] row : grid) Arrays.fill(row, '.');
        for (Point p : snake.getBody()) grid[p.x][p.y] = '*';
        grid[food.x][food.y] = '@';
        System.out.println("\n\n\n\n\n\n\n\n\n\n");
        for (char[] row : grid) {
            for (char c : row) System.out.print(c + " ");
            System.out.println();
        }
        System.out.println("Score: " + score);
    }
}
