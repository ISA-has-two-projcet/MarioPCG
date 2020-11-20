package marioPCGui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import cmatest.CMAMarioSolver;
import com.google.gson.Gson;
import viewer.MarioLevelPlayer;
import viewer.MarioLevelViewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ch.idsia.tools.ToolsConfigurator;

public class GuiLauncher {
    private static JTextArea logTextArea = null;
    private static JProgressBar progressBar = null;
    private static JLabel levelImgLabel = null;
    private static JButton playButton = null;
    private static JButton cmaButton = null;
    private static JFrame mainFrame = null;


    private static double[][] levelLatentVectors = null;


    private static int currentLoop;
    private static int currentEval;
    private static int maxLoop;
    private static int maxEval;

    //Redirect log to UI
    private static void updateTextPanel(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Document doc = logTextArea.getDocument();
                try {
                    doc.insertString(doc.getLength(), text, null);
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
                logTextArea.setCaretPosition(doc.getLength() - 1);
            }
        });
    }
    private static void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                updateTextPanel(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextPanel(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
    //Redirect log to UI

    static ArrayList<LatentVectorAndFit> getSortedLatentVecs(double[][] x, double[] y){
        ArrayList<LatentVectorAndFit> latentvecObjLst = new ArrayList<>();
        for(int i = 0;  i < x.length; i++) {
            LatentVectorAndFit latentVectorAndFit = new LatentVectorAndFit();
            latentVectorAndFit.mVector = x[i];
            latentVectorAndFit.fitness = y[i];
            latentvecObjLst.add(latentVectorAndFit);
        }
        latentvecObjLst.sort(new Comparator<LatentVectorAndFit>() {
            @Override
            public int compare(LatentVectorAndFit o1, LatentVectorAndFit o2) {
                return o1.fitness > o2.fitness? -1: 1;
            }
        });
        return  latentvecObjLst;
    }

    static class LatentVectorAndFit {
        public double[] mVector;
        public double fitness;
    }

    public static class CMAFinishCallBack{

        public void execute(double[][] bestX, double[] bestY) {

            ArrayList<LatentVectorAndFit> latentvecObjLst = getSortedLatentVecs(bestX, bestY);
            String vectorStr = "";
            double soertedVecs[][] = new double[latentvecObjLst.size()][];
            Gson gson = new Gson();
            for(int i = 0; i < latentvecObjLst.size(); i++) {
                soertedVecs[i] = latentvecObjLst.get(i).mVector;
            }
            levelLatentVectors = soertedVecs;
            vectorStr = gson.toJson(soertedVecs);
            try {
                MarioLevelViewer.runMarioLevelViewer(vectorStr, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateLevelImage();
            playButton.setEnabled(true);
            cmaButton.setEnabled(true);
        }

    }

    public static class PlayFinishCallBack{

        public void execute() {
            playButton.setEnabled(true);
            cmaButton.setEnabled(true);
        }

    }

    private static void placeButtonComponents(JPanel panel) {
        cmaButton = new JButton("CMA-ES");
        forceSetSize(cmaButton, 100, 50);

        panel.add(cmaButton);
        cmaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playButton.setEnabled(false);
                cmaButton.setEnabled(false);
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            CMAMarioSolver.invokeCMASolverFromUI(new CMAFinishCallBack());
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                });
                thread.start();

            }
        });

        playButton = new JButton("PLAY!");
        forceSetSize(playButton, 100, 50);
        panel.add(playButton);
        playButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                playButton.setEnabled(false);
                cmaButton.setEnabled(false);
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        Gson gson = new Gson();
                        try {
                            List<String> latentVecFileLines = Files.readAllLines(Paths.get("./ex_output.txt"));
                            double latentVecs[][] = new double[latentVecFileLines.size() - 1][];
                            for(int i = 0; i < latentVecFileLines.size() - 1; i++) {
                                latentVecs[i] = gson.fromJson(latentVecFileLines.get(i), double[].class);
                            }
                            double fitness[] = gson.fromJson(latentVecFileLines.get(latentVecFileLines.size() - 1), double[].class);

                            ArrayList<LatentVectorAndFit> latentvecObjLst = getSortedLatentVecs(latentVecs, fitness);
                            String vectorStr = "";
                            double soertedVecs[][] = new double[latentvecObjLst.size()][];
                            for(int i = 0; i < latentvecObjLst.size(); i++) {
                                soertedVecs[i] = latentvecObjLst.get(i).mVector;
                            }
                            MarioLevelPlayer.invokeLevelPlayerFromUI(gson.toJson(soertedVecs), true, new PlayFinishCallBack());

                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                });
                thread.start();

            }
        });
    }

    private static void placeInfoComponents(JPanel panel) {
        logTextArea=new JTextArea("",7,30);
        logTextArea.setLineWrap(true);
        JScrollPane logScrollTextArea=new JScrollPane(logTextArea);

        forceSetSize(logScrollTextArea, 500, 200);
        panel.add(logScrollTextArea);

        progressBar = new JProgressBar(0, 100);
        forceSetSize(progressBar, 500, 40);

        progressBar.setIndeterminate(true);

        panel.add(progressBar);
    }

    private static void placeBottomComponents(JPanel panel) {
        JPanel leftPanel = new JPanel();
        forceSetSize(leftPanel, 300, 300);

        leftPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 100, 10));
        placeButtonComponents(leftPanel);
        panel.add(leftPanel);

        JPanel rightPanel = new JPanel();
        forceSetSize(rightPanel, 600, 300);

        rightPanel.setLayout(new FlowLayout());
        placeInfoComponents(rightPanel);
        panel.add(rightPanel);
    }

    private static void placeTopComponents(JPanel panel) {
//        logTextArea=new JTextArea("",7,30);
//        logTextArea.setLineWrap(true);
//        JScrollPane logScrollTextArea=new JScrollPane(logTextArea);
//
//        logScrollTextArea.setSize(200, 200);
//        panel.add(logScrollTextArea);

        levelImgLabel = new JLabel();
        ImageIcon imgIcon = new ImageIcon("./logo.gif");
        levelImgLabel.setIcon(imgIcon);

        JScrollPane levelImageScroolArea = new JScrollPane(levelImgLabel);
        forceSetSize(levelImageScroolArea, 550, 300);

        panel.add(levelImageScroolArea);
    }

    public static void setProgress(int currentEval, int maxEval, int currentLoop, int maxLoop)  {
        if(progressBar == null) return;
        if(progressBar.isIndeterminate() == true){
            progressBar.setMinimum(0);
            progressBar.setMaximum(maxEval * maxLoop);
            progressBar.setValue(0);
            progressBar.setIndeterminate(false);
            GuiLauncher.currentEval = currentEval;
            GuiLauncher.currentLoop = currentLoop;
            GuiLauncher.maxEval = maxEval;
            GuiLauncher.maxLoop = maxLoop;
        }
        else {
            GuiLauncher.currentEval = currentEval;
            GuiLauncher.currentLoop = currentLoop;
            progressBar.setValue(GuiLauncher.currentEval + GuiLauncher.currentLoop * GuiLauncher.maxEval);

        }

    }

    public static void updateLevelImage(){
        ImageIcon imgIcon = new ImageIcon("./LevelFull.png");
        //Image img = imgIcon.getImage();
        //Image scaledImg = img.getScaledInstance(900, 330, Image.SCALE_SMOOTH);
        //levelImgLabel.setIcon(new ImageIcon(scaledImg));
        levelImgLabel.setIcon(imgIcon);
    }

    public static void setProgress(int currentEval)  {
        if(progressBar == null) return;
        GuiLauncher.currentEval = currentEval;
        progressBar.setValue(GuiLauncher.currentEval + GuiLauncher.currentLoop * GuiLauncher.maxEval);
    }

    private static void createAndShowGUI() {

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        mainFrame = new JFrame("Mario PCG");
        mainFrame.setSize(900, 600);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setLayout(new FlowLayout(FlowLayout.LEADING));

        JPanel topPanel = new JPanel();
        forceSetSize(topPanel, 900, 300);
        mainFrame.add(topPanel);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        placeTopComponents(topPanel);

        JPanel bottomPanel = new JPanel();
        forceSetSize(bottomPanel, 900, 300);
        mainFrame.add(bottomPanel);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        placeBottomComponents(bottomPanel);
        mainFrame.setVisible(true);
        mainFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                super.componentMoved(e);
                setMarioFrameFollowMain();
            }
        });

        mainFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent wEvt) {
                setMarioFrameFollowMain();
            }

            @Override
            public void windowDeactivated(WindowEvent wEvt) {
                setMarioFrameFollowMain();
            }

        });
    }

    public static void setMarioFrameFollowMain(){
        if(ToolsConfigurator.marioComponentFrame != null) {
            ToolsConfigurator.marioComponentFrame.setLocation(mainFrame.getLocation().x + 560, mainFrame.getLocation().y + 30);
            ToolsConfigurator.marioComponentFrame.toFront();
        }
    }

    public static void forceSetSize(JComponent component, int width, int height) {
        component.setPreferredSize(new Dimension(width, height));
        component.setMaximumSize(new Dimension(width, height));
    }

    public static void main(String[] args) throws IOException {

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                redirectSystemStreams();
                createAndShowGUI();

            }
        });
    }

}
