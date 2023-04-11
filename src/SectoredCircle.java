import javafx.util.Pair;
import server.ESPControlServer;
import util.EncodingUtils;
import util.FourierUtils.Complex;
import util.TransformationUtils;
import util.TransformationUtils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SectoredCircle extends JPanel {


    public static final int PANEL_WIDTH = 800;
    public static final int PANEL_HEIGHT = 800;

    public static final int SECTORS = 360;

    public static final int NUM_LEDS = 72;

    public static final int SAMPLE_RADIUS = 4;

    public static final byte[] FRAME_CLIENTS = {0x02, 0x03};
    private static final int FT_SAMPLES = 12;

    private BufferedImage img, result;
    private int xOff, yOff;
    private int xPix = -1, yPix = -1;
    private int scaling;

    private boolean show = true, showImage = true, showSampling = false;

    private HashMap<Integer, HashMap<Integer, Integer>> sampled;
    private HashMap<Integer, ArrayList<Integer>> converted, full, ring, derivative, ringDerivative, integration, ringIntegration,
                                                itransform, vrtransform, vitransform,
                                                vrderTransform, vrderringTransform, viderTransform, viderringTransform,
                                                iringTransform, vrringTransform, viringTransform,
                                                iderTransform, iderRingTransform;

    private HashMap<Integer, ArrayList<Integer>> displayed;
    private HashMap<Integer, ArrayList<Triple<Complex, Complex, Complex>>> transform, ringTransform, derTransform, derRingTransform;
    // Sector, <Sector to Copy, Changed values>
    private HashMap<Integer, Pair<Integer, HashMap<Integer, Integer>>> copies;

    // Sector, <Color, Start LED, End LED>
    private HashMap<Integer, ArrayList<Triple<Integer, Integer, Integer>>> compiled;

    private HashSet<Integer> colorMap;


    public SectoredCircle(){
        this.setBounds(0,0, PANEL_WIDTH, PANEL_HEIGHT);
        xOff = 0;
        yOff = 0;
        sampled = new HashMap<>();
        converted = new HashMap<>();
        full = new HashMap<>();
        ring = new HashMap<>();
        iringTransform = new HashMap<>();
        vrringTransform = new HashMap<>();
        viringTransform = new HashMap<>();
        ringTransform = new HashMap<>();
        copies = new HashMap<>();
        compiled = new HashMap<>();
        transform = new HashMap<>();
        itransform = new HashMap<>();
        colorMap = new HashSet<>();
        vrtransform = new HashMap<>();
        vitransform = new HashMap<>();
        derivative = new HashMap<>();
        ringDerivative = new HashMap<>();
        vrderTransform = new HashMap<>();
        vrderringTransform = new HashMap<>();
        viderTransform = new HashMap<>();
        viderringTransform = new HashMap<>();
        derTransform = new HashMap<>();
        derRingTransform = new HashMap<>();
        iderRingTransform = new HashMap<>();
        iderTransform = new HashMap<>();
        integration = new HashMap<>();
        ringIntegration = new HashMap();
        displayed = full;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        if(!this.show) {
            g2d.setColor(Color.BLACK);
            g2d.fillOval(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.fillOval(390, 390, 20, 20);
        }

        if(this.img != null && showImage) g2d.drawImage(this.img, xOff - scaling/2, yOff - scaling/2, PANEL_WIDTH + scaling, PANEL_HEIGHT + scaling, null);

        if(this.show){
            g2d.drawOval(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
            g2d.drawOval(390, 390, 20, 20);
        }

        if(showSampling){

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

            ArrayList<Integer> crgbs = null;

            int sector = 0;

            for (double i = 0; i < 2 * Math.PI; i += 2 * Math.PI / SECTORS) {

                double angle = i + Math.PI / SECTORS;

                double startX = (Math.cos(angle) * 10 + 400), startY = (Math.sin(angle) * 10 + 400),
                        endX = (Math.cos(angle) * 400 + 400), endY = (Math.sin(angle) * 400 + 400);

                double dist = Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
                double unitX = (endX - startX)/dist, unitY = (endY - startY)/dist;

                double iterX = startX, iterY = startY;

                boolean highlight = converted.containsKey(sector);

                crgbs = displayed.get(sector);

                for (int j = 0; j < NUM_LEDS; j++) {
                    int crgb = crgbs.get(j);

                    if (highlight && j == 71) {
                        g2d.setColor(Color.RED);
                        g2d.fillOval((int) (iterX - 3), (int) (iterY - 3), 6, 6);
                    }

                    g2d.setColor(new Color(crgb));
                    g2d.fillOval((int) (iterX - 2), (int) (iterY - 2), 4, 4);

                    iterX += unitX * dist / NUM_LEDS;
                    iterY += unitY * dist / NUM_LEDS;
                }
                sector++;

                if(sector >= SECTORS) sector -= SECTORS;
            }
        }

        if(this.xPix >= 0 && this.yPix >= 0) {

            if(showSampling) {
                double centeredX = xPix - PANEL_WIDTH / 2, centeredY = yPix - PANEL_HEIGHT / 2;
                double angle = Math.atan2(centeredY, centeredX);

                if(angle < 0){
                    angle += Math.PI * 2;
                }

                int sector = (int) (angle / (2 * Math.PI) * SECTORS), sectorCopy = -1;
                double sectorAngle = (double)sector/SECTORS * 2 * Math.PI;
                sectorAngle += Math.PI / SECTORS;
                double startX = (Math.cos(sectorAngle) * 10 + 400), startY = (Math.sin(sectorAngle) * 10 + 400),
                        endX = (Math.cos(sectorAngle) * 400 + 400), endY = (Math.sin(sectorAngle) * 400 + 400);
                g2d.setColor(Color.RED);
                //g2d.drawLine((int) startX, (int) startY, (int) endX, (int) endY);

                if (!compiled.isEmpty() && compiled.containsKey(sector)){
                    for(Triple<Integer, Integer, Integer> color : compiled.get(sector)) {
                        int startLed = color.first();
                        int endLed = color.second();
                        double unitX = (endX - startX) / NUM_LEDS, unitY = (endY - startY) / NUM_LEDS;

                        startX = (Math.cos(sectorAngle) * 10 + 400) + startLed * unitX;
                        startY = (Math.sin(sectorAngle) * 10 + 400) + startLed * unitY;
                        endX = (Math.cos(sectorAngle) * 10 + 400) + endLed * unitX;
                        endY = (Math.sin(sectorAngle) * 10 + 400) + endLed * unitY;

                        g2d.setColor(new Color(255, 255, 0, 255));
                        g2d.drawLine((int) startX, (int) startY, (int) endX, (int) endY);
                    }

                    Font font = new Font("Verdana", Font.BOLD, 20);
                    g2d.setColor(Color.WHITE);
                    drawCenteredString(g2d, compiled.get(sector).size() + "", new Rectangle(0, 0, PANEL_WIDTH, PANEL_HEIGHT), font);
                }

            } else {
                Font font = new Font("Verdana", Font.BOLD, 20);
                int rgb = result.getRGB(xPix, yPix);
                Triple<Integer, Integer, Integer> colors = TransformationUtils.SplitRGB(rgb);
                g2d.setColor(Color.BLACK);
                drawCenteredString(g2d, colors.first() + ", " + colors.second() + ", " + colors.third(), new Rectangle(0, 0, PANEL_WIDTH, PANEL_HEIGHT), font);
            }
        }

    }

    public void toggleSampleView(){
        this.showImage = !this.showImage;
        this.showSampling = !this.showSampling;
    }
    public BufferedImage generateImage(){
        BufferedImage result = new BufferedImage(PANEL_WIDTH , PANEL_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        paintComponent(result.getGraphics());
        this.result = result;
        return result;
    }

    public BufferedImage getImage() {
        return img;
    }

    public void setImage(BufferedImage img) {
        this.img = img;
    }

    public void setXoffset(int xOff){
        this.xOff = xOff;
    }

    public void setYoffset(int yOff){
        this.yOff = yOff;
    }

    public void setScaling(int scaling){
        this.scaling = scaling;
    }

    public void showGuide(boolean hide){
        this.show = !hide;
        this.repaint();
    }

    public void sample() {

        sampleImage();

        expandSampling();

        sampleDifferences();

        sampleDerivative(true);

        compileSamples();

        sampleTransform(true);

        sampleITransform(true);

        saveFiles();
    }

    private void saveFiles() {

        HashMap<Integer, ByteBuffer> fullEncoded = new HashMap<>();

        EncodingUtils.HuffmanEncode(full, fullEncoded, SECTORS);

        try {
            EncodingUtils.WriteToFile("./full.rie", fullEncoded);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HashMap<Integer, ByteBuffer> derivativeEncoded = new HashMap<>();

        EncodingUtils.HuffmanEncode(derivative, derivativeEncoded, SECTORS);

        try {
            EncodingUtils.WriteToFile("./derivative.rie", derivativeEncoded);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void sampleDerivative(boolean useDCT) {
        ringDerivative.clear();
        derivative.clear();
        vrderringTransform.clear();
        viderringTransform.clear();
        vrderTransform.clear();
        viderTransform.clear();
        iderTransform.clear();
        iderRingTransform.clear();
        integration.clear();
        ringIntegration.clear();

        TransformationUtils.RadialDerivative(full, derivative, SECTORS);

        TransformationUtils.RadialFourierTransform(derivative, viderTransform, vrderTransform, derTransform, SECTORS, FT_SAMPLES, useDCT);

        TransformationUtils.AngularDerivative(full, ringDerivative, SECTORS);

        TransformationUtils.RadialFourierTransform(ringDerivative, viderringTransform, vrderringTransform, derRingTransform, SECTORS, FT_SAMPLES, useDCT);

        int filterStart = 0;
        int filterEnd = 12;

        TransformationUtils.InverseRadialFourierTransform(derTransform, iderTransform, SECTORS, FT_SAMPLES, filterStart, filterEnd, useDCT);
        TransformationUtils.InverseRadialFourierTransform(derRingTransform, iderRingTransform, SECTORS, FT_SAMPLES, filterStart, filterEnd, useDCT);

        TransformationUtils.RadialIntegration(derivative, integration, SECTORS);
        TransformationUtils.AngularIntegration(ringDerivative, ringIntegration, SECTORS);

    }

    private void sampleTransform(boolean useDCT) {

        transform.clear();
        vrtransform.clear();
        vitransform.clear();

        TransformationUtils.RadialFourierTransform(full, vitransform, vrtransform, transform, SECTORS, FT_SAMPLES, useDCT);

        ringTransform.clear();
        vrringTransform.clear();
        vrringTransform.clear();

        TransformationUtils.AngularFourierTransform(ring, viringTransform, vrringTransform, ringTransform, NUM_LEDS, FT_SAMPLES, useDCT);

    }

    private void sampleITransform(boolean useDCT){

        iringTransform.clear();
        itransform.clear();

        int filterStart = 0;
        int filterEnd = FT_SAMPLES - 2;

        TransformationUtils.InverseRadialFourierTransform(transform, itransform, SECTORS, FT_SAMPLES, filterStart, filterEnd, useDCT);

        TransformationUtils.InverseAngularFourierTransform(ringTransform, iringTransform, NUM_LEDS, FT_SAMPLES, filterStart, filterEnd, useDCT);

    }


    private void sampleImage() {

        this.showGuide(true);
        boolean tempSampling = this.showSampling;
        this.showSampling = false;
        BufferedImage image = generateImage();
        this.showSampling = tempSampling;
        this.showGuide(false);
        try {
            ImageIO.write(image, "jpg", new File("./export.jpg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        sampled.clear();
        colorMap.clear();

        // LED <Sector, Color>
        for (int i = 0; i < NUM_LEDS; i++) {

            HashMap<Integer, Integer> ledColorChangeMap = new HashMap<>();

            int prevSector = 0, sector = 0;

            int trueSampleRadius = SAMPLE_RADIUS;

            int samples = SECTORS;
            int numSampled = 0;

            for (double j = 0; j < 2 * Math.PI; j += 2 * Math.PI / SECTORS) {
                double angle = j + Math.PI / SECTORS;

                double unitX = Math.cos(angle), unitY = Math.sin(angle);
                double startX = (unitX * 10 + 400), startY = (unitY * 10 + 400),
                        endX = (unitX * 400 + 400), endY = (unitY * 400 + 400);

                double trueUnitX = (endX - startX), trueUnitY = (endY - startY);

                double iterX = startX + (double) i / NUM_LEDS * trueUnitX, iterY = startY + (double) i / NUM_LEDS * trueUnitY;

                double red = 0, green = 0, blue = 0;

                for (int x = -trueSampleRadius; x <= trueSampleRadius; x++)
                    for (int y = -trueSampleRadius; y <= trueSampleRadius; y++) {
                        Color crgb = new Color(image.getRGB((int) iterX + x, (int) iterY + y));
                        int rgb = crgb.getRGB();

                        Triple<Integer, Integer, Integer> sample = TransformationUtils.SplitRGB(rgb);

                        red += sample.first();
                        green += sample.second();
                        blue += sample.third();
                    }

                double area = 255 * (trueSampleRadius * 2 + 1) * (trueSampleRadius * 2 + 1);

                double redB = red / area;
                double greenB = green / area;
                double blueB = blue / area;

                double gamma = 4.5;

                double redTB = Math.pow(redB, gamma);
                double greenTB = Math.pow(greenB, gamma);
                double blueTB = Math.pow(blueB, gamma);

                Triple<Integer, Integer, Integer> color = new Triple<>((int) (redTB * 255), (int) (greenTB * 176), (int) (blueTB * 240));

                int value = TransformationUtils.CombineRGB(color);

                if(angle > (2 * Math.PI / samples) * numSampled) {
                    colorMap.add(value);

                    if (ledColorChangeMap.isEmpty() || ledColorChangeMap.get(prevSector) != value) {
                        prevSector = sector;
                        ledColorChangeMap.put(prevSector, value);
                    }
                    numSampled++;
                }
                sector++;
            }
            sampled.put(i, ledColorChangeMap);
            //System.out.println(i + ":" + ledColorChangeMap.size());
        }

        //System.out.println("Unique Colors: " + colorMap.size());
    }

    private void expandSampling() {

        full.clear();
        converted.clear();
        ring.clear();

        ArrayList<Integer> prev = null;

        for (int i = 0; i < SECTORS; i++) {
            ArrayList<Integer> armFrame = new ArrayList<>();
            while (armFrame.size() < NUM_LEDS) armFrame.add(0);
            boolean hasSampled = false;
            for (int j = 0; j < NUM_LEDS; j++) {
                if (sampled.get(j).containsKey(i)) {
                    hasSampled = true;
                    armFrame.set(j, sampled.get(j).get(i));
                } else if (prev != null) {
                    armFrame.set(j, prev.get(j));
                }
            }

            if (hasSampled) {
                converted.put(i, armFrame);
                prev = armFrame;
            }
            full.put(i, armFrame);
        }

        for(int i = 0; i < NUM_LEDS; i++){

            ArrayList<Integer> ringColors = new ArrayList<>();

            for(int j = 0; j < SECTORS; j++){
                ringColors.add(full.get(j).get(i));
            }

            ring.put(i, ringColors);
        }
    }

    private void sampleDifferences() {

        copies.clear();

        for (int i = SECTORS - 1; i >= 0; i--) {
            if (converted.containsKey(i)) {

                HashMap<Integer, Integer> differences = null;

                ArrayList<Integer> checkI = converted.get(i);

                int minInd = -1;

                for (int j = i - 1; j >= 0; j--) {
                    if (converted.containsKey(j)) {
                        ArrayList<Integer> checkJ = converted.get(j);
                        HashMap<Integer, Integer> curr = new HashMap<>();
                        for (int k = 0; k < NUM_LEDS; k++) {
                            if (checkJ.get(k) != checkI.get(k)) {
                                curr.put(k, checkI.get(k));
                            }
                        }

                        if (differences == null || differences.size() > curr.size()) {
                            minInd = j;
                            differences = curr;
                        }
                    }
                }

                if (minInd >= 0)
                    copies.put(i, new Pair<>(minInd, differences));

            }
        }
    }

    private void compileSamples() {

        compiled.clear();

        for (int i = SECTORS - 1; i >= 0; i--) {
            if (converted.containsKey(i)) {

                int start = 0, current;

                ArrayList<Triple<Integer, Integer, Integer>> color = new ArrayList<>();

                ArrayList<Integer> checkI = converted.get(i);
                current = checkI.get(0);
                //System.out.print(i + " ");
                for (int j = 1; j <= checkI.size(); j++) {
                    if (j == checkI.size() || checkI.get(j) != current) {
                        //System.out.print("(" + current + ":" +  start + "=" + j + "),");
                        color.add(new Triple(start, j, current));

                        if (j != checkI.size()) {
                            current = checkI.get(j);
                            start = j;
                        }
                    }
                }
                //System.out.println();
                compiled.put(i, color);
            }
        }
    }

    public int sample(int x, int y){
        this.xPix = x;
        this.yPix = y;
        if(xPix >= 0 && yPix >= 0) {
            return result.getRGB(xPix, yPix);
        }
        return 0;
    }

    private void postDisplayUpdate() {
        colorMap.clear();
        for(int i = 0; i < SECTORS; i++){
            ArrayList<Integer> crgbs = this.displayed.get(i);
            colorMap.addAll(crgbs);
        }
        System.out.println("Unique Colors: " + colorMap.size());
    }

    public void showRadial(boolean ift, boolean real) {
        this.displayed = ift ? iringTransform : (real ? vrringTransform : viringTransform);
        this.postDisplayUpdate();
    }



    public void showAngular(boolean ift, boolean real) {
        this.displayed = ift ? itransform : (real ? vrtransform : vitransform);
        this.postDisplayUpdate();
    }

    public void showDerivative(boolean radial, boolean transform, boolean ift){
        this.displayed = !radial ? (ift ? iderRingTransform : (transform ? vrderringTransform : ringDerivative)) : (ift ? iderTransform : (transform ? vrderTransform : derivative));
        this.postDisplayUpdate();
    }

    public void showIntegration(boolean radial){
        this.displayed = radial ? integration : ringIntegration;
        this.postDisplayUpdate();
    }

    public void showDefaultSample() {
        this.displayed = full;
        this.postDisplayUpdate();
    }


    public String createCode(){
        StringBuilder code = new StringBuilder();
        code.append("BladeFrame *f1 = new BladeFrame();\n");

        int i = 0;

        for(int sector: converted.keySet()){
            code.append("ArmFrame *sector" + i + " = new ArmFrame(NUM_LEDS);\n");
            ArrayList<Integer> crgbs = converted.get(sector);
            for(int j = 0; j < crgbs.size(); j++){
                code.append("sector" + i + "->SetLED(" + j + ", CRGB(" + crgbs.get(j) + "));\n");
            }
            code.append("f1->AddArmFrame(sector" + i + ", " + ((double)sector/SECTORS * Math.PI * 2) + ");\n");
            i++;
        }

        return code.toString();
    }

    public void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(font);
        // Determine the X coordinate for the text
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        // Set the font
        g.setFont(font);
        // Draw the String
        g.drawString(text, x, y);
    }

    public void display(ESPControlServer server) {
        server.addRequest("stage-fr", FRAME_CLIENTS[0], SECTORS);
        for(int sector = 0; sector < SECTORS; sector++) {
            if (compiled.containsKey(sector)){

                byte id = FRAME_CLIENTS[sector % FRAME_CLIENTS.length];

                ArrayList<Triple<Integer, Integer, Integer>> crgb = compiled.get(sector);

                server.addRequest("stage-ar", id, sector);

                for (Triple<Integer, Integer, Integer> color : crgb) {
                    int ledStart = color.first();
                    int ledEnd = color.second();
                    if(ledEnd - ledStart > 1)
                        server.addRequest("leds", id, ledStart, ledEnd, color.third());
                    else
                        server.addRequest("led", id, ledStart, color.third());
                }

                server.addRequest("commit-ar", id, new ArrayList<Integer>());
            }
        }

        server.addRequest("commit-fr", FRAME_CLIENTS[0], new ArrayList<Integer>());
        //server.addRequest("anim", Main.DISPLAY_ID, 6);
    }
}
