package cmatest;

import basicMap.Settings;
import ch.idsia.mario.engine.level.Level;
import ch.idsia.mario.engine.level.LevelParser;
import ch.idsia.mario.engine.sprites.Enemy;
import reader.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static reader.JsonReader.JsonToDoubleArray;

public class LevelStatistics {


    /* For testing purposes -- arg parsing copied from MarioLevelPlayer */
    public static void main(String[] args) throws IOException {
        Settings.setPythonProgram();
        // This is used because it contains code for communicating with the GAN
        MarioEvalFunctionImproved eval = new MarioEvalFunctionImproved();

        Level level;
        // Read input level
        String strLatentVector = "[[0.7223726486256712, -0.8144046463058702, -0.581285311878616, -0.281361878750429, -0.030582126682515975, 0.800782289641333, -0.008041576280385355, 0.9713680344969697, 0.27716128531508394, 0.2079059151380096, -0.8803378210802248, -0.7473451230553672, -0.2388377040116749, -0.12542953153461953, -0.3083731522514313, 0.1531946157041087, 0.8359040212591892, 0.13615080957527675, 0.044527185376987174, -0.8484191188631249, 0.835405692406362, 0.7645986800437078, -0.2040729470895779, 0.4662541740756924, 0.18993431141898137, -0.4121439322558321, -1.1069273558973947, -0.9138044261807096, -0.5372303974722598, -0.38982685080834667, 0.8900294226441797, -0.3146479973878492], [0.6273941395707298, -0.9427500090478498, -0.5290874882311567, 0.852353248733728, 0.14645202803438054, -1.108995213701999, 0.2550097127073096, 0.6851574109321987, 0.7428163820105644, 0.6511184458635828, -0.3201412289656914, -0.545551788916962, 0.49859294331285875, 0.4675868229106976, -0.802305109173559, 0.539902671356367, 1.4448418064525828, 1.765386314148962, 1.0090532316214686, 0.09945767778801401, -0.3562897260838752, -0.2891259372194832, 0.6772946320165032, 1.159098080696282, 0.8313164684773304, 0.5251534615202216, 0.729715907681231, -0.558702402625405, 1.3891140811189535, 1.1936680173083096, -0.9645974904199145, -1.044879475850995]]";
        if (true) {
//            StringBuilder builder = new StringBuilder();
//            for (String str : args) {
//                builder.append(str);
//            }
//            strLatentVector = builder.toString();
//            Settings.printInfoMsg("Passed vector(s): " + strLatentVector);
            // If the input starts with two square brackets, then it must be an array of arrays,
            // and hence a series of several latent vectors rather than just one. In this case,
            // patch all of the levels together into one long level.
            if(strLatentVector.subSequence(0, 2).equals("[[")) {
                // remove opening/closing brackets
                strLatentVector = strLatentVector.substring(1,strLatentVector.length()-1);
                String levels = "";
                while(strLatentVector.length() > 0) {
                    int end = strLatentVector.indexOf("]")+1;
                    String oneVector = strLatentVector.substring(0,end);
                    System.out.println("ONE VECTOR: " + oneVector);
                    levels += eval.stringToFromGAN(oneVector); // Use the GAN
                    strLatentVector = strLatentVector.substring(end); // discard processed vector
                    if(strLatentVector.length() > 0) {
                        levels += ",";
                        strLatentVector = strLatentVector.substring(1); // discard leading comma
                    }
                }
                levels = "["+levels+"]"; // Put back in brackets
                System.out.println(levels);
                List<List<List<Integer>>> allLevels = JsonReader.JsonToInt(levels);
                // This list contains several separate levels. The following code
                // merges the levels by appending adjacent rows
                ArrayList<List<Integer>> oneLevel = new ArrayList<List<Integer>>();
                // Create the appropriate number of rows in the array
                for(List<Integer> row : allLevels.get(0)) { // Look at first level (assume all are same size)
                    oneLevel.add(new ArrayList<Integer>()); // Empty row
                }
                // Now fill up the rows, one level at a time
                for(List<List<Integer>> aLevel : allLevels) {
                    int index = 0;
                    for(List<Integer> row : aLevel) { // Loot at each row
                        oneLevel.get(index++).addAll(row);
                    }
                }
                // Now create the Mario level from the combined list representation
                level = LevelParser.createLevelJson(oneLevel);
            } else { // Otherwise, there must be a single latent vector, and thus a single level
                double[] latentVector = JsonToDoubleArray(strLatentVector);
                level = eval.levelFromLatentVector(latentVector);
            }
        } else {
            System.out.println("Generating level with default vector");
            level = eval.levelFromLatentVector(new double[] {0.9881835842209917, -0.9986077315374948, 0.9995512051242508, 0.9998643432807639, -0.9976165917284504, -0.9995247114230822, -0.9997001909358728, 0.9995694511739592, -0.9431036754879115, 0.9998155541290887, 0.9997863689962382, -0.8761392912669269, -0.999843833016589, 0.9993230720045649, 0.9995470247917402, -0.9998847606084427, -0.9998322053148382, 0.9997707200294411, -0.9998905141832997, -0.9999512510490688, -0.9533512808031753, 0.9997703088007039, -0.9992229823819915, 0.9953917828622341, 0.9973473366437476, 0.9943030781608361, 0.9995290290713732, -0.9994945079679955, 0.9997109900652238, -0.9988379572928884, 0.9995070647543864, 0.9994132207570211});
        }

        LevelStatistics levelStats = new LevelStatistics(level);
        System.out.println("Broken Pipe Tiles: " + levelStats.numBrokenPipeTiles);
        System.out.println("numGroundRocks:" + levelStats.numGroundRocks);
        System.out.println("Stuck: " + levelStats.numStuckEnemy);
        System.out.println("Goombas: " + levelStats.numGoombas);
        System.out.println(levelStats.gapFitness());


        eval.exit();
        System.exit(0);
    }

