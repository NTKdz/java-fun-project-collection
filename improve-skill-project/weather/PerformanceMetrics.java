import ai.djl.Device;
import ai.djl.ndarray.NDManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Performance Tracking and Metrics
 */
public class PerformanceMetrics {
    private String name;
    private List<Float> rewards = new ArrayList<>();
    private List<Integer> scores = new ArrayList<>();
    private List<Integer> lengths = new ArrayList<>();
    private long trainingTime;
    private int convergenceEpisode = -1;

    public PerformanceMetrics(String name) {
        this.name = name;
    }

    public void add(float reward, int score, int length) {
        rewards.add(reward);
        scores.add(score);
        lengths.add(length);
    }

    public void setTrainingTime(long timeMs) {
        this.trainingTime = timeMs;
    }

    public float avgScore() {
        return avgScore(100);
    }

    public float avgScore(int window) {
        if (scores.isEmpty()) return 0;
        int start = Math.max(0, scores.size() - window);
        return (float) scores.subList(start, scores.size())
                .stream()
                .mapToInt(i -> i)
                .average()
                .orElse(0);
    }

    public float avgReward() {
        return avgReward(100);
    }

    public float avgReward(int window) {
        if (rewards.isEmpty()) return 0;
        int start = Math.max(0, rewards.size() - window);
        return (float) rewards.subList(start, rewards.size())
                .stream()
                .mapToDouble(f -> f)
                .average()
                .orElse(0);
    }

    public int maxScore() {
        return scores.stream().mapToInt(i -> i).max().orElse(0);
    }

    public float avgLength() {
        return avgLength(100);
    }

    public float avgLength(int window) {
        if (lengths.isEmpty()) return 0;
        int start = Math.max(0, lengths.size() - window);
        return (float) lengths.subList(start, lengths.size())
                .stream()
                .mapToInt(i -> i)
                .average()
                .orElse(0);
    }

    public void detectConvergence(float threshold) {
        if (convergenceEpisode != -1) return;

        // Check if last 100 episodes maintain score above threshold
        if (scores.size() >= 100) {
            float avg = avgScore(100);
            if (avg >= threshold) {
                convergenceEpisode = scores.size() - 100;
            }
        }
    }

    public String getName() {
        return name;
    }

    public long getTrainingTime() {
        return trainingTime;
    }

    public int getConvergenceEpisode() {
        return convergenceEpisode;
    }

    public List<Float> getRewards() {
        return rewards;
    }

    public List<Integer> getScores() {
        return scores;
    }

    public List<Integer> getLengths() {
        return lengths;
    }

    // Get statistics summary
    public String getSummary() {
        return String.format(
                "%s: Avg=%.2f, Max=%d, Time=%.1fs, Convergence=%s",
                name,
                avgScore(),
                maxScore(),
                trainingTime / 1000.0,
                convergenceEpisode >= 0 ? "Ep " + convergenceEpisode : "N/A"
        );
    }
}
