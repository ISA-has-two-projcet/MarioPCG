package cmatest;

import ch.idsia.mario.engine.level.Level;
import ch.idsia.mario.engine.level.LevelParser;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.ToolsConfigurator;
import communication.GANProcess;
import communication.MarioProcess;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import reader.JsonReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static reader.JsonReader.JsonToDoubleArray;

public class MarioEvalFunctionImproved implements IObjectiveFunction {

	// This is the distance that Mario traverses when he beats the short levels
	// that we are generating. It would need to be changed if we train on larger
	// levels or in any way change the level length.
	public static final int LEVEL_LENGTH = 704;

	private GANProcess ganProcess;
	private MarioProcess marioProcess;

	// changing floor will change the reason for termination
	// (in conjunction with the target value)
	// see cma.options.stopFitness
	static double floor = 0.0;

	public MarioEvalFunctionImproved() throws IOException {
		// set up process for GAN
		ganProcess = new GANProcess();
		ganProcess.start();
		// set up mario game
		marioProcess = new MarioProcess();
		marioProcess.start();
		// consume all start-up messages that are not data responses
		String response = "";
		while(!response.equals("READY")) {
			response = ganProcess.commRecv();
		}
	}

        public MarioEvalFunctionImproved(String GANPath, String GANDim) throws IOException {
		// set up process for GAN
		ganProcess = new GANProcess(GANPath, GANDim);
		ganProcess.start();
		// set up mario game
		marioProcess = new MarioProcess();
		marioProcess.start();        
		// consume all start-up messages that are not data responses
		String response = "";
		while(!response.equals("READY")) {
			response = ganProcess.commRecv();
		}
	}

	/**
	 * Takes a json String representing several levels 
	 * and returns an array of all of those Mario levels.
	 * In order to convert a single level, it needs to be put into
	 * a json array by adding extra square brackets [ ] around it.
	 * @param json Json String representation of multiple Mario levels
	 * @return Array of those levels
	 */
	public static Level[] marioLevelsFromJson(String json) {
		List<List<List<Integer>>> allLevels = JsonReader.JsonToInt(json);
		Level[] result = new Level[allLevels.size()];
		int index = 0;
		for(List<List<Integer>> listRepresentation : allLevels) {
			result[index++] = LevelParser.createLevelJson(listRepresentation);
		}
		return result;
	}
        
        public void exit() throws IOException{
            ganProcess.commSend("0");
            marioProcess.stopProcess();
        }

	/**
	 * Helper method to get the Mario Level from the latent vector
	 * @param x Latent vector
	 * @return Mario Level
	 * @throws IOException Problems communicating with Python GAN process
	 */
	public Level levelFromLatentVector(double[] x) throws IOException {
		x = mapArrayToOne(x);
		// Interpret x to a level
		// Brackets required since generator.py expects of list of multiple levels, though only one is being sent here
		ganProcess.commSend("[" + Arrays.toString(x) + "]");
		String levelString = ganProcess.commRecv(); // Response to command just sent
		Level[] levels = marioLevelsFromJson("[" +levelString + "]"); // Really only one level in this array
		Level level = levels[0];
		return level;
	}
	
	/**
	 * Directly send a string to the GAN (Should be array of arrays of doubles in Json format).
	 * 
	 * Note: A bit redundant: This could be called from the method above.
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public String stringToFromGAN(String input) throws IOException {
                double[] x = JsonToDoubleArray(input);
                x = mapArrayToOne(x);
		ganProcess.commSend(Arrays.toString(x));
		String levelString = ganProcess.commRecv(); // Response to com	mand just sent
		return levelString;
	}

	/**
	 * Gets objective score for single latent vector.
	 */
	@Override
	public double valueOf(double[] x) {
		try {
			Level level = levelFromLatentVector(x);
			// Do a simulation
			EvaluationInfo info = this.marioProcess.simulateOneLevel(level);
			LevelStatistics levelStats = new LevelStatistics(level);
			System.out.println("Broken Pipe Tiles: " + levelStats.numBrokenPipeTiles);
			// Fitness is negative since CMA-ES tries to minimize
                        //System.out.println("done");
                        //System.out.println(info.jumpActionsPerformed);
                        //System.out.println(info.computeJumpFraction());
			double penalty = 0;
			if(info.computeDistancePassed() < LEVEL_LENGTH) { // Did not beat level
				// Only optimize distance passed in this case
				penalty = 1 - info.computeDistancePassed() / LEVEL_LENGTH;
				if (info.marioRanOutOfTime) {
					penalty *= 100;
				}
				else if (info.marioDiedToFall) {
					penalty *= 50;
				}
				else if (info.marioDiedToEnemy) {
					penalty *= 20;
				}
				else {
					throw new RuntimeException("If mario failed, it should have been due to running out of time," +
												" running into an enemy, or falling into a hole!");
				}
			}
			// Did beat level
			//System.out.println("Beat level!");
							//System.out.println(info.computeJumpFraction());
			// Also maximize time, since this would imply the level is more challenging/interesting
			//return -info.computeDistancePassed() - info.timeSpentOnLevel;
			System.out.println("Goombas: " + levelStats.numGoombas);
			return  - info.computeDistancePassed() / LEVEL_LENGTH
					- EvaluationInfo.hardJumpWeight * info.hardJumpActionsPerformed
					- EvaluationInfo.mediumJumpWeight * info.mediumJumpActionsPerformed
					- EvaluationInfo.easyJumpWeight * info.easyJumpActionsPerformed
					- EvaluationInfo.trivialJumpWeight * info.trivialJumpActionsPerformed
					- levelStats.numBrokenPipeTiles * EvaluationInfo.badPipeTileWeight
					- levelStats.numValidPipeTiles * EvaluationInfo.goodPipeTileWeight
					- levelStats.gapFitness() * EvaluationInfo.gapWeight
					//- levelStats.optTransform(2, 1.5).apply((double) levelStats.numGoombas) * EvaluationInfo.enemyWeight
					- (levelStats.numGoombas - levelStats.numStuckEnemy) * EvaluationInfo.enemyWeight
					- levelStats.numStuckEnemy * EvaluationInfo.stuckEnemyWeight
					+ penalty;

		} catch (IOException e) {
			// Error occurred
			e.printStackTrace();
			System.exit(1);
			return Double.NaN;
		}
	}

	@Override
	public boolean isFeasible(double[] x) {
		return true;
	}

	/**
	 * Map the value in R to (-1, 1)
	 * @param valueInR
	 * @return
	 */
	public static double mapToOne(double valueInR) {
		return ( valueInR / Math.sqrt(1+valueInR*valueInR) );
	}

	public static double[] mapArrayToOne(double[] arrayInR) {
		double[] newArray = new double[arrayInR.length];
		for(int i=0; i<newArray.length; i++) {
			double valueInR = arrayInR[i];
			newArray[i] = mapToOne(valueInR);
                        //System.out.println(valueInR);
		}
		return newArray;
	}

}
