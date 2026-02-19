import java.util.*;

public class StringGA {

    static final String TARGET = "the algorithm starts with random strings then repeats a loop where it scores each string keeps the best one copies parents from the population mutates the copies and replaces the old population mutation adds randomness selection gives direction and repeating this loop slowly improves the result";
    static final int POP_SIZE = 200;
    static final int MAX_GENERATIONS = 100000;
    static final double MUTATION_RATE = 1.0 / TARGET.length();

    static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ ".toLowerCase().toCharArray();
    static final Random rand = new Random();

    public static void main(String[] args) {
        List<Individual> population = initPopulation();

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {

            // 1. Evaluate
            population.forEach(Individual::evaluate);
            population.sort(Comparator.comparingInt(i -> -i.fitness));

            Individual best = population.get(0);
            System.out.printf(
                    "Gen %4d | Fitness %2d | %s%n",
                    generation, best.fitness, new String(best.genome)
            );

            // Stop condition
            if (best.fitness == TARGET.length()) {
                break;
            }

            // 2. Replacement
            List<Individual> nextGen = new ArrayList<>();

            // Elitism: keep best unchanged
            nextGen.add(best.copy());

            // Fill remaining slots
            while (nextGen.size() < POP_SIZE) {
                Individual parent = tournamentSelect(population);
                Individual child = parent.copy();
                child.mutate();
                nextGen.add(child);
            }

            population = nextGen;
        }
    }

    // -------- GA Parts --------

    static List<Individual> initPopulation() {
        List<Individual> pop = new ArrayList<>();
        for (int i = 0; i < POP_SIZE; i++) {
            pop.add(Individual.random());
        }
        return pop;
    }

    static Individual tournamentSelect(List<Individual> pop) {
        Individual a = pop.get(rand.nextInt(pop.size()));
        Individual b = pop.get(rand.nextInt(pop.size()));
        return a.fitness >= b.fitness ? a : b;
    }

    // -------- Individual --------

    static class Individual {
        char[] genome;
        int fitness;

        static Individual random() {
            Individual i = new Individual();
            i.genome = new char[TARGET.length()];
            for (int j = 0; j < i.genome.length; j++) {
                i.genome[j] = ALPHABET[rand.nextInt(ALPHABET.length)];
            }
            return i;
        }

        void evaluate() {
            fitness = 0;
            for (int i = 0; i < genome.length; i++) {
                if (genome[i] == TARGET.charAt(i)) {
                    fitness++;
                }
            }
        }

        void mutate() {
            for (int i = 0; i < genome.length; i++) {
                if (rand.nextDouble() < MUTATION_RATE) {
                    char newChar;
                    do {
                        newChar = ALPHABET[rand.nextInt(ALPHABET.length)];
                    } while (newChar == genome[i]);
                    genome[i] = newChar;
                }
            }
        }

        Individual copy() {
            Individual c = new Individual();
            c.genome = genome.clone();
            c.fitness = fitness;
            return c;
        }
    }
}
