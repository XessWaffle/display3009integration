package util;

import java.util.ArrayList;
import java.util.HashMap;

import util.FourierUtils.Complex;

public class TransformationUtils {

    public record Triple<T, U, V>(T first, U second, V third) {}


    public static Triple<Integer, Integer, Integer> SplitRGB(int rgb){
        return new Triple<>((rgb >> 16)  & 0xFF, (rgb >> 8)  & 0xFF, rgb  & 0xFF);
    }

    public static int CombineRGB(Triple<Integer, Integer, Integer> rgb){
        int value = rgb.first();
        value = ((value << 8) + rgb.second());
        value = ((value << 8) + rgb.third());
        return value;
    }

    public static void RadialDerivative(HashMap<Integer, ArrayList<Integer>> samples,
                                        HashMap<Integer, ArrayList<Integer>> transform,
                                        int sectors){
        for(int i = 0; i < sectors; i++){
            int prev = samples.get(i).get(0), curr;
            for(int j = 0; j < samples.get(i).size(); j++){
                transform.putIfAbsent(i, new ArrayList<>());
                if(j != 0) {
                    curr = samples.get(i).get(j);

                    Triple<Integer, Integer, Integer> currColor = TransformationUtils.SplitRGB(curr),
                            prevColor = TransformationUtils.SplitRGB(prev),
                            difference = new Triple<>(((currColor.first() - prevColor.first())),
                                    ((currColor.second() - prevColor.second())),
                                    ((currColor.third() - prevColor.third())));
                    transform.get(i).add(TransformationUtils.CombineRGB(difference));
                    prev = curr;
                } else {
                    transform.get(i).add(prev);
                }

            }
        }
    }

    public static void RadialIntegration(HashMap<Integer, ArrayList<Integer>> samples,
                                        HashMap<Integer, ArrayList<Integer>> transform,
                                        int sectors){
        for(int i = 0; i < sectors; i++){
            int first = samples.get(i).get(0), total = first;
            for(int j = 0; j < samples.get(i).size(); j++){
                transform.putIfAbsent(i, new ArrayList<>());
                if(j != 0) {
                    int curr = samples.get(i).get(j);
                    Triple<Integer, Integer, Integer> currDer = TransformationUtils.SplitRGB(curr),
                            totalColor = TransformationUtils.SplitRGB(total),
                            nextTotal = new Triple<>(totalColor.first() + (currDer.first()),
                                    totalColor.second() + (currDer.second()),
                                    totalColor.third() + (currDer.third()));

                    System.out.println(currDer.first() + " " + currDer.second() + " " + currDer.third());



                    total = TransformationUtils.CombineRGB(nextTotal);
                    transform.get(i).add(total);
                } else {
                    transform.get(i).add(first);
                }

            }
        }
    }

    public static void AngularDerivative(HashMap<Integer, ArrayList<Integer>> samples,
                                        HashMap<Integer, ArrayList<Integer>> transform,
                                        int sectors){
        ArrayList<Integer> prev = samples.get(0), curr;

        transform.put(0, new ArrayList<>(prev));

        for(int i = 1; i < sectors; i++){

            curr = samples.get(i);

            for(int j = 0; j < samples.get(i).size(); j++){
                transform.putIfAbsent(i, new ArrayList<>());

                Triple<Integer, Integer, Integer> currColor = TransformationUtils.SplitRGB(curr.get(j)),
                        prevColor = TransformationUtils.SplitRGB(prev.get(j)),
                        difference = new Triple<>((currColor.first() - prevColor.first()),
                                (currColor.second() - prevColor.second()),
                                (currColor.third() - prevColor.third()));
                transform.get(i).add(TransformationUtils.CombineRGB(difference));

            }
            prev = curr;
        }
    }

