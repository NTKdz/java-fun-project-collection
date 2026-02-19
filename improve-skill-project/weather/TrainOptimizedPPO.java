import ai.djl.Device;
import ai.djl.ndarray.NDManager;

/**
 * Focused training for Optimized PPO
 * Goal: Achieve scores of 50+ consistently
 */
public class TrainOptimizedPPO {

    public static void main(String[] args) throws Exception {
        System.setProperty("ai.djl.default_engine", "PyTorch");
        System.setProperty("PYTORCH_PRECXX11", "true");

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         Optimized PPO for Snake - Deep Dive         ║");
        System.out.println("║                                                      ║");
        System.out.println("║  Improvements:                                       ║");
        System.out.println("║  • Deeper networks (512-512-256-128)                ║");
        System.out.println("║  • Better reward shaping (+20 for food)             ║");
        System.out.println("║  • Compact state (52 vs 412 features)               ║");
        System.out.println("║  • Entropy bonus for exploration                    ║");
        System.out.println("║  • Higher learning rates                            ║");
        System.out.println("║  • Vision rays for spatial awareness                ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        NDManager manager = NDManager.newBaseManager(Device.gpu());
        System.out.println("Device: " + manager.getDevice() + "\n");

        // Training configuration
        int episodes = 10000;  // More episodes for convergence
        int maxSteps = Integer.MAX_VALUE;   // Allow longer episodes
        int updateFrequency = 5;  // Update every 5 episodes
        int updateEpochs = 10;    // More training epochs per update

        PerformanceMetrics metrics = trainOptimizedPPO(
                manager, episodes, maxSteps, updateFrequency, updateEpochs
        );

        // Print detailed results
        printDetailedResults(metrics);

        manager.close();
    }

    private static PerformanceMetrics trainOptimizedPPO(
            NDManager m, int episodes, int maxSteps, int updateFreq, int updateEpochs) {

        System.out.println("🚀 Training Optimized PPO...\n");

        PerformanceMetrics metrics = new PerformanceMetrics("Optimized-PPO");
        EnhancedSnakeEnv env = new EnhancedSnakeEnv();
        OptimizedPPOAgent agent = new OptimizedPPOAgent(m, env.getStateSize(), env.getActionSize());
        EpisodeBuffer buffer = new EpisodeBuffer();

        long start = System.currentTimeMillis();
        int bestScore = 0;
        int episodesSinceImprovement = 0;

        for (int ep = 0; ep < episodes; ep++) {
            env.reset();
            float[] state = env.getCompactState();
            int step = 0;
            float totalReward = 0;

            while (!env.isDone() && step < maxSteps) {
                OptimizedPPOAgent.ActionResult actionResult = agent.selectAction(state);
                float value = agent.getValue(state);
                EnhancedSnakeEnv.StepResult result = env.step(actionResult.action);

                buffer.add(state, actionResult.action, result.reward, value, actionResult.logProb);
                state = result.nextState;
                totalReward += result.reward;
                step++;
            }

            metrics.add(totalReward, env.getScore(), step);
            metrics.detectConvergence(10.0f);  // Higher threshold

            // Update policy
            if ((ep + 1) % updateFreq == 0) {
                agent.train(buffer, updateEpochs);
                buffer.clear();
            }

            // Track improvements
            if (env.getScore() > bestScore) {
                bestScore = env.getScore();
                episodesSinceImprovement = 0;
            } else {
                episodesSinceImprovement++;
            }

            // Progress reporting
            if ((ep + 1) % 100 == 0) {
                float avgScore = metrics.avgScore(100);
                float avgReward = metrics.avgReward(100);
                System.out.printf("Episode %5d | Avg Score: %6.2f | Max: %3d | Best: %3d | Avg Reward: %8.2f | Steps: %4.0f%n",
                        ep + 1, avgScore, metrics.maxScore(), bestScore, avgReward, metrics.avgLength(100));
            }

            // Milestone celebrations
//            if (env.getScore() >= 30 && env.getScore() % 10 == 0) {
//                System.out.println("🎉 Milestone! Score: " + env.getScore() + " at episode " + (ep + 1));
//            }

            // Early stopping if plateaued
            if (episodesSinceImprovement > 2000 && ep > 3000) {
                System.out.println("\n⚠️  Training plateaued. Stopping early at episode " + (ep + 1));
                break;
            }
        }

        metrics.setTrainingTime(System.currentTimeMillis() - start);
        agent.close();
        return metrics;
    }

    private static void printDetailedResults(PerformanceMetrics m) {
        System.out.println("\n\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║                 TRAINING COMPLETE                    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        System.out.println("📊 FINAL STATISTICS:");
        System.out.println("─".repeat(60));
        System.out.printf("  Average Score (last 100):     %.2f%n", m.avgScore());
        System.out.printf("  Average Score (last 500):     %.2f%n", m.avgScore(500));
        System.out.printf("  Average Score (last 1000):    %.2f%n", m.avgScore(1000));
        System.out.printf("  Maximum Score Achieved:       %d%n", m.maxScore());
        System.out.printf("  Average Reward (last 100):    %.2f%n", m.avgReward());
        System.out.printf("  Average Episode Length:       %.0f steps%n", m.avgLength());
        System.out.printf("  Total Training Time:          %.1f seconds%n", m.getTrainingTime() / 1000.0);

        if (m.getConvergenceEpisode() >= 0) {
            System.out.printf("  Convergence Episode:          %d%n", m.getConvergenceEpisode());
        } else {
            System.out.println("  Convergence Episode:          Not reached");
        }
        System.out.println("─".repeat(60));

        // Performance analysis
        System.out.println("\n🎯 PERFORMANCE ANALYSIS:");
        float finalScore = m.avgScore();
        if (finalScore >= 50) {
            System.out.println("  ✅ EXCELLENT - Achieving 50+ average score!");
        } else if (finalScore >= 30) {
            System.out.println("  ✅ GOOD - Strong performance (30-50 range)");
        } else if (finalScore >= 20) {
            System.out.println("  ⚠️  DECENT - Room for improvement (20-30 range)");
        } else {
            System.out.println("  ❌ NEEDS WORK - Consider more training or tuning");
        }

        System.out.println("\n💡 RECOMMENDATIONS:");
        if (finalScore < 30) {
            System.out.println("  • Increase training episodes to 20,000");
            System.out.println("  • Try higher learning rates (0.001 for actor)");
            System.out.println("  • Increase entropy coefficient to 0.02");
            System.out.println("  • Add curriculum learning (start with easier scenarios)");
        } else if (finalScore < 50) {
            System.out.println("  • Continue training to 15,000+ episodes");
            System.out.println("  • Fine-tune reward shaping");
            System.out.println("  • Consider adding recurrent layers (LSTM)");
        } else {
            System.out.println("  • Try even larger grids (30x30)");
            System.out.println("  • Experiment with multi-food scenarios");
            System.out.println("  • Test obstacle variants");
        }

        System.out.println();
    }
}