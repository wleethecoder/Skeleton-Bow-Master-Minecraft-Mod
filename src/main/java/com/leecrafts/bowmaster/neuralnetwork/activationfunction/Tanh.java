package com.leecrafts.bowmaster.neuralnetwork.activationfunction;

import com.leecrafts.bowmaster.neuralnetwork.NeuralNetwork;

import java.io.Serializable;

public class Tanh implements ActivationFunction, Serializable {

    public String getString() {
        return NeuralNetwork.TANH;
    }

    public double activate(double input) {
        return Math.tanh(input);
    }

    public double[] activate(double[] inputs) {
        double[] outputs = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            outputs[i] = this.activate(inputs[i]);
        }
        return outputs;
    }
}