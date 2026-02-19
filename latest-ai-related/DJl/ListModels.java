import ai.djl.Application;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.MRL;

import java.util.List;
import java.util.Map;

public class ListModels {
    public static void main(String[] args) {
        Map<Application, List<MRL>> models =
                ModelZoo.listModels(Criteria.builder().build());

        System.out.println("Available Models:");
        for (Map.Entry<Application, List<MRL>> entry : models.entrySet()) {
            System.out.println("\n== " + entry.getKey().getPath() + " ==");
            for (MRL model : entry.getValue()) {
                System.out.println("  - " + model);
            }
        }
    }
}