    public static void AngularIntegration(HashMap<Integer, ArrayList<Integer>> samples,
                                         HashMap<Integer, ArrayList<Integer>> transform,
                                         int sectors){
        ArrayList<Integer> total = new ArrayList<>(samples.get(0));

        transform.put(0, new ArrayList<>(total));

        for(int i = 1; i < sectors; i++){

            ArrayList<Integer> derArray = samples.get(i);

            for(int j = 0; j < samples.get(i).size(); j++){
                transform.putIfAbsent(i, new ArrayList<>());

                Triple<Integer, Integer, Integer> currDer = TransformationUtils.SplitRGB(derArray.get(j)),
                        totalColor = TransformationUtils.SplitRGB(total.get(j)),
                        nextTotal = new Triple<>(totalColor.first() + (currDer.first()),
                                totalColor.second() + (currDer.second()),
                                totalColor.third() + (currDer.third()));
                total.set(j, TransformationUtils.CombineRGB(nextTotal));
                transform.get(i).add(total.get(j));
            }
        }
    }

    public static void RadialFourierTransform(HashMap<Integer, ArrayList<Integer>> samples,
                                              HashMap<Integer, ArrayList<Integer>> imaginaryTransform,
                                              HashMap<Integer, ArrayList<Integer>> realTransform,
                                              HashMap<Integer, ArrayList<Triple<Complex, Complex, Complex>>> transform,
                                              int sectors, int ftSamples, boolean useDCT){
        for(int i = 0; i < sectors; i++){
            ArrayList<Integer> sectorColors = samples.get(i);

            ArrayList<Double> red = new ArrayList<>(), green = new ArrayList<>(), blue = new ArrayList<>(), temp = new ArrayList<>();
            ArrayList<FourierUtils.Complex> redFt = new ArrayList<>(), greenFt = new ArrayList<>(), blueFt = new ArrayList<>();

            sectorColors.forEach((e)->{
                int redSample = (e >> 16) & 0xFF;
                int greenSample = (e >> 8) & 0xFF;
                int blueSample = e & 0xFF;
                red.add((double) redSample - 128.0);
                green.add((double) greenSample - 128.0);
                blue.add((double) blueSample - 128.0);
            });


            for(int j = 0; j < red.size(); j++){
                temp.add(red.get(j));
                if(temp.size() >= ftSamples) {
                    redFt.addAll(useDCT ? FourierUtils.DCT(temp) : FourierUtils.DFT(temp));
                    temp.clear();
                }
            }

            for(int j = 0; j < green.size(); j++){
                temp.add(green.get(j));
                if(temp.size() >= ftSamples) {
                    greenFt.addAll(useDCT ? FourierUtils.DCT(temp) : FourierUtils.DFT(temp));
                    temp.clear();
                }
            }

            for(int j = 0; j < blue.size(); j++){
                temp.add(blue.get(j));
                if(temp.size() >= ftSamples) {
                    blueFt.addAll(useDCT ? FourierUtils.DCT(temp) : FourierUtils.DFT(temp));
                    temp.clear();
                }
            }

            ArrayList<Triple<FourierUtils.Complex, FourierUtils.Complex, FourierUtils.Complex>> sectorFt = new ArrayList<>();
            ArrayList<Integer> sectorR = new ArrayList<>(), sectorI = new ArrayList<>();

            for(int j = 0; j < redFt.size(); j++){
                sectorFt.add(new Triple<>(redFt.get(j), greenFt.get(j), blueFt.get(j)));

                int redV = (int) redFt.get(j).real;
                int greenV = (int) greenFt.get(j).real;
                int blueV = (int) blueFt.get(j).real;

                int value = redV;
                value = ((value << 8) + greenV);
                value = ((value << 8) + blueV);
                sectorR.add(value);

                redV = (int) redFt.get(j).imag;
                greenV = (int) greenFt.get(j).imag;
                blueV = (int) blueFt.get(j).imag;

                value = redV;
                value = ((value << 8) + greenV);
                value = ((value << 8) + blueV);

                sectorI.add(value);
            }

            realTransform.put(i, sectorR);
            imaginaryTransform.put(i, sectorI);
            transform.put(i, sectorFt);
        }
    }

