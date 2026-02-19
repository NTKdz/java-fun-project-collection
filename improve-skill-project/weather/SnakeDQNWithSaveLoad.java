import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.Device;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.Activation;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import ai.djl.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Snake DQN with Save/Load Functionality
 *
 * Features:
 * - Save trained model to disk
 * - Load pre-trained model
 * - Continue training from checkpoint
 * - Test saved models
 */
public class SnakeDQNWithSaveLoad {

    // ---- Swing Game Visualization Panel ----
    static class SnakeGamePanel extends JPanel {
        private SnakeEnv env;
        private int cellSize;
        private int margin = 20;
        private String currentAction = "";
        private float currentReward = 0;
        private int currentStep = 0;
        private int episode = 0;
        private float avgScore = 0;

        public SnakeGamePanel(SnakeEnv env) {
            this.env = env;
            this.cellSize = Math.max(15, Math.min(30, 600 / env.gridSize));
            setPreferredSize(new Dimension(
                    env.gridSize * cellSize + 2 * margin + 250,
                    env.gridSize * cellSize + 2 * margin
            ));
            setBackground(new Color(20, 20, 20));
        }

        public void updateInfo(String action, float reward, int step, int episode, float avgScore) {
            this.currentAction = action;
            this.currentReward = reward;
            this.currentStep = step;
            this.episode = episode;
            this.avgScore = avgScore;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2d.setColor(new Color(30, 30, 30));
            g2d.fillRect(margin, margin, env.gridSize * cellSize, env.gridSize * cellSize);

            g2d.setColor(new Color(40, 40, 40));
            for (int i = 0; i <= env.gridSize; i++) {
                g2d.drawLine(margin + i * cellSize, margin,
                        margin + i * cellSize, margin + env.gridSize * cellSize);
                g2d.drawLine(margin, margin + i * cellSize,
                        margin + env.gridSize * cellSize, margin + i * cellSize);
            }

            if (env.snake.size() > 1) {
                for (int i = env.snake.size() - 1; i >= 1; i--) {
                    int[] segment = env.snake.get(i);
                    int x = margin + segment[1] * cellSize;
                    int y = margin + segment[0] * cellSize;
                    float ratio = (float) i / env.snake.size();
                    int green = (int) (100 + ratio * 75);
                    g2d.setColor(new Color(50, green, 50));
                    g2d.fillRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 8, 8);
                    g2d.setColor(new Color(30, green - 20, 30));
                    g2d.drawRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 8, 8);
                }
            }

            if (!env.snake.isEmpty()) {
                int[] head = env.snake.peekFirst();
                int x = margin + head[1] * cellSize;
                int y = margin + head[0] * cellSize;
                GradientPaint gradient = new GradientPaint(
                        x + 2, y + 2, new Color(100, 220, 100),
                        x + cellSize - 2, y + cellSize - 2, new Color(60, 180, 60)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 10, 10);
                g2d.setColor(new Color(40, 160, 40));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 10, 10);
                g2d.setStroke(new BasicStroke(1));

