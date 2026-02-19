import ai.djl.Device;
import ai.djl.ndarray.NDManager;
import java.util.*;

/**
 * Snake AI Model Comparison
 * Comprehensive comparison of multiple RL algorithms
 */
public class SnakeAIComparison {

    public static void main(String[] args) throws Exception {
        System.setProperty("ai.djl.default_engine", "PyTorch");
        System.setProperty("PYTORCH_PRECXX11", "true");

        printHeader();

        NDManager manager = NDManager.newBaseManager(Device.gpu());
        System.out.println("Device: " + manager.getDevice() + "\n");

        int episodes = 10000;
        int maxSteps = Integer.MAX_VALUE;

        // Train all models
        List<PerformanceMetrics> results = new ArrayList<>();

        // Value-based methods
//        results.add(trainDQN(manager, episodes, maxSteps));
//        results.add(trainDDQN(manager, episodes, maxSteps));
//        results.add(trainDuelingDQN(manager, episodes, maxSteps));
//        results.add(trainPrioritizedDQN(manager, episodes, maxSteps));
//        results.add(trainNoisyDQN(manager, episodes, maxSteps));
//        results.add(trainRainbowDQN(manager, episodes, maxSteps));
//        results.add(trainSARSA(manager, episodes, maxSteps));

        // Policy-based methods
        results.add(trainPPO(manager, episodes, maxSteps));
//        results.add(trainA2C(manager, episodes, maxSteps));
//        results.add(trainREINFORCE(manager, episodes, maxSteps));

        // Print comprehensive results
        printResults(results);

        manager.close();
    }

