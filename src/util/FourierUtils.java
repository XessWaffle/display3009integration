package util;

import java.util.ArrayList;
import java.util.HashSet;

public class FourierUtils {

    public static class Complex{
        public double real;
        public double imag;

        public Complex(double real, double imag){
            this.real = real;
            this.imag = imag;
        }

        public double magnitude(){
            return Math.sqrt(real * real + imag * imag);
        }
    }

    public static ArrayList<Integer> zeros(int num){
        ArrayList<Integer> zero = new ArrayList<>();
        for(int i = 0; i < num; i++){
            zero.add(0);
        }
        return zero;
    }

    public static ArrayList<Integer> scale(ArrayList<Integer> colors, double scale){
        ArrayList<Integer> dimmed = new ArrayList<>();

        for(int i = 0; i < colors.size(); i++){
            int redSample = (colors.get(i) >> 16) & 0xFF;
            int greenSample = (colors.get(i) >> 8) & 0xFF;
            int blueSample = colors.get(i) & 0xFF;

            int value = (int) (redSample * scale);
            value = ((value << 8) + (int) (greenSample * scale));
            value = ((value << 8) + (int) (blueSample * scale));

            dimmed.add(value);
        }

        return dimmed;
    }

    public static double clamp(double value, double max, double min){
        if(value > max) return max;
        if(value < min) return min;
        return value;
    }

    public static ArrayList<Complex> DFT(ArrayList<Double> samples) {

        ArrayList<Complex> transformed = new ArrayList<>();

        double maxReal = Double.MIN_VALUE, minReal = Double.MAX_VALUE, maxImaginary = Double.MIN_VALUE, minImaginary = Double.MAX_VALUE;

        for(int i = 0; i < samples.size(); i++) {
            double next = 0;
            double nextSin = 0;
            for (int j = 0; j < samples.size(); j++) {
                next += samples.get(j) * Math.cos(2 * Math.PI * i * j / samples.size());
                nextSin += samples.get(j) * Math.sin(2 * Math.PI * i * j / samples.size());
            }

            if(next > maxReal) maxReal = next;
            if(next < minReal) minReal = next;
            if(nextSin > maxImaginary) maxImaginary = nextSin;
            if(nextSin < minImaginary) minImaginary = nextSin;

            transformed.add(new Complex(next, nextSin));
        }

        return transformed;
    }


    public static ArrayList<Complex> DCT(ArrayList<Double> samples) {

        ArrayList<Complex> transformed = new ArrayList<>();

        double max = Double.MIN_VALUE, min = Double.MAX_VALUE;

        HashSet<Integer> counter = new HashSet<>();

        for(int i = 0; i < samples.size(); i++) {
            double next = 0;
            for (int j = 0; j < samples.size(); j++) {
                next += samples.get(j) * Math.cos(Math.PI * i * (j + 0.5) / samples.size());
            }

            if(next > max) max = next;
            if(next < min) min = next;

            counter.add((int) next);

            transformed.add(new Complex((int)next, 0));
        }


        //System.out.println(counter.size());

        if(max > 127 || min < -128)
            System.out.println("Uh oh! " + (int)max + " " + (int)min);

        return transformed;
    }


    public static ArrayList<Double> iDFT(ArrayList<Complex> samples, int filterStart, int filterEnd, boolean inclusive) {

        ArrayList<Double> transformed = new ArrayList<>();

        for(int i = 0; i < samples.size(); i++) {
            double next = 0;
            for (int j = 0; j < samples.size(); j++) {

                boolean inclusiveCase = (inclusive && j >= filterStart && j <= filterEnd);
                boolean exclusiveCase = (!inclusive && (j <= filterStart || j >= filterEnd));

                if(inclusiveCase || exclusiveCase)
                    next += samples.get(j).real * Math.cos(2 * Math.PI * i * j / samples.size())  + samples.get(j).imag * Math.sin(2 * Math.PI * i * j / samples.size());
            }
            next /= samples.size();
            transformed.add(next);
        }

        return transformed;
    }

    public static ArrayList<Double> iDCT(ArrayList<Complex> samples, int filterStart, int filterEnd, boolean inclusive) {

        ArrayList<Double> transformed = new ArrayList<>();

        for(int i = 0; i < samples.size(); i++) {
            double next = 0.5 * samples.get(0).real;
            for (int j = 1; j < samples.size(); j++) {

                boolean inclusiveCase = (inclusive && j >= filterStart && j <= filterEnd);
                boolean exclusiveCase = (!inclusive && (j <= filterStart || j >= filterEnd));

                if(inclusiveCase || exclusiveCase)
                    next += samples.get(j).real * Math.cos(Math.PI * (i + 0.5) * j / samples.size());
            }
            next *= 2.0 / samples.size();
            transformed.add(next);
        }

        return transformed;
    }

}
