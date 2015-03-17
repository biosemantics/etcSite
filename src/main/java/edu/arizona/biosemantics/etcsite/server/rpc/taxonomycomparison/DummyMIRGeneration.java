package edu.arizona.biosemantics.etcsite.server.rpc.taxonomycomparison;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class DummyMIRGeneration implements MIRGeneration {

	private String eulerInputFile;
	private String outputDir;

	public DummyMIRGeneration(String eulerInputFile, String outputDir) {
		this.eulerInputFile = eulerInputFile;
		this.outputDir = outputDir;
	}

	@Override
	public Void call() throws Exception {
		File pwDir = new File(outputDir + File.separator + "6-PWs-pdf");
		File aggregateDir = new File(outputDir + File.separator + "7-PWs-aggregate");
		File diagnosisDir = new File(outputDir + File.separator + "XYZ-diagnosis");
		pwDir.mkdirs();
		aggregateDir.mkdirs();
		diagnosisDir.mkdirs();
		FileUtils.copyDirectory(new File("C:/Users/rodenhausen/etcsite/eulerdummyPWs"), pwDir);
		FileUtils.copyDirectory(new File("C:/Users/rodenhausen/etcsite/eulerdummyAggregate"), aggregateDir);
		FileUtils.copyDirectory(new File("C:/Users/rodenhausen/etcsite/eulerdummyDiagnosis"), diagnosisDir);
		return null;
	}

	@Override
	public boolean isExecutedSuccessfully() {
		return true;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

}