    /* Constants used by Level class to represent blocks */
    private static final int EMPTY_SPACE = 0;
    private static final int GROUND_ROCK = 9;
    private static final int TOP_LEFT_PIPE = 10;
    private static final int TOP_RIGHT_PIPE = 11;
    private static final int LEFT_PIPE = 26;
    private static final int RIGHT_PIPE = 27;

    private static final int MagicNumberUndef = -42;

    private Level level;

    /* Total number of pipe tiles */
    public int numPipeTiles = MagicNumberUndef;
    /* Number of pipe tiles in level that are part of a valid pipe */
    public int numValidPipeTiles = MagicNumberUndef;
    /* Number of pipe tiles in level that aren't part of a valid pipe */
    public int numBrokenPipeTiles = MagicNumberUndef;
    /* Total number of ground rocks (including those not on ground level) */
    public int numRocks = MagicNumberUndef;
    /* Number of rocks on ground level */
    public int numGroundRocks = MagicNumberUndef;
    /* Number of empty spaces on ground level */
    public int numGaps = MagicNumberUndef;
    /* Number of goombas */
    public int numGoombas = MagicNumberUndef;
    /* Number of stuck enemy */
    public int numStuckEnemy = MagicNumberUndef;


    public LevelStatistics(Level level) {
        this.level = level;
        numPipeTiles = 0;
        numBrokenPipeTiles = 0;
        numValidPipeTiles = 0;
        numRocks = 0;
        numGroundRocks = 0;
        numGaps = 0;
        numGoombas = 0;
        numStuckEnemy = 0;
        processLevel();
    }

    public UnaryOperator<Double> optTransform(double opt, double mult) {
        return (x) -> opt - mult * Math.abs(opt - x);
    }