    public static void AngularFourierTransform(HashMap<Integer, ArrayList<Integer>> samples,
                                               HashMap<Integer, ArrayList<Integer>> imaginaryTransform,
                                               HashMap<Integer, ArrayList<Integer>> realTransform,
                                               HashMap<Integer, ArrayList<Triple<Complex, Complex, Complex>>> transform,
                                               int radius, int ftSamples, boolean useDCT){
        for(int i = 0; i < radius; i++){
            ArrayList<Integer> sectorColors = samples.get(i);

            ArrayList<Double> red = new ArrayList<>(), green = new ArrayList<>(), blue = new ArrayList<>(), temp = new ArrayList<>();
            ArrayList<Complex> redFt = new ArrayList<>(), greenFt = new ArrayList<>(), blueFt = new ArrayList<>();

            sectorColors.forEach((e)->{
                int redSample = (e >> 16) & 0xFF;
                int greenSample = (e >> 8) & 0xFF;
                int blueSample = e & 0xFF;
                red.add((double) redSample - 128.0);
                green.add((double) greenSample - 128.0);
                blue.add((double) blueSample - 128.0);
            });

            for(int j = 0; j < red.size(); j++){
                temp.add(red.get(j));
                if(temp.size() >= ftSamples) {
                    redFt.addAll(useDCT ? FourierUtils.DCT(temp) : FourierUtils.DFT(temp));
                    temp.clear();
                }
            }

            for(int j = 0; j < green.size(); j++){
                temp.add(green.get(j));
                if(temp.size() >= ftSamples) {
                    greenFt.addAll(useDCT ? FourierUtils.DCT(temp) : FourierUtils.DFT(temp));
                    temp.clear();
                }
            }

            for(int j = 0; j < blue.size(); j++){
                temp.add(blue.get(j));
                if(temp.size() >= ftSamples) {
                    blueFt.addAll(useDCT ? FourierUtils.DCT(temp) : FourierUtils.DFT(temp));
                    temp.clear();
                }
            }

            ArrayList<Triple<Complex, Complex, Complex>> sectorFt = new ArrayList<>();


            for(int j = 0; j < redFt.size(); j++){
                sectorFt.add(new Triple<>(redFt.get(j), greenFt.get(j), blueFt.get(j)));

                int redV = (int) redFt.get(j).real;
                int greenV = (int) greenFt.get(j).real;
                int blueV = (int) blueFt.get(j).real;

                int value = redV;
                value = ((value << 8) + greenV);
                value = ((value << 8) + blueV);

                realTransform.putIfAbsent(j, new ArrayList<Integer>());
                realTransform.get(j).add(value);

                redV = (int) redFt.get(j).imag;
                greenV = (int) greenFt.get(j).imag;
                blueV = (int) blueFt.get(j).imag;

                value = redV;
                value = ((value << 8) + greenV);
                value = ((value << 8) + blueV);

                imaginaryTransform.putIfAbsent(j, new ArrayList<Integer>());
                imaginaryTransform.get(j).add(value);

            }

            transform.put(i, sectorFt);
        }
    }

