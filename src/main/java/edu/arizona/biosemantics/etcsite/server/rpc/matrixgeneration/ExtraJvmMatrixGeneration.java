package edu.arizona.biosemantics.etcsite.server.rpc.matrixgeneration;

import java.util.HashSet;
import java.util.Set;

import edu.arizona.biosemantics.etcsite.server.Configuration;
import edu.arizona.biosemantics.etcsite.server.ExtraJvmCallable;
import edu.arizona.biosemantics.etcsite.shared.log.LogLevel;
import edu.arizona.biosemantics.etcsite.shared.rpc.matrixGeneration.MatrixGenerationException;
import edu.arizona.biosemantics.etcsite.shared.rpc.semanticmarkup.SemanticMarkupException;

public class ExtraJvmMatrixGeneration extends ExtraJvmCallable<Void> implements MatrixGeneration {

	public static class MainWrapper {
		
		public static void main(String[] args) {
			try {
				edu.arizona.biosemantics.matrixgeneration.Main.main(args);
			} catch (Exception e) {
				System.exit(-1);
			}
		}
		
	}
	
	private String inputDir;
	private String outputFile;
	private boolean inheritValues;
	private boolean generateAbsentPresent;

	public ExtraJvmMatrixGeneration(String inputDir, String outputFile, boolean inheritValues, 
			boolean generateAbsentPresent) {
		this.inputDir = inputDir;
		this.outputFile = outputFile;
		this.inheritValues = inheritValues;
		this.generateAbsentPresent = generateAbsentPresent;
		
		this.setArgs(createArgs());
		//could be reduced to only libraries relevant to matrixgeneration
		if(!Configuration.matrixgeneration_xms.isEmpty()) 
			this.setXms(Configuration.matrixgeneration_xms);
		if(!Configuration.matrixgeneration_xmx.isEmpty()) 
			this.setXmx(Configuration.matrixgeneration_xmx);
		if(Configuration.classpath.isEmpty())
			this.setClassPath(System.getProperty("java.class.path"));
		else
			this.setClassPath(Configuration.classpath);
		this.setMainClass(MainWrapper.class);
	}
	
	private String[] createArgs() {
		String[] args = new String[2];
		args[0] = inputDir;
		args[1] = outputFile;
		args[2] = String.valueOf(inheritValues);
		args[3] = String.valueOf(generateAbsentPresent);
		return args;
	}

	@Override
	public Void createReturn() throws MatrixGenerationException {
		if(exitStatus != 0) {
			log(LogLevel.ERROR, "Matrix generation failed.");
			throw new MatrixGenerationException(null);
		}
		return null;
	}
	
	public static void main(String[] args) throws Exception {
		//MatrixGeneration mg = new MatrixGeneration("C:/test/users/1070/input_2", "C:/test/temp/matrixGeneration/124/Matrix.mx");
		ExtraJvmMatrixGeneration mg = new ExtraJvmMatrixGeneration("C:/test/Test_mmm", "C:/test/Test_mmm.mx", true, true);
		mg.call();
		
	}


}
