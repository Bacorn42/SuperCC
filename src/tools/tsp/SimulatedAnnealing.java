package tools.tsp;

import javax.swing.*;
import java.util.Random;

public class SimulatedAnnealing {
    private double temparature;
    private double end;
    private double cooling;
    private int iterations;
    private int[][] distances;
    Random r = new Random();
    private int[] bestSolution;
    private int bestDistance;
    private int inputNodeSize;
    private int exitNodeSize;
    public int bestExit;
    private int exitChosen;

    private JTextPane output;

    public SimulatedAnnealing(double temparature, double end, double cooling, int iterations, int[][] distances,
                              int inputNodeSize, int exitNodeSize, JTextPane output) {
        this.temparature = temparature;
        this.end = end;
        this.cooling = cooling;
        this.iterations = iterations;
        this.distances = distances;
        this.inputNodeSize = inputNodeSize;
        this.exitNodeSize = exitNodeSize;
        this.bestSolution = initialSolution();
        this.bestDistance = calculateDistance(bestSolution);
        this.output = output;
    }

    public int[] start() {
        bestExit = exitChosen;

        if(inputNodeSize == 0) {
            return new int[]{};
        }
        if(inputNodeSize == 1) {
            return new int[]{1};
        }

        int[] solution = bestSolution.clone();
        int distance = bestDistance;

        while(temparature > end) {
            temparature *= cooling;
            output.setText("Calculating shortest path...\nTemperature: " + temparature + "\nCurrent best: " + bestDistance);
            int startDistance = distance;
            for(int i = 0; i < iterations; i++) {
                int[] newSolution = solution.clone();
                mutate(newSolution);
                int newDistance = calculateDistance(newSolution);

                if(newDistance < distance) {
                    distance = newDistance;
                    solution = newSolution.clone();
                }
                else if(Math.exp(((double)distance - newDistance)/temparature) > r.nextDouble()) {
                    distance = newDistance;
                    solution = newSolution.clone();
                }

                if(newDistance < bestDistance) {
                    bestDistance = newDistance;
                    bestSolution = newSolution.clone();
                    bestExit = exitChosen;
                    System.out.println(temparature);
                    System.out.println(bestDistance);
                }
            }
            if(distance < startDistance) {
                temparature /= cooling;
            }
        }
        return bestSolution;
    }

    private int[] initialSolution() {
        int[] solution = new int[inputNodeSize];
        for(int i = 0; i < inputNodeSize; i++) {
            solution[i] = i + 1;
        }

        for(int i = solution.length - 1; i > 0; i--) {
            int index = r.nextInt(i + 1);
            int temp = solution[index];
            solution[index] = solution[i];
            solution[i] = temp;
        }

        return solution;
    }

    private int calculateDistance(int[] solution) {
        int distance = distances[0][solution[0]];
        for(int i = 0; i < solution.length - 1; i++) {
            distance += distances[solution[i]][solution[i + 1]];
        }

        exitChosen = 0;
        int bestExitDistance = distances[solution[solution.length - 1]][solution.length + 1];
        for(int i = 1; i < exitNodeSize; i++) {
            if(distances[solution[solution.length - 1]][solution.length + 1 + i] < bestExitDistance) {
                exitChosen = i;
                bestExitDistance = distances[solution[solution.length - 1]][solution.length + 1 + i];
            }
        }
        distance += bestExitDistance;
        return distance;
    }

    private void mutate(int[] solution) {
        int index1 = r.nextInt(solution.length);
        int index2 = index1;
        while(index1 == index2) {
            index2 = r.nextInt(solution.length);
        }
        if(index1 > index2) {
            int temp = index1;
            index1 = index2;
            index2 = temp;
        }

        int type = r.nextInt(3);

        if(type == 0) {
            int temp = solution[index1];
            solution[index1] = solution[index2];
            solution[index2] = temp;
        }
        else if(type == 1) {
            int temp = solution[index1];
            for(int i = index1 + 1; i <= index2; i++) {
                solution[i - 1] = solution[i];
            }
            solution[index2] = temp;
        }
        else if(type == 2) {
            for(int i = index1; i <= (index1 + index2)/2; i++) {
                int temp = solution[i];
                solution[i] = solution[solution.length - i - 1];
                solution[solution.length - i - 1] = temp;
            }
        }
    }
}
