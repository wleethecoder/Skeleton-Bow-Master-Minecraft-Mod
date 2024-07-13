package com.leecrafts.bowmaster.neuralnetwork.activationfunction;

import java.io.Serializable;

public class Tanh implements ActivationFunction, Serializable {

    public String getString() {
        return "tanh";
    }

    public double activate(double input) {
        return Math.tanh(input);
    }

    public double[] activate(double[] inputs) {
        double[] outputs = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            outputs[i] = Math.tanh(inputs[i]);
        }
        return outputs;
    }
}