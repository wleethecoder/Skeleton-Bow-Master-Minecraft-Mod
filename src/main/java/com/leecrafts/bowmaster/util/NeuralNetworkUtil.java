package com.leecrafts.bowmaster.util;

import com.leecrafts.bowmaster.entity.custom.SkeletonBowMasterEntity;
import com.leecrafts.bowmaster.neuralnetwork.NetworkLayer;
import com.leecrafts.bowmaster.neuralnetwork.NeuralNetwork;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeuralNetworkUtil {

    private static final File MODEL_DIRECTORY_PATH = new File(System.getProperty("user.dir"), "networks");
    private static final String MODEL_BASE_NAME = "model";
    public static final File REWARDS_LOG_FILE_PATH = new File(System.getProperty("user.dir"), "rewardlog/rewards.csv");
    private static final int INPUT_SIZE = 9;
    private static final int OUTPUT_SIZE = 12;
    private static double LEARNING_RATE = 0.1;
    private static final double GAMMA = 0.99;
    public static final double EPSILON_MAX = 0.9;
    public static final double EPSILON_MIN = 0.1;
    public static final double EPSILON_DECAY = (EPSILON_MAX - EPSILON_MIN) / 100; // linear epsilon decay
    public static double EPSILON = EPSILON_MAX;
    public static final double GAUSSIAN_NOISE_MAX = 1.0;
    public static final double GAUSSIAN_NOISE_MIN = 0.1;
    public static final double GAUSSIAN_NOISE_DECAY = (GAUSSIAN_NOISE_MAX - GAUSSIAN_NOISE_MIN) / 100; // linear gaussian noise decay
    public static double GAUSSIAN_NOISE = GAUSSIAN_NOISE_MAX;

    /*
    Actions:
    - Turn head (pitch and yaw)
    - Press / don't press right click
    - Move forward/backward/neither
    - Strafe left/right/neither
    - Jump / no jump
     */
    public static NeuralNetwork createNetwork() {
        int[] hiddenLayerSizes = {32};
        String[] hiddenActivations = {NeuralNetwork.TANH};
        int[] outputSizes = {2, 2, 3, 3, 2};
        String[] outputActivations = {
                NeuralNetwork.TANH,
                NeuralNetwork.SOFTMAX,
                NeuralNetwork.SOFTMAX,
                NeuralNetwork.SOFTMAX,
                NeuralNetwork.SOFTMAX};

        return new NeuralNetwork(INPUT_SIZE, hiddenLayerSizes, hiddenActivations, outputSizes, outputActivations);
    }

//    public static void updateNetwork(NeuralNetwork network,
//                                     ArrayList<double[]> states,
//                                     ArrayList<List<double[]>> actionProbs,
//                                     ArrayList<int[]> actions,
//                                     ArrayList<Double> rewards) {
//        ArrayList<Double> returns = new ArrayList<>();
//        double cumulativeReturn = 0.0;
//        for (int i = rewards.size() - 1; i >= 0; i--) {
//            cumulativeReturn = rewards.get(i) + GAMMA * cumulativeReturn;
//            returns.add(0, cumulativeReturn);
//        }
//
//        // Assume each layer might have different gradient needs based on activation functions
//        List<double[]> averageGradients = new ArrayList<>(network.getOutputLayers().size());
//        for (NetworkLayer layer : network.getOutputLayers()) {
//            averageGradients.add(new double[layer.getNeurons().size()]);
//        }
//
//        for (int t = 0; t < states.size(); t++) {
//            List<double[]> probsList = actionProbs.get(t);
//            double Gt = returns.get(t);
//            for (int layer = 0; layer < network.getOutputLayers().size(); layer++) {
//                double[] probs = probsList.get(layer);
//                double[] gradients = new double[probs.length];
//
//                for (int a = 0; a < probs.length; a++) {
//                    String af = network.getOutputActivations()[layer];
//                    if (af.equals(NeuralNetwork.SOFTMAX)) {
//                        int chosenAction = actions.get(t)[layer];
//                        // Multiply gradient by Gt here
//                        gradients[a] = ((a == chosenAction ? 1 : 0) - probs[a]) * Gt;
//                    }
//                    else if (af.equals(NeuralNetwork.TANH)) {
//                        gradients[a] = probs[a] * Gt; // Direct use of action value as part of the gradient calculation
//                    }
//                }
//
//                network.getOutputLayers().get(layer).updateLayerWeights(gradients, LEARNING_RATE);
//
//                // Accumulate gradients for backpropagation (simple average or sum)
//                for (int i = 0; i < gradients.length; i++) {
//                    averageGradients.get(layer)[i] += (gradients[i] / states.size());
//                }
//            }
//        }
//
//        // Backpropagation
//        network.backpropagate(averageGradients, LEARNING_RATE);
//    }

    public static void updateNetwork(NeuralNetwork network,
                                     ArrayList<double[]> states,
                                     ArrayList<List<double[]>> actionProbs,
                                     ArrayList<int[]> actions,
                                     ArrayList<Double> rewards) {
        // Calculate returns (discounted cumulative rewards)
        ArrayList<Double> returns = new ArrayList<>();
        double cumulativeReturn = 0.0;
        for (int i = rewards.size() - 1; i >= 0; i--) {
            cumulativeReturn = rewards.get(i) + GAMMA * cumulativeReturn;
            returns.add(0, cumulativeReturn);
        }

        // Initialize gradients for each output layer
        List<double[]> gradients = new ArrayList<>();
        for (NetworkLayer layer : network.getOutputLayers()) {
            gradients.add(new double[layer.getNeurons().size()]);
        }

        // Calculate gradients for each time step
        for (int t = 0; t < states.size(); t++) {
            List<double[]> probsList = actionProbs.get(t);
            double Gt = returns.get(t);

            for (int layerIndex = 0; layerIndex < network.getOutputLayers().size(); layerIndex++) {
                double[] probs = probsList.get(layerIndex);
                int chosenAction = actions.get(t)[layerIndex];
                String activationFunction = network.getOutputActivations()[layerIndex];

                for (int a = 0; a < probs.length; a++) {
                    if (activationFunction.equals(NeuralNetwork.SOFTMAX)) {
                        // For softmax, we use the policy gradient theorem
                        gradients.get(layerIndex)[a] += ((a == chosenAction ? 1 : 0) - probs[a]) * Gt;
                    } else if (activationFunction.equals(NeuralNetwork.TANH)) {
                        // For tanh, we use the actual output as the gradient
                        gradients.get(layerIndex)[a] += probs[a] * Gt;
                    }
                }
            }
        }

        // Normalize gradients by the number of samples
        for (double[] layerGradients : gradients) {
            for (int i = 0; i < layerGradients.length; i++) {
                layerGradients[i] /= states.size();
            }
        }

        // Apply gradients using backpropagation
        network.backpropagate(gradients, LEARNING_RATE);
    }

    public static void print2DArray(double[][] array) {
        System.out.print("[");
        for (int i = 0; i < array.length; i++) {
            System.out.print("[");
            for (int j = 0; j < array[i].length; j++) {
                System.out.print(array[i][j]);
                if (j < array[i].length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.print("]");
            if (i < array.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
    }

    public static void saveModel(NeuralNetwork network) {
        NeuralNetwork.saveModel(network, modelFile(getNewestModelNumber() + 1));
    }

    public static NeuralNetwork loadOrCreateModel() {
        if (SkeletonBowMasterEntity.PRODUCTION) {
            // Access a file within the JAR through the class loader
            try (InputStream inputStream = NeuralNetworkUtil.class.getResourceAsStream("/assets/bowmaster/network/model.dat");
                 ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                System.out.println("existing model in resources folder found");
                return (NeuralNetwork) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading network: " + e.getMessage());
            }
        }
        return loadOrCreateModel(getNewestModelNumber());
    }

    public static NeuralNetwork loadOrCreateModel(int modelNumber) {
        // TODO consider learning rate decay
        // TODO consider epsilon decay
        EPSILON = Math.max(EPSILON_MAX - EPSILON_DECAY * modelNumber, EPSILON_MIN);
        GAUSSIAN_NOISE = Math.max(GAUSSIAN_NOISE_MAX - GAUSSIAN_NOISE_DECAY * modelNumber, GAUSSIAN_NOISE_MIN);
        File file = modelFile(modelNumber);
        if (file.exists()) {
            System.out.println("existing model found (" + modelNumber + ")");
            return NeuralNetwork.loadModel(file);
        }
        else {
            System.out.println("no model found, so creating new one");
            return createNetwork();
        }
    }

    private static int getNewestModelNumber() {
        File[] files = MODEL_DIRECTORY_PATH.listFiles();  // Get all files in the directory
        int maxNum = 0;
        if (files == null) {
            return maxNum;
        }

        Pattern pattern = Pattern.compile(String.format("^%s-(\\d+)\\.dat$", MODEL_BASE_NAME));  // Regex to find files
        for (File file : files) {
            if (file.isFile()) {
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.matches()) {
                    maxNum = Math.max(maxNum, Integer.parseInt(matcher.group(1)));
                }
            }
        }

        return maxNum;
    }

    private static File modelFile(int modelNumber) {
//        return new File(String.format("%s/%s-%d.dat", MODEL_DIRECTORY_PATH, MODEL_BASE_NAME, modelNumber));
        return new File(MODEL_DIRECTORY_PATH, String.format("%s-%d.dat", MODEL_BASE_NAME, modelNumber));
    }

    public static void logRewards(ArrayList<Double> winnerRewards, ArrayList<Double> loserRewards) {
        try {
            FileWriter writer = new FileWriter(REWARDS_LOG_FILE_PATH, true);
            if (REWARDS_LOG_FILE_PATH.length() == 0) {
                writer.append("Episode,Winner Total Reward,Loser Total Reward\n");
            }
            writer.append(String.valueOf(getNewestModelNumber() + 1))
                    .append(",")
                    .append(String.valueOf(winnerRewards.stream().mapToDouble(Double::doubleValue).sum()))
                    .append(",")
                    .append(String.valueOf(loserRewards.stream().mapToDouble(Double::doubleValue).sum()))
                    .append("\n");
            writer.close();
            System.out.println("Rewards have been logged.");
        } catch (IOException e) {
            System.out.println("Error logging rewards: " + e.getMessage());
        }
    }

}
