package edu.arizona.biosemantics.etcsite.client.common.files;

import java.util.Map;

import com.google.inject.Inject;

import edu.arizona.biosemantics.etcsite.client.common.files.FileDragDropHandler.IFileMoveListener;
import edu.arizona.biosemantics.etcsite.shared.model.file.FileFilter;
import edu.arizona.biosemantics.etcsite.shared.model.file.FileInfo;
import edu.arizona.biosemantics.etcsite.shared.model.file.Tree;
import edu.arizona.biosemantics.etcsite.shared.rpc.file.IFileServiceAsync;

public class DnDFileTreePresenter extends FileTreePresenter implements IFileMoveListener {

	private FileFilter fileFilter;
	private FileDragDropHandler fileDragDropHandler;
	
	@Inject
	public DnDFileTreePresenter(IFileTreeView view, IFileServiceAsync fileService, 
			FileTreeDecorator fileTreeDecorator, FileDragDropHandler fileDragDropHandler) {
		super(view, fileService, fileTreeDecorator);
		this.fileDragDropHandler = fileDragDropHandler;
		fileDragDropHandler.addListener(this);
	}
	
	@Override
	protected void decorate(Tree<FileInfo> tree, String selectionPath, Map<String, Boolean> retainedStates, FileFilter fileFilter) {
		this.fileFilter = fileFilter;
		fileTreeDecorator.decorate(view, tree, fileFilter, fileDragDropHandler, selectionPath, retainedStates);
	}	
	
	@Override
	public void notifyFileMove() {
		this.refresh(fileFilter);
	}
}