                if (cellSize >= 20) {
                    g2d.setColor(Color.WHITE);
                    int eyeSize = Math.max(3, cellSize / 8);
                    int eyeOffset = Math.max(4, cellSize / 5);
                    g2d.fillOval(x + eyeOffset, y + eyeOffset, eyeSize, eyeSize);
                    g2d.fillOval(x + cellSize - eyeOffset - eyeSize, y + eyeOffset, eyeSize, eyeSize);
                    g2d.setColor(Color.BLACK);
                    int pupilSize = Math.max(1, eyeSize / 2);
                    g2d.fillOval(x + eyeOffset + 1, y + eyeOffset + 1, pupilSize, pupilSize);
                    g2d.fillOval(x + cellSize - eyeOffset - eyeSize + 1, y + eyeOffset + 1, pupilSize, pupilSize);
                }
            }

            if (env.food != null) {
                int x = margin + env.food[1] * cellSize;
                int y = margin + env.food[0] * cellSize;
                for (int i = 3; i > 0; i--) {
                    g2d.setColor(new Color(255, 100, 100, 30 * i));
                    g2d.fillOval(x + 3 - i * 2, y + 3 - i * 2,
                            cellSize - 6 + i * 4, cellSize - 6 + i * 4);
                }
                GradientPaint foodGradient = new GradientPaint(
                        x + 4, y + 4, new Color(255, 80, 80),
                        x + cellSize - 4, y + cellSize - 4, new Color(200, 40, 40)
                );
                g2d.setPaint(foodGradient);
                g2d.fillOval(x + 4, y + 4, cellSize - 8, cellSize - 8);
                g2d.setColor(new Color(255, 150, 150, 200));
                g2d.fillOval(x + 6, y + 6, Math.max(4, cellSize / 4), Math.max(4, cellSize / 4));
            }

            int infoX = margin + env.gridSize * cellSize + 30;
            int infoY = margin + 30;

            g2d.setColor(new Color(100, 220, 100));
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("SNAKE AI", infoX, infoY);

            g2d.setFont(new Font("Consolas", Font.BOLD, 14));
            g2d.setColor(Color.WHITE);
            int lineHeight = 30;
            int currentY = infoY + 40;

            g2d.drawString("Episode: " + episode, infoX, currentY);
            currentY += lineHeight;
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString("Score: " + env.getScore(), infoX, currentY);
            currentY += lineHeight;
            g2d.setColor(new Color(150, 150, 255));
            g2d.drawString("Length: " + env.snake.size(), infoX, currentY);
            currentY += lineHeight;
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawString("Step: " + currentStep, infoX, currentY);
            currentY += lineHeight + 10;

            g2d.setFont(new Font("Consolas", Font.PLAIN, 13));
            g2d.setColor(new Color(180, 180, 180));
            g2d.drawString("Action:", infoX, currentY);
            currentY += 20;
            g2d.setFont(new Font("Consolas", Font.BOLD, 14));

            switch (currentAction) {
                case "UP" -> g2d.setColor(new Color(100, 150, 255));
                case "DOWN" -> g2d.setColor(new Color(255, 150, 100));
                case "LEFT" -> g2d.setColor(new Color(255, 200, 100));
                case "RIGHT" -> g2d.setColor(new Color(150, 255, 150));
                default -> g2d.setColor(Color.GRAY);
            }
            g2d.drawString(currentAction, infoX + 10, currentY);
            currentY += 30;

            g2d.setFont(new Font("Consolas", Font.PLAIN, 13));
            g2d.setColor(new Color(180, 180, 180));
            g2d.drawString("Reward:", infoX, currentY);
            currentY += 20;
            g2d.setFont(new Font("Consolas", Font.BOLD, 14));

            if (currentReward >= 10) {
                g2d.setColor(new Color(50, 255, 50));
            } else if (currentReward <= -10) {
                g2d.setColor(new Color(255, 50, 50));
            } else if (currentReward > 0) {
                g2d.setColor(new Color(150, 255, 150));
            } else if (currentReward < 0) {
                g2d.setColor(new Color(255, 180, 100));
            } else {
                g2d.setColor(Color.GRAY);
            }
            g2d.drawString(String.format("%.2f", currentReward), infoX + 10, currentY);
            currentY += 35;

            g2d.setFont(new Font("Consolas", Font.PLAIN, 13));
            g2d.setColor(new Color(180, 180, 180));
            g2d.drawString("Avg Score:", infoX, currentY);
            currentY += 20;
            g2d.setFont(new Font("Consolas", Font.BOLD, 14));
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(String.format("%.1f", avgScore), infoX + 10, currentY);
            currentY += 35;

            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            if (env.isDone()) {
                g2d.setColor(new Color(255, 80, 80));
                g2d.drawString("GAME OVER", infoX, currentY);
            } else {
                g2d.setColor(new Color(80, 255, 80));
                g2d.drawString("PLAYING", infoX, currentY);
            }
        }
    }

    // ---- Enhanced Environment ----
    static class SnakeEnv {
        int gridSize = 20;
        LinkedList<int[]> snake = new LinkedList<>();
        int[] food;
        Random rand = new Random();
        boolean done = false;
        int stepsWithoutFood = 0;
        int maxStepsWithoutFood = 100;
        int score = 0;

        public SnakeEnv() { reset(); }

        public void reset() {
            snake.clear();
            snake.add(new int[]{gridSize / 2, gridSize / 2});
            placeFood();
            done = false;
            stepsWithoutFood = 0;
            score = 0;
        }

        private void placeFood() {
            Set<String> snakePositions = new HashSet<>();
            for (int[] p : snake) {
                snakePositions.add(p[0] + "," + p[1]);
            }
            List<int[]> availablePositions = new ArrayList<>();
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    if (!snakePositions.contains(i + "," + j)) {
                        availablePositions.add(new int[]{i, j});
                    }
                }
            }
            if (!availablePositions.isEmpty()) {
                food = availablePositions.get(rand.nextInt(availablePositions.size()));
            }
        }

        public Pair<float[], Float> step(int action) {
            if (done) return new Pair<>(getEnhancedState(), 0f);
            int[] head = snake.peekFirst().clone();
            int[] oldHead = head.clone();

            switch (action) {
                case 0 -> head[0]--;
                case 1 -> head[0]++;
                case 2 -> head[1]--;
                case 3 -> head[1]++;
            }

            if (head[0] < 0 || head[0] >= gridSize || head[1] < 0 || head[1] >= gridSize) {
                done = true;
                return new Pair<>(getEnhancedState(), -10f);
            }

            for (int[] p : snake) {
                if (p[0] == head[0] && p[1] == head[1]) {
                    done = true;
                    return new Pair<>(getEnhancedState(), -10f);
                }
            }

            snake.addFirst(head);
            stepsWithoutFood++;
            float reward = 0f;

            if (head[0] == food[0] && head[1] == food[1]) {
                reward = 10f;
                score++;
                stepsWithoutFood = 0;
                placeFood();
            } else {
                snake.removeLast();
                double oldDistance = Math.sqrt(Math.pow(oldHead[0] - food[0], 2) +
                        Math.pow(oldHead[1] - food[1], 2));
                double newDistance = Math.sqrt(Math.pow(head[0] - food[0], 2) +
                        Math.pow(head[1] - food[1], 2));
                if (newDistance < oldDistance) {
                    reward = 0.1f;
                } else {
                    reward = -0.15f;
                }
            }

            if (stepsWithoutFood >= maxStepsWithoutFood) {
                done = true;
                reward = -5f;
            }

            return new Pair<>(getEnhancedState(), reward);
        }

        public boolean isDone() { return done; }

        public float[] getEnhancedState() {
            int[] head = snake.peekFirst();
            int stateSize = 12 + (gridSize * gridSize);
            float[] state = new float[stateSize];
            int idx = 0;

            state[idx++] = isDanger(head[0] - 1, head[1]) ? 1f : 0f;
            state[idx++] = isDanger(head[0] + 1, head[1]) ? 1f : 0f;
            state[idx++] = isDanger(head[0], head[1] - 1) ? 1f : 0f;
            state[idx++] = isDanger(head[0], head[1] + 1) ? 1f : 0f;
            state[idx++] = food[0] < head[0] ? 1f : 0f;
            state[idx++] = food[0] > head[0] ? 1f : 0f;
            state[idx++] = food[1] < head[1] ? 1f : 0f;
            state[idx++] = food[1] > head[1] ? 1f : 0f;

            if (snake.size() > 1) {
                int[] neck = snake.get(1);
                state[idx++] = neck[0] < head[0] ? 1f : 0f;
                state[idx++] = neck[0] > head[0] ? 1f : 0f;
                state[idx++] = neck[1] < head[1] ? 1f : 0f;
                state[idx++] = neck[1] > head[1] ? 1f : 0f;
            } else {
                idx += 4;
            }

            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    boolean isSnake = false;
                    for (int[] p : snake) {
                        if (p[0] == i && p[1] == j) {
                            isSnake = true;
                            break;
                        }
                    }
                    if (isSnake) {
                        state[idx++] = 1f;
                    } else if (food[0] == i && food[1] == j) {
                        state[idx++] = 0.5f;
                    } else {
                        state[idx++] = 0f;
                    }
                }
            }

            return state;
        }

        private boolean isDanger(int row, int col) {
            if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) {
                return true;
            }
            for (int[] p : snake) {
                if (p[0] == row && p[1] == col) {
                    return true;
                }
            }
            return false;
        }

        public int getScore() { return score; }
    }

    // ---- Replay Buffer ----
    static class ReplayBuffer {
        static class Transition {
            float[] state;
            int action;
            float reward;
            float[] nextState;
            boolean done;
        }

        ArrayDeque<Transition> buffer = new ArrayDeque<>();
        int maxSize = 50000;
        Random rand = new Random();

        public void add(float[] state, int action, float reward, float[] nextState, boolean done) {
            if (buffer.size() >= maxSize) {
                buffer.removeFirst();
            }
            Transition t = new Transition();
            t.state = state;
            t.action = action;
            t.reward = reward;
            t.nextState = nextState;
            t.done = done;
            buffer.addLast(t);
        }

        public List<Transition> sample(int batch) {
            List<Transition> bufferList = new ArrayList<>(buffer);
            List<Transition> batchList = new ArrayList<>(batch);
            for (int i = 0; i < batch; i++) {
                batchList.add(bufferList.get(rand.nextInt(bufferList.size())));
            }
            return batchList;
        }

        public int size() { return buffer.size(); }
    }

    // ---- DQN Agent with Save/Load ----
    static class DQNAgent {
        NDManager manager;
        Model model;
        Trainer trainer;
        int stateSize, actionSize;
        Random rand = new Random();
        float epsilon = 1.0f;
        float epsilonMin = 0.01f;
        float epsilonDecay = 0.995f;

        public DQNAgent(NDManager m, int stateSize, int actionSize) {
            this.manager = m;
            this.stateSize = stateSize;
            this.actionSize = actionSize;

            SequentialBlock net = new SequentialBlock()
                    .add(Linear.builder().setUnits(512).build())
                    .add(Activation::relu)
                    .add(Linear.builder().setUnits(512).build())
                    .add(Activation::relu)
                    .add(Linear.builder().setUnits(256).build())
                    .add(Activation::relu)
                    .add(Linear.builder().setUnits(128).build())
                    .add(Activation::relu)
                    .add(Linear.builder().setUnits(actionSize).build());

            model = Model.newInstance("snake-dqn");
            model.setBlock(net);

            DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                    .optOptimizer(Adam.builder()
                            .optLearningRateTracker(Tracker.fixed(0.0005f))
                            .build());

            trainer = model.newTrainer(config);
            trainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));
        }

        public int selectAction(float[] state) {
            if (rand.nextFloat() < epsilon) {
                return rand.nextInt(actionSize);
            }
            try (NDManager subManager = manager.newSubManager()) {
                NDArray s = subManager.create(state).reshape(1, -1);
                NDArray q = trainer.forward(new NDList(s)).singletonOrThrow();
                return (int) q.argMax(1).getLong();
            }
        }

        public void train(ReplayBuffer buffer, int batchSize, float gamma) {
            if (buffer.size() < batchSize) return;

            List<ReplayBuffer.Transition> batch = buffer.sample(batchSize);
            float[][] states = new float[batchSize][stateSize];
            float[][] nextStates = new float[batchSize][stateSize];
            long[] actions = new long[batchSize];
            float[] rewards = new float[batchSize];
            boolean[] dones = new boolean[batchSize];

            for (int i = 0; i < batchSize; i++) {
                ReplayBuffer.Transition t = batch.get(i);
                states[i] = t.state;
                nextStates[i] = t.nextState;
                actions[i] = t.action;
                rewards[i] = t.reward;
                dones[i] = t.done;
            }

            try (NDManager subManager = manager.newSubManager()) {
                NDArray statesBatch = subManager.create(states);
                NDArray actionsBatch = subManager.create(actions);
                NDArray rewardsBatch = subManager.create(rewards);
                NDArray donesBatch = subManager.create(dones);
                NDArray nextStatesBatch = subManager.create(nextStates);

                NDArray nextQ = trainer.forward(new NDList(nextStatesBatch)).singletonOrThrow();
                NDArray maxNextQ = nextQ.max(new int[]{1});
                NDArray future = donesBatch.logicalNot().mul(gamma).mul(maxNextQ);
                NDArray targetValues = rewardsBatch.add(future);

                try (ai.djl.training.GradientCollector gc = trainer.newGradientCollector()) {
                    NDArray predictions = trainer.forward(new NDList(statesBatch)).singletonOrThrow();
                    NDArray currentSelected = predictions.gather(
                            actionsBatch.reshape(new ai.djl.ndarray.types.Shape(batchSize, 1)), 1
                    ).squeeze(1);
                    NDArray lossValue = trainer.getLoss().evaluate(
                            new NDList(targetValues), new NDList(currentSelected)
                    );
                    gc.backward(lossValue);
                }
                trainer.step();
            }
        }

        public void decayEpsilon() {
            epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
        }

        /**
         * Save the model and training state
         */
        public void saveModel(String modelPath, int currentEpisode, float avgScore) throws IOException {
            Path path = Paths.get(modelPath);
            Files.createDirectories(path.getParent());

            // Save model parameters
            model.save(path, "snake-dqn");

            // Save training metadata
            Path metadataPath = Paths.get(modelPath + ".metadata");
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(metadataPath))) {
                writer.println("episode=" + currentEpisode);
                writer.println("epsilon=" + epsilon);
                writer.println("avgScore=" + avgScore);
                writer.println("stateSize=" + stateSize);
                writer.println("actionSize=" + actionSize);
            }

            System.out.println("✓ Model saved to: " + modelPath);
            System.out.println("  Episode: " + currentEpisode);
            System.out.println("  Epsilon: " + epsilon);
            System.out.println("  Avg Score: " + avgScore);
        }

        /**
         * Load a pre-trained model
         */
        public void loadModel(String modelPath) throws IOException, MalformedModelException {
            Path path = Paths.get(modelPath);

            if (!Files.exists(path)) {
                throw new IOException("Model file not found: " + modelPath);
            }

            // Load model parameters
            model.load(path);

            // Load training metadata
            Path metadataPath = Paths.get(modelPath + ".metadata");
            if (Files.exists(metadataPath)) {
                Properties props = new Properties();
                try (InputStream input = Files.newInputStream(metadataPath)) {
                    props.load(input);
                    epsilon = Float.parseFloat(props.getProperty("epsilon", "0.01"));

                    System.out.println("✓ Model loaded from: " + modelPath);
                    System.out.println("  Episode: " + props.getProperty("episode"));
                    System.out.println("  Epsilon: " + epsilon);
                    System.out.println("  Avg Score: " + props.getProperty("avgScore"));
                }
            }
        }

        public void close() {
            model.close();
        }
    }

    // ---- Training Statistics ----
    static class TrainingStats {
        List<Float> episodeRewards = new ArrayList<>();
        List<Integer> episodeScores = new ArrayList<>();
        int windowSize = 100;

        public void addEpisode(float reward, int score) {
            episodeRewards.add(reward);
            episodeScores.add(score);
        }

        public float getAverageScore() {
            int start = Math.max(0, episodeScores.size() - windowSize);
            return (float) episodeScores.subList(start, episodeScores.size())
                    .stream().mapToInt(i -> i).average().orElse(0.0);
        }
    }

    // ---- Main ----
    public static void main(String[] args) throws Exception {
        System.setProperty("ai.djl.default_engine", "PyTorch");
        System.setProperty("PYTORCH_PRECXX11", "true");

        // Parse command line arguments
        String mode = args.length > 0 ? args[0] : "train";
        String modelPath = "models/snake_dqn_model";

        System.out.println("===========================================");
        System.out.println("Snake DQN with Save/Load");
        System.out.println("===========================================");
        System.out.println("Mode: " + mode);
        System.out.println("Model path: " + modelPath);
        System.out.println();

        NDManager manager = NDManager.newBaseManager(Device.gpu());
        System.out.println("Device: " + manager.getDevice());
        System.out.println("===========================================\n");

        SnakeEnv env = new SnakeEnv();
        int stateSize = env.getEnhancedState().length;
        int actionSize = 4;

        DQNAgent agent = new DQNAgent(manager, stateSize, actionSize);
        ReplayBuffer buffer = new ReplayBuffer();
        TrainingStats stats = new TrainingStats();

        int episodes = 10000;
        int batchSize = 256;
        float gamma = 0.95f;
        int maxSteps = Integer.MAX_VALUE;
        int warmupEpisodes = 50;
        int trainEvery = 3;
        int saveEvery = 1000; // Save model every 1000 episodes

        int startEpisode = 0;

        switch (mode.toLowerCase()) {
            case "train" -> {
                // Fresh training
                System.out.println("=== TRAINING FROM SCRATCH ===\n");
                runTraining(agent, env, buffer, stats, startEpisode, episodes,
                        batchSize, gamma, maxSteps, warmupEpisodes, trainEvery,
                        saveEvery, modelPath);
            }

            case "continue" -> {
                // Continue training from checkpoint
                System.out.println("=== CONTINUE TRAINING ===\n");
                try {
                    agent.loadModel(modelPath);
                    runTraining(agent, env, buffer, stats, startEpisode, episodes,
                            batchSize, gamma, maxSteps, warmupEpisodes, trainEvery,
                            saveEvery, modelPath);
                } catch (IOException e) {
                    System.err.println("Error loading model: " + e.getMessage());
                    System.err.println("Starting fresh training instead...\n");
                    runTraining(agent, env, buffer, stats, startEpisode, episodes,
                            batchSize, gamma, maxSteps, warmupEpisodes, trainEvery,
                            saveEvery, modelPath);
                }
            }

            case "test" -> {
                // Test saved model
                System.out.println("=== TESTING SAVED MODEL ===\n");
                try {
                    agent.loadModel(modelPath);
                    agent.epsilon = 0f; // No exploration
                    runVisualization(agent, env, maxSteps, stats.getAverageScore());
                } catch (IOException e) {
                    System.err.println("Error loading model: " + e.getMessage());
                    System.err.println("Cannot test without a trained model!");
                }
            }

            default -> {
                System.out.println("Usage: java SnakeDQNWithSaveLoad [mode]");
                System.out.println("Modes:");
                System.out.println("  train    - Train from scratch");
                System.out.println("  continue - Continue training from checkpoint");
                System.out.println("  test     - Test saved model with visualization");
            }
        }

        agent.close();
        manager.close();
    }

    private static void runTraining(DQNAgent agent, SnakeEnv env, ReplayBuffer buffer,
                                    TrainingStats stats, int startEpisode, int episodes,
                                    int batchSize, float gamma, int maxSteps,
                                    int warmupEpisodes, int trainEvery, int saveEvery,
                                    String modelPath) throws IOException {
        long startTime = System.currentTimeMillis();
        int globalStep = 0;

        for (int ep = startEpisode; ep < episodes; ep++) {
            env.reset();
            float[] state = env.getEnhancedState();
            int step = 0;
            float totalReward = 0;

            while (!env.isDone() && step < maxSteps) {
                int action = agent.selectAction(state);
                var pair = env.step(action);
                float[] nextState = pair.getKey();
                float reward = pair.getValue();

                buffer.add(state, action, reward, nextState, env.isDone());

                if (ep >= warmupEpisodes && globalStep % trainEvery == 0) {
                    agent.train(buffer, batchSize, gamma);
                }

                state = nextState;
                totalReward += reward;
                step++;
                globalStep++;
            }

            if (ep >= warmupEpisodes) {
                agent.decayEpsilon();
            }

            stats.addEpisode(totalReward, env.getScore());

            if ((ep + 1) % 10 == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("Episode %5d | Reward: %6.2f | Score: %2d | Steps: %3d | Epsilon: %.3f | Avg Score: %.2f | Time: %.1fs%n",
                        ep + 1, totalReward, env.getScore(), step, agent.epsilon, stats.getAverageScore(), elapsed / 1000.0);
            }

            // Auto-save checkpoint
            if ((ep + 1) % saveEvery == 0) {
                agent.saveModel(modelPath, ep + 1, stats.getAverageScore());
            }
        }

        long trainingTime = System.currentTimeMillis() - startTime;
        System.out.printf("\nTraining completed in %.2f seconds%n", trainingTime / 1000.0);
        System.out.printf("Final Average Score: %.2f%n", stats.getAverageScore());

        // Final save
        agent.saveModel(modelPath, episodes, stats.getAverageScore());
    }

    private static void runVisualization(DQNAgent agent, SnakeEnv env, int maxSteps, float avgScore) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake AI - Testing Saved Model");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            SnakeGamePanel gamePanel = new SnakeGamePanel(env);
            frame.add(gamePanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            new Thread(() -> {
                try {
                    int testEpisodes = 10;
                    for (int testEp = 0; testEp < testEpisodes; testEp++) {
                        env.reset();
                        float[] state = env.getEnhancedState();
                        int step = 0;
                        float totalReward = 0;

                        while (!env.isDone() && step < maxSteps) {
                            int action = agent.selectAction(state);
                            var pair = env.step(action);
                            state = pair.getKey();
                            float reward = pair.getValue();
                            totalReward += reward;

                            String actionName = getActionName(action);
                            gamePanel.updateInfo(actionName, reward, step, testEp + 1, avgScore);
                            gamePanel.repaint();

                            Thread.sleep(80);
                            step++;
                        }

                        System.out.printf("Test Episode %d: Score=%d, Steps=%d, Reward=%.2f%n",
                                testEp + 1, env.getScore(), step, totalReward);

                        Thread.sleep(1500);
                    }

                    System.out.println("\nAll test episodes completed!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private static String getActionName(int action) {
        return switch (action) {
            case 0 -> "UP";
            case 1 -> "DOWN";
            case 2 -> "LEFT";
            case 3 -> "RIGHT";
            default -> "UNKNOWN";
        };
    }
}