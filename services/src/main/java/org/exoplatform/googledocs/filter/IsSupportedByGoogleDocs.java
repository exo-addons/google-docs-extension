package org.exoplatform.googledocs.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

public class IsSupportedByGoogleDocs extends UIExtensionAbstractFilter {

	private List<String> supportedMimeTypes = Arrays.asList(new String[] {
			"text/html",
			"text/plain",			
			"text/csv",
			"application/rtf",
			"application/vnd.oasis.opendocument.text",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.oasis.opendocument.spreadsheet",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/vnd.oasis.opendocument.presentation",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation"}); 
	
	public IsSupportedByGoogleDocs() {
		this("UIActionBar.msg.googledocs.not-supported-format");
	}

	public IsSupportedByGoogleDocs(String messageKey) {
		super(messageKey, UIExtensionFilterType.MANDATORY);
	}

	@Override
	public boolean accept(Map<String, Object> context) throws Exception {
		if (context == null) {
	    	return true;
	    }
	    
	    Node currentNode = (Node) context.get(Node.class.getName());
	    if(!currentNode.isNodeType("nt:file")) {
	    	return false;
	    }
	    
	    String mimeType = currentNode.getNode("jcr:content").getProperty("jcr:mimeType").getString();

	    return supportedMimeTypes.contains(mimeType);
	}

	@Override
	public void onDeny(Map<String, Object> context) throws Exception {
		if (context == null) return;
	    createUIPopupMessages(context, messageKey);
	}

}
