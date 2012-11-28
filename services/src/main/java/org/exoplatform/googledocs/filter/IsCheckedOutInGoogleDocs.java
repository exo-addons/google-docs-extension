package org.exoplatform.googledocs.filter;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.googledocs.GoogleDocsConstants;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

public class IsCheckedOutInGoogleDocs extends UIExtensionAbstractFilter {

	public IsCheckedOutInGoogleDocs() {
		this("UIActionBar.msg.googledocs.not-in-google-docs");
	}

	public IsCheckedOutInGoogleDocs(String messageKey) {
		super(messageKey, UIExtensionFilterType.MANDATORY);
	}

	@Override
	public boolean accept(Map<String, Object> context) throws Exception {
	    if (context == null) {
	    	return true;
	    }
	    Node currentNode = (Node) context.get(Node.class.getName());
	    
	    return currentNode.isNodeType(GoogleDocsConstants.GOOGLE_DRIVE_NODE_TYPE) && currentNode.hasProperty(GoogleDocsConstants.GOOGLE_DRIVE_NODE_PROPERTY);
	}

	@Override
	public void onDeny(Map<String, Object> context) throws Exception {
		if (context == null) return;
	    createUIPopupMessages(context, messageKey);
	}

}