    private static void printHeader() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   Snake AI: Multi-Model RL Performance Comparison   ║");
        System.out.println("║                                                      ║");
        System.out.println("║  Models: DQN, DDQN, Dueling-DQN, PPO, A2C          ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    private static PerformanceMetrics trainDQN(NDManager m, int eps, int maxSteps) {
        System.out.println("Training DQN...");
        PerformanceMetrics metrics = new PerformanceMetrics("DQN");
        SnakeEnv env = new SnakeEnv();
        DQNAgent agent = new DQNAgent(m, env.getStateSize(), env.getActionSize());
        ReplayBuffer buffer = new ReplayBuffer(50000);

        long start = System.currentTimeMillis();
        trainOffPolicy(env, agent, buffer, metrics, eps, maxSteps, 256, 0.95f);
        metrics.setTrainingTime(System.currentTimeMillis() - start);

        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainDDQN(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining DDQN...");
        PerformanceMetrics metrics = new PerformanceMetrics("DDQN");
        SnakeEnv env = new SnakeEnv();
        DDQNAgent agent = new DDQNAgent(m, env.getStateSize(), env.getActionSize());
        ReplayBuffer buffer = new ReplayBuffer(50000);

        long start = System.currentTimeMillis();
        trainOffPolicy(env, agent, buffer, metrics, eps, maxSteps, 256, 0.95f);
        metrics.setTrainingTime(System.currentTimeMillis() - start);

        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainDuelingDQN(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining Dueling-DQN...");
        PerformanceMetrics metrics = new PerformanceMetrics("Dueling-DQN");
        SnakeEnv env = new SnakeEnv();
        DuelingDQNAgent agent = new DuelingDQNAgent(m, env.getStateSize(), env.getActionSize());
        ReplayBuffer buffer = new ReplayBuffer(50000);

        long start = System.currentTimeMillis();
        trainOffPolicy(env, agent, buffer, metrics, eps, maxSteps, 256, 0.95f);
        metrics.setTrainingTime(System.currentTimeMillis() - start);

        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainPPO(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining PPO...");
        PerformanceMetrics metrics = new PerformanceMetrics("PPO");
        SnakeEnv env = new SnakeEnv();
        PPOAgent agent = new PPOAgent(m, env.getStateSize(), env.getActionSize());
        EpisodeBuffer buffer = new EpisodeBuffer();

        long start = System.currentTimeMillis();
        trainOnPolicy(env, agent, buffer, metrics, eps, maxSteps, 4);
        metrics.setTrainingTime(System.currentTimeMillis() - start);

        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainA2C(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining A2C...");
        PerformanceMetrics metrics = new PerformanceMetrics("A2C");
        SnakeEnv env = new SnakeEnv();
        A2CAgent agent = new A2CAgent(m, env.getStateSize(), env.getActionSize());
        EpisodeBuffer buffer = new EpisodeBuffer();

        long start = System.currentTimeMillis();
        trainOnPolicy(env, agent, buffer, metrics, eps, maxSteps, 4);
        metrics.setTrainingTime(System.currentTimeMillis() - start);

        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainPrioritizedDQN(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining Prioritized-DQN...");
        PerformanceMetrics metrics = new PerformanceMetrics("Prioritized-DQN");
        SnakeEnv env = new SnakeEnv();
        PrioritizedDQNAgent agent = new PrioritizedDQNAgent(m, env.getStateSize(), env.getActionSize());
        PrioritizedReplayBuffer buffer = new PrioritizedReplayBuffer(50000);

        long start = System.currentTimeMillis();
        int globalStep = 0;

        for (int ep = 0; ep < eps; ep++) {
            env.reset();
            float[] state = env.getEnhancedState();
            int step = 0;
            float totalReward = 0;

            while (!env.isDone() && step < maxSteps) {
                int action = agent.selectAction(state);
                SnakeEnv.StepResult result = env.step(action);

                buffer.add(state, action, result.reward, result.nextState, result.done);

                if (ep >= 50 && globalStep % 3 == 0) {
                    agent.train(buffer, 256, 0.95f);
                }

                state = result.nextState;
                totalReward += result.reward;
                step++;
                globalStep++;
            }

            if (ep >= 50) agent.decayEpsilon();
            metrics.add(totalReward, env.getScore(), step);
            metrics.detectConvergence(5.0f);

            if ((ep + 1) % 500 == 0) {
                System.out.printf("  Episode %5d | Avg Score: %.2f | Max: %d%n",
                        ep + 1, metrics.avgScore(), metrics.maxScore());
            }
        }

        metrics.setTrainingTime(System.currentTimeMillis() - start);
        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainNoisyDQN(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining Noisy-DQN...");
        PerformanceMetrics metrics = new PerformanceMetrics("Noisy-DQN");
        SnakeEnv env = new SnakeEnv();
        NoisyDQNAgent agent = new NoisyDQNAgent(m, env.getStateSize(), env.getActionSize());
        ReplayBuffer buffer = new ReplayBuffer(50000);

        long start = System.currentTimeMillis();
        trainOffPolicy(env, agent, buffer, metrics, eps, maxSteps, 256, 0.95f);
        metrics.setTrainingTime(System.currentTimeMillis() - start);

        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainSARSA(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining SARSA...");
        PerformanceMetrics metrics = new PerformanceMetrics("SARSA");
        SnakeEnv env = new SnakeEnv();
        SARSAAgent agent = new SARSAAgent(m, env.getStateSize(), env.getActionSize());

        long start = System.currentTimeMillis();

        for (int ep = 0; ep < eps; ep++) {
            env.reset();
            agent.reset();
            float[] state = env.getEnhancedState();
            int step = 0;
            float totalReward = 0;

            int action = agent.selectAction(state);

            while (!env.isDone() && step < maxSteps) {
                SnakeEnv.StepResult result = env.step(action);
                int nextAction = agent.selectActionAndTrain(result.nextState, result.reward, result.done);

                totalReward += result.reward;
                action = nextAction;
                step++;
            }

            if (ep >= 50) agent.decayEpsilon();
            metrics.add(totalReward, env.getScore(), step);
            metrics.detectConvergence(5.0f);

            if ((ep + 1) % 500 == 0) {
                System.out.printf("  Episode %5d | Avg Score: %.2f | Max: %d%n",
                        ep + 1, metrics.avgScore(), metrics.maxScore());
            }
        }

        metrics.setTrainingTime(System.currentTimeMillis() - start);
        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainREINFORCE(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining REINFORCE...");
        PerformanceMetrics metrics = new PerformanceMetrics("REINFORCE");
        SnakeEnv env = new SnakeEnv();
        REINFORCEAgent agent = new REINFORCEAgent(m, env.getStateSize(), env.getActionSize());
        EpisodeBuffer buffer = new EpisodeBuffer();

        long start = System.currentTimeMillis();

        for (int ep = 0; ep < eps; ep++) {
            env.reset();
            float[] state = env.getEnhancedState();
            int step = 0;
            float totalReward = 0;

            while (!env.isDone() && step < maxSteps) {
                REINFORCEAgent.ActionResult actionResult = agent.selectAction(state);
                SnakeEnv.StepResult result = env.step(actionResult.action);

                buffer.add(state, actionResult.action, result.reward, 0f, actionResult.logProb);
                state = result.nextState;
                totalReward += result.reward;
                step++;
            }

            metrics.add(totalReward, env.getScore(), step);
            metrics.detectConvergence(5.0f);

            // Train after each episode (Monte Carlo)
            agent.train(buffer);
            buffer.clear();

            if ((ep + 1) % 500 == 0) {
                System.out.printf("  Episode %5d | Avg Score: %.2f | Max: %d%n",
                        ep + 1, metrics.avgScore(), metrics.maxScore());
            }
        }

        metrics.setTrainingTime(System.currentTimeMillis() - start);
        agent.close();
        return metrics;
    }

    private static PerformanceMetrics trainRainbowDQN(NDManager m, int eps, int maxSteps) {
        System.out.println("\nTraining Rainbow-DQN...");
        PerformanceMetrics metrics = new PerformanceMetrics("Rainbow-DQN");
        SnakeEnv env = new SnakeEnv();
        RainbowDQNAgent agent = new RainbowDQNAgent(m, env.getStateSize(), env.getActionSize());
        PrioritizedReplayBuffer buffer = new PrioritizedReplayBuffer(50000);

        long start = System.currentTimeMillis();
        int globalStep = 0;

        for (int ep = 0; ep < eps; ep++) {
            env.reset();
            float[] state = env.getEnhancedState();
            int step = 0;
            float totalReward = 0;

            while (!env.isDone() && step < maxSteps) {
                int action = agent.selectAction(state);
                SnakeEnv.StepResult result = env.step(action);

                buffer.add(state, action, result.reward, result.nextState, result.done);

                if (ep >= 50 && globalStep % 3 == 0) {
                    agent.train(buffer, 256, 0.95f);
                }

                state = result.nextState;
                totalReward += result.reward;
                step++;
                globalStep++;
            }

            metrics.add(totalReward, env.getScore(), step);
            metrics.detectConvergence(5.0f);

            if ((ep + 1) % 500 == 0) {
                System.out.printf("  Episode %5d | Avg Score: %.2f | Max: %d%n",
                        ep + 1, metrics.avgScore(), metrics.maxScore());
            }
        }

        metrics.setTrainingTime(System.currentTimeMillis() - start);
        agent.close();
        return metrics;
    }

    private static void trainOffPolicy(SnakeEnv env, DQNAgent agent, ReplayBuffer buffer,
                                       PerformanceMetrics metrics, int episodes,
                                       int maxSteps, int batchSize, float gamma) {
        int globalStep = 0;

        for (int ep = 0; ep < episodes; ep++) {
            env.reset();
            float[] state = env.getEnhancedState();
            int step = 0;
            float totalReward = 0;

            while (!env.isDone() && step < maxSteps) {
                int action = agent.selectAction(state);
                SnakeEnv.StepResult result = env.step(action);

                buffer.add(state, action, result.reward, result.nextState, result.done);

                if (ep >= 50 && globalStep % 3 == 0) {
                    agent.train(buffer, batchSize, gamma);
                }

                state = result.nextState;
                totalReward += result.reward;
                step++;
                globalStep++;
            }

            if (ep >= 50) agent.decayEpsilon();
            metrics.add(totalReward, env.getScore(), step);
            metrics.detectConvergence(5.0f);

            if ((ep + 1) % 500 == 0) {
                System.out.printf("  Episode %5d | Avg Score: %.2f | Max: %d%n",
                        ep + 1, metrics.avgScore(), metrics.maxScore());
            }
        }
    }

    private static void trainOnPolicy(SnakeEnv env, PPOAgent agent, EpisodeBuffer buffer,
                                      PerformanceMetrics metrics, int episodes,
                                      int maxSteps, int updateEpochs) {
        for (int ep = 0; ep < episodes; ep++) {
            env.reset();
            float[] state = env.getEnhancedState();
            int step = 0;
            float totalReward = 0;

            while (!env.isDone() && step < maxSteps) {
                PPOAgent.ActionResult actionResult = agent.selectAction(state);
                float value = agent.getValue(state);
                SnakeEnv.StepResult result = env.step(actionResult.action);

                buffer.add(state, actionResult.action, result.reward, value, actionResult.logProb);
                state = result.nextState;
                totalReward += result.reward;
                step++;
            }

            metrics.add(totalReward, env.getScore(), step);
            metrics.detectConvergence(5.0f);

            if ((ep + 1) % 10 == 0) {
                agent.train(buffer, updateEpochs);
                buffer.clear();
            }

            if ((ep + 1) % 500 == 0) {
                System.out.printf("  Episode %5d | Avg Score: %.2f | Max: %d%n",
                        ep + 1, metrics.avgScore(), metrics.maxScore());
            }
        }
    }

    private static void printResults(List<PerformanceMetrics> results) {
        System.out.println("\n\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║                  FINAL COMPARISON                    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        // Sort by average score
        results.sort((a, b) -> Float.compare(b.avgScore(), a.avgScore()));

        // Print comparison table
        System.out.printf("%-15s | %10s | %10s | %10s | %12s | %12s%n",
                "Model", "Avg Score", "Max Score", "Avg Reward", "Time (s)", "Convergence");
        System.out.println("─".repeat(90));

        for (PerformanceMetrics m : results) {
            System.out.printf("%-15s | %10.2f | %10d | %10.2f | %12.1f | %12s%n",
                    m.getName(),
                    m.avgScore(),
                    m.maxScore(),
                    m.avgReward(),
                    m.getTrainingTime() / 1000.0,
                    m.getConvergenceEpisode() >= 0 ? "Ep " + m.getConvergenceEpisode() : "N/A"
            );
        }

        System.out.println("─".repeat(90));

        // Winner and analysis
        PerformanceMetrics winner = results.get(0);
        System.out.println("\n🏆 WINNER: " + winner.getName());
        System.out.printf("   Average Score: %.2f%n", winner.avgScore());
        System.out.printf("   Max Score: %d%n", winner.maxScore());

        // Performance categories
        System.out.println("\n📊 PERFORMANCE CATEGORIES:");

        PerformanceMetrics fastest = results.stream()
                .min(Comparator.comparingLong(PerformanceMetrics::getTrainingTime))
                .orElse(null);
        if (fastest != null) {
            System.out.printf("   ⚡ Fastest Training: %s (%.1fs)%n",
                    fastest.getName(), fastest.getTrainingTime() / 1000.0);
        }

        PerformanceMetrics highestMax = results.stream()
                .max(Comparator.comparingInt(PerformanceMetrics::maxScore))
                .orElse(null);
        if (highestMax != null) {
            System.out.printf("   🎯 Highest Peak: %s (score: %d)%n",
                    highestMax.getName(), highestMax.maxScore());
        }

        PerformanceMetrics mostStable = results.stream()
                .filter(m -> m.getConvergenceEpisode() >= 0)
                .min(Comparator.comparingInt(PerformanceMetrics::getConvergenceEpisode))
                .orElse(null);
        if (mostStable != null) {
            System.out.printf("   📈 Fastest Convergence: %s (episode %d)%n",
                    mostStable.getName(), mostStable.getConvergenceEpisode());
        }

        // Algorithm insights
        System.out.println("\n💡 ALGORITHM INSIGHTS:");
        System.out.println("   VALUE-BASED (Off-Policy):");
        System.out.println("   • DQN: Deep Q-Network baseline with experience replay");
        System.out.println("   • DDQN: Reduces Q-value overestimation with double learning");
        System.out.println("   • Dueling-DQN: Separates state value and action advantages");
        System.out.println("   • Prioritized-DQN: Samples important experiences more frequently");
        System.out.println("   • Noisy-DQN: Learnable noise parameters for exploration");
        System.out.println("   • Rainbow-DQN: Combines DDQN + Dueling + Prioritized + Noisy");
        System.out.println();
        System.out.println("   POLICY-BASED (On-Policy):");
        System.out.println("   • PPO: Stable policy optimization with clipped objective");
        System.out.println("   • A2C: Advantage Actor-Critic, simpler than PPO");
        System.out.println("   • REINFORCE: Monte Carlo policy gradient (high variance)");
        System.out.println();
        System.out.println("   TEMPORAL DIFFERENCE:");
        System.out.println("   • SARSA: On-policy TD learning, uses actual next action");

        System.out.println();
    }
}