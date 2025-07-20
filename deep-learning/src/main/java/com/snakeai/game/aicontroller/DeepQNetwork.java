package com.snakeai.game.aicontroller;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;

public class DeepQNetwork {

    private MultiLayerNetwork model;
    private String modelName;

    public DeepQNetwork(String modelName, int inputSize, int outputSize) {
        this.modelName = modelName;
        this.model = createModel(inputSize, outputSize, modelName);
    }

    private MultiLayerNetwork createModel(int inputSize, int outputSize, String modelType) {
        MultiLayerConfiguration conf;
        System.out.println("Creating model of type: " + modelType);

        switch (modelType) {
            case "model_wide":
                conf = new NeuralNetConfiguration.Builder()
                        .seed(123)
                        .weightInit(WeightInit.XAVIER)
                        .updater(new Adam(0.001))
                        .list()
                        .layer(0, new DenseLayer.Builder().nIn(inputSize).nOut(512)
                                .activation(Activation.RELU)
                                .build())
                        .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .activation(Activation.IDENTITY)
                                .nIn(512).nOut(outputSize).build())
                        .build();
                break;
            case "model_deep":
                conf = new NeuralNetConfiguration.Builder()
                        .seed(123)
                        .weightInit(WeightInit.XAVIER)
                        .updater(new Adam(0.001))
                        .list()
                        .layer(0, new DenseLayer.Builder().nIn(inputSize).nOut(256)
                                .activation(Activation.RELU)
                                .build())
                        .layer(1, new DenseLayer.Builder().nIn(256).nOut(256)
                                .activation(Activation.RELU)
                                .build())
                        .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .activation(Activation.IDENTITY)
                                .nIn(256).nOut(outputSize).build())
                        .build();
                break;
            case "model_simple":
            default:
                conf = new NeuralNetConfiguration.Builder()
                        .seed(123)
                        .weightInit(WeightInit.XAVIER)
                        .updater(new Adam(0.001))
                        .list()
                        .layer(0, new DenseLayer.Builder().nIn(inputSize).nOut(256)
                                .activation(Activation.RELU)
                                .build())
                        .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .activation(Activation.IDENTITY) // For Q-values
                                .nIn(256).nOut(outputSize).build())
                        .build();
                break;
        }

        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        return network;
    }

    public INDArray predict(INDArray state) {
        return model.output(state);
    }

    public void train(INDArray states, INDArray targets) {
        model.fit(states, targets);
    }

    public void saveModel() throws IOException {
        System.out.println("Saving model to " + modelName + ".zip");
        File file = new File(modelName + ".zip");
        ModelSerializer.writeModel(model, file, true);
    }

    public void loadModel() throws IOException {
        System.out.println("Loading model from " + modelName + ".zip");
        File file = new File(modelName + ".zip");
        this.model = ModelSerializer.restoreMultiLayerNetwork(file);
    }
}