import server.ESPControlServer;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

/**
 Simple harness for testing GUI code.

 <P>To use this class, edit the code to suit your needs.  
 */
public final class Main {

    public static final byte DISPLAY_ID = 0x01;
    public static final int STREAM_INDEX = 6;

    private JFrame frame;

    private SectoredCircle circle;

    private ESPControlServer server;
    private Lock serverLock;

    boolean started = false;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Main window = new Main();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public Main() {

        startServer();
        initialize();
    }

    private void startServer(){
        this.serverLock = new ReentrantLock();
        try{
            server = new ESPControlServer();

            Thread service = new Thread(server);
            service.start();

            Thread readService = new Thread(() -> {
                System.out.println("Starting Read Service");
                while(true) {
                    serverLock.lock();
                    String result = server.readResult(DISPLAY_ID);
                    serverLock.unlock();
                    if(result != null)
                        System.out.println(result);
                }
            });
            readService.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 1250, 950);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setResizable(false);

        circle = new SectoredCircle();

        circle.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                circle.sample(e.getX(), e.getY());
                circle.repaint();
            }
        });

        circle.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StringSelection selection = new StringSelection(circle.sample(e.getX(), e.getY()) + "");
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                circle.generateImage();
                circle.sample(e.getX(), e.getY());
                circle.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                circle.sample(-1, -1);
                circle.repaint();
            }
        });

        circle.setBounds(50, 50, SectoredCircle.PANEL_WIDTH, SectoredCircle.PANEL_HEIGHT);
        frame.getContentPane().add(circle);

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(new ImageFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);

            int option = fileChooser.showOpenDialog(frame);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                loadButton.setText(file.getName());
                try {
                    circle.setImage(ImageIO.read(file));
                    circle.repaint();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }else{
                loadButton.setText("Load");
            }
        });

        loadButton.setBounds(1000, 0, SectoredCircle.PANEL_WIDTH / 4, 50);
        frame.getContentPane().add(loadButton);

        JButton sampleButton = new JButton("Toggle Sampling");
        sampleButton.addActionListener(e -> {

            circle.sample();
            circle.toggleSampleView();
            circle.repaint();

        });

        sampleButton.setBounds(1000, 60, SectoredCircle.PANEL_WIDTH / 4, 50);
        frame.getContentPane().add(sampleButton);

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> {

            circle.sample();

            StringSelection selection = new StringSelection(circle.createCode());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });

        exportButton.setBounds(1000, 120, SectoredCircle.PANEL_WIDTH / 4, 50);
        frame.getContentPane().add(exportButton);

        JButton toggleStart = new JButton("Start");
        toggleStart.addActionListener(e -> {
            this.serverLock.lock();
            server.addRequest(started ? "stop" : "start", Main.DISPLAY_ID, new ArrayList<Integer>());
            this.serverLock.unlock();
            started = !started;
            toggleStart.setText(started ? "Stop" : "Start");
        });

        toggleStart.setBounds(1000, 180, SectoredCircle.PANEL_WIDTH / 4, 50);
        frame.getContentPane().add(toggleStart);


        JButton displayButton = new JButton("Display");
        displayButton.addActionListener(e -> {

            circle.sample();
            this.serverLock.lock();
            server.addRequest("anim", Main.DISPLAY_ID, 3);
            circle.display(server);
            this.serverLock.unlock();

        });

        displayButton.setBounds(1000, 240, SectoredCircle.PANEL_WIDTH / 4, 50);
        frame.getContentPane().add(displayButton);

        JButton random = new JButton("Random");
        random.addActionListener(e -> {

            this.serverLock.lock();
            server.addRequest("anim", Main.DISPLAY_ID, (int) (Math.random() * 6));
            this.serverLock.unlock();

        });

        random.setBounds(1000, 750, SectoredCircle.PANEL_WIDTH / 4, 50);
        frame.getContentPane().add(random);

        JButton uploaded = new JButton("Uploaded Animation");
        uploaded.addActionListener(e -> {

            this.serverLock.lock();
            server.addRequest("anim", Main.DISPLAY_ID, 6);
            this.serverLock.unlock();

        });

        uploaded.setBounds(1000, 810, SectoredCircle.PANEL_WIDTH / 4, 50);
        frame.getContentPane().add(uploaded);


        JLabel xlabel, ylabel, scalelabel, multlabel, throttlelabel;

        xlabel = new JLabel("X Offset: ");
        ylabel = new JLabel("Y Offset: ");
        scalelabel = new JLabel("Scale: ");
        multlabel = new JLabel("Multiplier: ");
        throttlelabel = new JLabel("Throttle: ");


        JSlider xSlider = new JSlider(JSlider.HORIZONTAL,-SectoredCircle.PANEL_WIDTH, SectoredCircle.PANEL_WIDTH, 0);
        JSlider ySlider = new JSlider(JSlider.VERTICAL,-SectoredCircle.PANEL_HEIGHT, SectoredCircle.PANEL_HEIGHT, 0);
        JSlider scaleSlider = new JSlider(JSlider.VERTICAL, -500, 500, 0);
        JSlider multSlider = new JSlider(JSlider.VERTICAL, 700, 2000, 1000);
        JSlider throttleSlider = new JSlider(JSlider.VERTICAL, 1250, 1400, 1300);

        xSlider.setBorder(BorderFactory.createTitledBorder("X Offset"));
        xSlider.setMinorTickSpacing(25);
        xSlider.setMajorTickSpacing(100);
        xSlider.setPaintTicks(true);

        ySlider.setBorder(BorderFactory.createTitledBorder("Y Offset"));
        ySlider.setMinorTickSpacing(25);
        ySlider.setMajorTickSpacing(100);
        ySlider.setPaintTicks(true);

        scaleSlider.setBorder(BorderFactory.createTitledBorder("Scale"));
        scaleSlider.setMinorTickSpacing(25);
        scaleSlider.setMajorTickSpacing(100);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);

        multSlider.setBorder(BorderFactory.createTitledBorder("Multiplier"));
        multSlider.setMinorTickSpacing(25);
        multSlider.setMajorTickSpacing(100);
        multSlider.setPaintTicks(true);
        multSlider.setPaintLabels(true);

        throttleSlider.setBorder(BorderFactory.createTitledBorder("Throttle"));
        throttleSlider.setMinorTickSpacing(1);
        throttleSlider.setMajorTickSpacing(10);
        throttleSlider.setPaintTicks(true);
        throttleSlider.setPaintLabels(true);


        xSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                circle.setXoffset(xSlider.getValue());
                xlabel.setText("X Offset: " + xSlider.getValue());
                circle.repaint();
            }
        });

        ySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                circle.setYoffset(-ySlider.getValue());
                ylabel.setText("Y Offset: " + ySlider.getValue());
                circle.repaint();
            }
        });

        scaleSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                circle.setScaling(scaleSlider.getValue());
                scalelabel.setText("Scale: " + scaleSlider.getValue());
                circle.repaint();
            }
        });

        multSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                serverLock.lock();
                server.addRequest("mult", Main.DISPLAY_ID, multSlider.getValue());
                serverLock.unlock();
                multlabel.setText("Multiplier: " + multSlider.getValue());
            }
        });

        throttleSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                serverLock.lock();
                server.addRequest("throttle", Main.DISPLAY_ID, throttleSlider.getValue());
                serverLock.unlock();
                throttlelabel.setText("Throttle: " + throttleSlider.getValue());
            }
        });

        xSlider.setBounds(50, 0, SectoredCircle.PANEL_WIDTH, 40);
        ySlider.setBounds(SectoredCircle.PANEL_WIDTH + 50, 50, 70, SectoredCircle.PANEL_HEIGHT);
        scaleSlider.setBounds(SectoredCircle.PANEL_WIDTH + 120, 50, 70, SectoredCircle.PANEL_HEIGHT);
        multSlider.setBounds(1000, 300, 70, 400);
        throttleSlider.setBounds(1130, 300, 70, 400);

        xlabel.setBounds(50, 850, 100, 50);
        ylabel.setBounds(200, 850, 100, 50);
        scalelabel.setBounds(350, 850, 100, 50);
        multlabel.setBounds(500, 850, 100, 50);
        throttlelabel.setBounds(650, 850, 100, 50);

        frame.getContentPane().add(xSlider);
        frame.getContentPane().add(ySlider);
        frame.getContentPane().add(scaleSlider);
        frame.getContentPane().add(multSlider);
        frame.getContentPane().add(throttleSlider);

        frame.getContentPane().add(xlabel);
        frame.getContentPane().add(ylabel);
        frame.getContentPane().add(scalelabel);
        frame.getContentPane().add(multlabel);
        frame.getContentPane().add(throttlelabel);

    }

    class ImageFilter extends FileFilter {
        public final static String JPEG = "jpeg";
        public final static String JPG = "jpg";
        public final static String GIF = "gif";
        public final static String TIFF = "tiff";
        public final static String TIF = "tif";
        public final static String PNG = "png";

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String extension = getExtension(f);
            if (extension != null) {
                if (extension.equals(TIFF) ||
                        extension.equals(TIF) ||
                        extension.equals(GIF) ||
                        extension.equals(JPEG) ||
                        extension.equals(JPG) ||
                        extension.equals(PNG)) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Image Only";
        }

        String getExtension(File f) {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 &&  i < s.length() - 1) {
                ext = s.substring(i+1).toLowerCase();
            }
            return ext;
        }
    }
}