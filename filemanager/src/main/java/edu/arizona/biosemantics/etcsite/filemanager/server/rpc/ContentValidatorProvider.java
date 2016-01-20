package edu.arizona.biosemantics.etcsite.filemanager.server.rpc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

import edu.arizona.biosemantics.etcsite.common.shared.model.FileTypeEnum;
import edu.arizona.biosemantics.etcsite.filemanager.server.Configuration;

public class ContentValidatorProvider {

	private Map<FileTypeEnum, IContentValidator> fileTypeValidatorMap = new HashMap<FileTypeEnum, IContentValidator>();
	
	@Inject
	public ContentValidatorProvider() {
		fileTypeValidatorMap.put(FileTypeEnum.TAXON_DESCRIPTION, new XMLValidator(new File(Configuration.taxonDescriptionSchemaFile)));
		fileTypeValidatorMap.put(FileTypeEnum.MARKED_UP_TAXON_DESCRIPTION, new XMLValidator(new File(Configuration.markedUpTaxonDescriptionSchemaFile)));
		fileTypeValidatorMap.put(FileTypeEnum.PLAIN_TEXT, null);
		fileTypeValidatorMap.put(FileTypeEnum.CLEANTAX, new CleanTaxReader());
	}
	
	public IContentValidator getValidator(FileTypeEnum fileTypeEnum) {
		return fileTypeValidatorMap.get(fileTypeEnum);
	}

}