    public static void InverseRadialFourierTransform(HashMap<Integer, ArrayList<Triple<Complex, Complex, Complex>>> transform,
                                                     HashMap<Integer, ArrayList<Integer>> invTransform,
                                                     int sectors, int ftSamples, int filterStart, int filterEnd, boolean useDCT){
        for(int i = 0; i < sectors; i++){
            ArrayList<Triple<Complex, Complex, Complex>> ftColors = transform.get(i);

            ArrayList<Complex> red = new ArrayList<>(), green = new ArrayList<>(), blue = new ArrayList<>(), temp = new ArrayList<>();
            ArrayList<Double> sectorT = new ArrayList<>(), greenT = new ArrayList<>(), blueT = new ArrayList<>();

            ftColors.forEach((e)->{
                red.add(e.first);
                green.add(e.second);
                blue.add(e.third);
            });

            for(int j = 0; j < red.size(); j++){
                temp.add(red.get(j));
                if(temp.size() >= ftSamples) {
                    sectorT.addAll(useDCT ? FourierUtils.iDCT(temp, filterStart, filterEnd, true) :
                            FourierUtils.iDFT(temp, filterStart, filterEnd, true));
                    temp.clear();
                }
            }

            for(int j = 0; j < green.size(); j++){
                temp.add(green.get(j));
                if(temp.size() >= ftSamples) {
                    greenT.addAll(useDCT ? FourierUtils.iDCT(temp, filterStart, filterEnd, true) :
                            FourierUtils.iDFT(temp, filterStart, filterEnd, true));
                    temp.clear();
                }
            }

            for(int j = 0; j < blue.size(); j++){
                temp.add(blue.get(j));
                if(temp.size() >= ftSamples) {
                    blueT.addAll(useDCT ? FourierUtils.iDCT(temp, filterStart, filterEnd, true) :
                            FourierUtils.iDFT(temp, filterStart, filterEnd, true));
                    temp.clear();
                }
            }

            ArrayList<Integer> trueColors = new ArrayList<>();

            for(int j = 0; j < sectorT.size(); j++){
                int value = (int) (sectorT.get(j) + 128);
                value = ((value << 8) + (int)(greenT.get(j) + 128));
                value = ((value << 8) + (int)(blueT.get(j) + 128));
                trueColors.add(value);
            }

            invTransform.put(i, trueColors);
        }
    }

    public static void InverseAngularFourierTransform(HashMap<Integer, ArrayList<Triple<Complex, Complex, Complex>>> transform,
                                                      HashMap<Integer, ArrayList<Integer>> invTransform,
                                                      int radius, int ftSamples, int filterStart, int filterEnd, boolean useDCT){
        for(int i = 0; i < radius; i++){
            ArrayList<Triple<Complex, Complex, Complex>> ftColors = transform.get(i);

            ArrayList<Complex> red = new ArrayList<>(), green = new ArrayList<>(), blue = new ArrayList<>(), temp = new ArrayList<>();
            ArrayList<Double> sectorT = new ArrayList<>(), greenT = new ArrayList<>(), blueT = new ArrayList<>();

            ftColors.forEach((e)->{
                red.add(e.first);
                green.add(e.second);
                blue.add(e.third);
            });

            for(int j = 0; j < red.size(); j++){
                temp.add(red.get(j));
                if(temp.size() >= ftSamples) {
                    sectorT.addAll(useDCT ? FourierUtils.iDCT(temp, filterStart, filterEnd, true) :
                            FourierUtils.iDFT(temp, filterStart, filterEnd, true));
                    temp.clear();
                }
            }

            for(int j = 0; j < green.size(); j++){
                temp.add(green.get(j));
                if(temp.size() >= ftSamples) {
                    greenT.addAll(useDCT ? FourierUtils.iDCT(temp, filterStart, filterEnd, true) :
                            FourierUtils.iDFT(temp, filterStart, filterEnd, true));
                    temp.clear();
                }
            }

            for(int j = 0; j < blue.size(); j++){
                temp.add(blue.get(j));
                if(temp.size() >= ftSamples) {
                    blueT.addAll(useDCT ? FourierUtils.iDCT(temp, filterStart, filterEnd, true) :
                            FourierUtils.iDFT(temp, filterStart, filterEnd, true));
                    temp.clear();
                }
            }

            for(int j = 0; j < sectorT.size(); j++){
                int value = (int) (sectorT.get(j) + 128);
                value = ((value << 8) + (int)(greenT.get(j) + 128));
                value = ((value << 8) + (int)(blueT.get(j) + 128));

                invTransform.putIfAbsent(j, new ArrayList<Integer>());
                invTransform.get(j).add(value);
            }
        }

    }

}
