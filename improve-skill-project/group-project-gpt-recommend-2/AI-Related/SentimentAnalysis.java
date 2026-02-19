import ai.djl.Application;
import ai.djl.Device;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import java.io.IOException;

public class SentimentAnalysis {
    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        String input = "I love using Deep Java Library for PyTorch models!";

        System.out.println();
        // Define criteria for the DistilBERT model
        Criteria<String, Classifications> criteria = Criteria.builder()
                .setTypes(String.class, Classifications.class)
                .optApplication(Application.NLP.SENTIMENT_ANALYSIS)
                .optEngine("PyTorch")
                .optModelUrls("djl://ai.djl.pytorch/distilbert") // Model from your file
//                .optDevice(Device.gpu())
                .build();

        try (ZooModel<String, Classifications> model = criteria.loadModel();
             Predictor<String, Classifications> predictor = model.newPredictor()) {
            Classifications result = predictor.predict(input);
            System.out.println(result);
        }
    }
}