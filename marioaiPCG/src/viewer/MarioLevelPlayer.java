package viewer;

import static reader.JsonReader.JsonToDoubleArray;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import basicMap.Settings;
import ch.idsia.ai.agents.human.HumanKeyboardAgent;
import ch.idsia.mario.engine.level.Level;
import ch.idsia.mario.engine.level.LevelParser;
import ch.idsia.tools.ToolsConfigurator;
import cmatest.MarioEvalFunction;
import com.google.gson.Gson;
import communication.MarioProcess;
import competition.icegic.robin.AStarAgent;
import marioPCGui.GuiLauncher;
import reader.JsonReader;

/**
 * This file allows you to generate a level and play it for any latent vector
 * or your choice. The vector must have a length of 32 numbers separated
 * by commas enclosed in square brackets [ ]. For example,
 * [0.9881835842209917, -0.9986077315374948, 0.9995512051242508, 0.9998643432807639, -0.9976165917284504, -0.9995247114230822, -0.9997001909358728, 0.9995694511739592, -0.9431036754879115, 0.9998155541290887, 0.9997863689962382, -0.8761392912669269, -0.999843833016589, 0.9993230720045649, 0.9995470247917402, -0.9998847606084427, -0.9998322053148382, 0.9997707200294411, -0.9998905141832997, -0.9999512510490688, -0.9533512808031753, 0.9997703088007039, -0.9992229823819915, 0.9953917828622341, 0.9973473366437476, 0.9943030781608361, 0.9995290290713732, -0.9994945079679955, 0.9997109900652238, -0.9988379572928884, 0.9995070647543864, 0.9994132207570211]
 * 
 * Additionally, if you send in a 2D array where each sub-array has length 32, this will be interpreted as several
 * latent vectors. Each latent vector will create a separate level segment, and the segments will be stitched together
 * into one level for you to play.
 * 
 */
public class MarioLevelPlayer {

	public static final int BLOCK_SIZE = 16;
	public static final int LEVEL_HEIGHT = 14;

	public static GuiLauncher.PlayFinishCallBack finishCallBack = null;


	public static void main(String[] args) throws IOException {
		runMarioLevelPlayer("[[0.37855129001686233, 0.3429553501447944, 1.1191979199065958, -1.9945258498705476, 0.8003953822186769, -0.48463372434393615, -0.49186990832291055, -1.0289544737355159, -1.4054700932645754, 0.5289802698814884, -1.1127875453237743, 0.20517924168572074, 0.12170267761816436, 0.11202939458137268, -0.7036358812829535, -0.38338393848026664, -0.6544392412149584, -1.4319057251694087, 0.5784921793770716, 1.7217219542714794, -0.6633160706784128, 0.2450562770186566, 0.01137704348470811, 0.9158674355294263, -0.926799598696636, -2.253563851408587, 1.1919884386449564, 0.5987469594419272, 1.4565416589987321, -0.6711315768047172, -1.0170250880153562, -0.09472008873394422], [-0.6028637376437607, 1.3121435594362283, 1.5621553598406932, -0.8868378418707203, 1.3313259833754776, -0.21259417201577752, 0.8973043678894235, 2.126300625870445, -0.41979577390481265, 1.6233552615969726, 0.4215065587107957, -0.9112944075764653, 0.69029797129477, -2.2115621918404376, -1.3293190391369991, -1.2095644255288363, -0.540058332859831, -2.121502576805296, 1.125666213352298, -1.1031444181678791, 1.0845127757902786, 1.0801549934977266, 0.6789204977436121, -0.37601405698475115, -1.8930325358980202, 1.5087582482008484, 1.570622766027442, -1.147543009306691, -0.611612903024582, -0.7139511224668209, 1.1330570776314173, -0.22312554172707694]]"
				, true);
		//runMarioLevelPlayer("[0.37855129001686233, 0.3429553501447944, 1.1191979199065958, -1.9945258498705476, 0.8003953822186769, -0.48463372434393615, -0.49186990832291055, -1.0289544737355159, -1.4054700932645754, 0.5289802698814884, -1.1127875453237743, 0.20517924168572074, 0.12170267761816436, 0.11202939458137268, -0.7036358812829535, -0.38338393848026664, -0.6544392412149584, -1.4319057251694087, 0.5784921793770716, 1.7217219542714794, -0.6633160706784128, 0.2450562770186566, 0.01137704348470811, 0.9158674355294263, -0.926799598696636, -2.253563851408587, 1.1919884386449564, 0.5987469594419272, 1.4565416589987321, -0.6711315768047172, -1.0170250880153562, -0.09472008873394422]",
		//		false);
		System.exit(0);
	}