    public double gapFitness() {
        // List of all gap lengths
        ArrayList<Double> gaps = new ArrayList<>();
        int gapStart = level.width;
        for (int xCurr = 0; xCurr < level.width; xCurr++) {
            int tile = level.getBlock(xCurr, level.height - 1);
            if (tile == GROUND_ROCK) {
                // We're at the end of a gap
                if (gapStart < xCurr - 1) {
                    gaps.add((double) xCurr - gapStart - 1);
                    System.out.println("======================gap size: " + (xCurr - gapStart + 1));
                }
                // gapStart tracks xCarr as long as tile is rock
                gapStart = xCurr;
            }
        }
        gaps.replaceAll(optTransform(3, 1));
        return optTransform(7, 1.5).apply(gaps.stream().mapToDouble(a -> a).sum());
    }

    /* Private helper functions */

    private boolean isPipeTile(int x, int y) {
        int tile = level.getBlock(x, y);
        return tile == TOP_LEFT_PIPE || tile == TOP_RIGHT_PIPE
                || tile == LEFT_PIPE || tile == RIGHT_PIPE;
    }

    private boolean isValidPipe(int xLeft, int yTop) {
        int tileTopLeft = level.getBlock(xLeft, yTop);
        int tileTopRight = level.getBlock(xLeft + 1, yTop);
        if (tileTopLeft != TOP_LEFT_PIPE || tileTopRight != TOP_RIGHT_PIPE)
            return false;


        int y = yTop + 1;
        while (y < level.height && level.getBlock(xLeft, y) == LEFT_PIPE
                && level.getBlock(xLeft + 1, y) == RIGHT_PIPE) y++;
        if (y == yTop + 1) return false;

        return level.getBlock(xLeft, y) == GROUND_ROCK
                && level.getBlock(xLeft + 1, y) == GROUND_ROCK;
    }

    private boolean containedInValidPipe(int x, int y) {
        for (int y_i = y; y_i >=0; y_i--) {
            if (isValidPipe(x - 1, y_i) || isValidPipe(x, y_i)) return true;
        }
        return false;
    }

    private void processTile(int x, int y) {
        if (isPipeTile(x, y)) {
            numPipeTiles++;
            if (containedInValidPipe(x, y)) numValidPipeTiles++;
            else numBrokenPipeTiles++;
        }
        if (level.getBlock(x, y) == GROUND_ROCK) {
            numRocks++;
            if (y == level.height - 1) numGroundRocks++;
        }
        if (y == level.height - 1 && level.getBlock(x, y) == EMPTY_SPACE)
            numGaps++;
        if (level.getSpriteTemplate(x, y) != null && level.getSpriteTemplate(x, y).getType() == Enemy.ENEMY_GOOMBA)
            numGoombas++;
        if ( level.getSpriteTemplate(x, y) != null &&
                (level.getSpriteTemplate(x, y).getType() == Enemy.ENEMY_GOOMBA
                || level.getSpriteTemplate(x, y).getType() == Enemy.ENEMY_GREEN_KOOPA
                || level.getSpriteTemplate(x, y).getType() == Enemy.ENEMY_RED_KOOPA) && checkStuckEnemy(x, y)) {
            numStuckEnemy++;
        }
    }

    private boolean checkStuckEnemy(int x, int y) {
        if(level.getBlock(x, y) != EMPTY_SPACE) {
            return true;
        }

//        if(x - 1 >= 0 && level.getBlock(x - 1 , y) != EMPTY_SPACE
//            && x + 1 < level.width && level.getBlock(x + 1, y) != EMPTY_SPACE) {
//            return true;
//        }
//
//        if(y - 1 >= 0 && level.getBlock(x, y - 1) != EMPTY_SPACE) {
//            return true;
//        }

        return false;
    }

    private void processLevel() {
        for (int x = 0; x < level.width; x++) {
            for (int y = 0; y < level.height; y++) {
                processTile(x, y);
            }
        }

    }
}