	public static void invokeLevelPlayerFromUI(String latentVectors, boolean multiVector, GuiLauncher.PlayFinishCallBack callback) {
		finishCallBack = callback;
		try {
			runMarioLevelPlayer(latentVectors, multiVector);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//If multiVector == true, then use multiple latent vector to form a longer level
	public static void runMarioLevelPlayer(String latentVectors, boolean multiVector) throws IOException {
		Settings.setPythonProgram();
		// This is used because it contains code for communicating with the GAN
		String GanPath = "./pytorch/netG_epoch_5000.pth";
		MarioEvalFunction eval = new MarioEvalFunction(GanPath, "32", 27, new AStarAgent());
		Level level = null;

		Settings.printInfoMsg("Passed vector(s): " + latentVectors);

		if (multiVector) {
			Gson gson = new Gson();
			ArrayList<List<Double>> inputVectors = new ArrayList();
			inputVectors = gson.fromJson(latentVectors, inputVectors.getClass());

			String levels = "";
			for(int i = 0; i < inputVectors.size(); i++) {
				String oneVector = inputVectors.get(i).toString();
				System.out.println("ONE VECTOR: " + oneVector);
				levels += eval.stringToFromGAN(oneVector); // Use the GAN
				if(i < inputVectors.size() -1)
					levels += ',';
			}
			levels = "[" + levels + "]"; // Put back in brackets
			System.out.println(levels);
			List<List<List<Integer>>> allLevels = JsonReader.JsonToInt(levels);
			// This list contains several separate levels. The following code
			// merges the levels by appending adjacent rows
			ArrayList<List<Integer>> oneLevel = new ArrayList<List<Integer>>();
			// Create the appropriate number of rows in the array
			for (List<Integer> row : allLevels.get(0)) { // Look at first level (assume all are same size)
				oneLevel.add(new ArrayList<Integer>()); // Empty row
			}
			// Now fill up the rows, one level at a time
			for (List<List<Integer>> aLevel : allLevels) {
				int index = 0;
				for (List<Integer> row : aLevel) { // Loot at each row
					oneLevel.get(index++).addAll(row);
				}
			}
			// Now create the Mario level from the combined list representation
			level = LevelParser.createLevelJson(oneLevel);
		} else { // Otherwise, there must be a single latent vector, and thus a single level
			double[] latentVector = JsonToDoubleArray(latentVectors);
			level = eval.levelFromLatentVector(latentVector);
		}


		MarioProcess marioProcess = new MarioProcess();
		String[] arguments = new String[0];
		marioProcess.launchMario(arguments, new HumanKeyboardAgent()); // true means there is a human player
		ToolsConfigurator.marioComponentFrame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				try {
					eval.getGanProcess().commSend("0");
					eval.getMarioProcess().interrupt();
					ToolsConfigurator.marioComponentFrame = null;

				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
				if(finishCallBack != null) {
					finishCallBack.execute();
					finishCallBack = null;
				}
			}
		});
		System.out.println(marioProcess.simulateOneLevel(level));
		if(finishCallBack != null) {
			finishCallBack.execute();
			finishCallBack = null;
		}
		
		eval.exit();

	}
}
