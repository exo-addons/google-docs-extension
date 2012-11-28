package org.exoplatform.googledocs;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.googledocs.exception.GoogleDocsException;
import org.exoplatform.googledocs.filter.IsSupportedByGoogleDocs;
import org.exoplatform.googledocs.services.GoogleDriveService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import com.google.api.services.drive.model.File;

@ComponentConfig(events = { @EventConfig(listeners = UIGoogleDocsCheckoutAction.GoogleDocsCheckoutActionListener.class) })
public class UIGoogleDocsCheckoutAction extends UIAbstractManagerComponent {
	
	private static final Log log = ExoLogger.getLogger(UIGoogleDocsCheckoutAction.class);
	
	private static final List<UIExtensionFilter> FILTERS = Arrays
			.asList(new UIExtensionFilter[] { new CanAddNodeFilter(),
					new IsNotLockedFilter(), new IsCheckedOutFilter(),
					new IsNotTrashHomeNodeFilter(), new IsNotInTrashFilter(),
					new IsNotEditingDocumentFilter(), new IsSupportedByGoogleDocs() });
	
	@UIExtensionFilters
	public List<UIExtensionFilter> getFilters() {
		return FILTERS;
	}

	public static class GoogleDocsCheckoutActionListener extends
			UIActionBarActionListener<UIGoogleDocsCheckoutAction> {
		
		private GoogleDriveService googleDriveService;
		private OrganizationService organizationService;
		

		@Override
		protected void processEvent(Event<UIGoogleDocsCheckoutAction> event)
				throws Exception {

			googleDriveService = (GoogleDriveService) PortalContainer.getInstance().getComponentInstanceOfType(GoogleDriveService.class);
			organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
			
			UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
			Node currentNode = uiExplorer.getCurrentNode();
			
			try {
				File googleDriveFile = null;
				String googleDriveFileID = null;
				if(!currentNode.isNodeType(GoogleDocsConstants.GOOGLE_DRIVE_NODE_TYPE) || !currentNode.hasProperty(GoogleDocsConstants.GOOGLE_DRIVE_NODE_PROPERTY)) {
					// insert doc in google drive
					googleDriveFile = googleDriveService.addFileInGoogleDrive(currentNode);
					googleDriveFileID = googleDriveFile.getId();
					
					// share the document with the main Google account
					googleDriveService.shareFileWithMasterAccount(googleDriveFileID);
					
					// add google drive mixin
					currentNode.addMixin(GoogleDocsConstants.GOOGLE_DRIVE_NODE_TYPE);
					currentNode.setProperty(GoogleDocsConstants.GOOGLE_DRIVE_NODE_PROPERTY, googleDriveFileID);
					currentNode.save();
				} else {
					// get the Google Drive file id
					googleDriveFileID = currentNode.getProperty(GoogleDocsConstants.GOOGLE_DRIVE_NODE_PROPERTY).getString();
					
					// get the Google Drive file
					googleDriveFile = googleDriveService.getFile(googleDriveFileID);					
				}
								
				// share the document with the contributor
				String userId = ConversationState.getCurrent().getIdentity().getUserId();
				User user = organizationService.getUserHandler().findUserByName(userId);
				if(user == null) {
					throw new GoogleDocsException("UIActionBar.msg.googledocs.user-not-found", "User " + userId + " not found");
				}
				String userEmail = user.getEmail();
				if(userEmail == null || userEmail.isEmpty()) {
					throw new GoogleDocsException("UIActionBar.msg.googledocs.no-user-email", "Email of user " + userId + " is empty");
				}
				googleDriveService.shareFileWith(googleDriveFileID, userEmail);
				
				event.getRequestContext().getJavascriptManager().addCustomizedOnLoadScript("ajaxRedirect('" + googleDriveFile.getAlternateLink() + "');");				
			    
			} catch (GoogleDocsException gde) {
				log.error("Error while adding the document " + currentNode.getPath() + " in Google Drive - Cause : " + gde.getMessage(), gde);
				org.exoplatform.wcm.webui.Utils.createPopupMessage(uiExplorer,
                        gde.getMessageKey(),
                        null,
                        ApplicationMessage.ERROR);
			} catch (Exception e) {
				log.error("Error while adding the document " + currentNode.getPath() + " in Google Drive", e);
				org.exoplatform.wcm.webui.Utils.createPopupMessage(uiExplorer,
                        "UIActionBar.msg.googledocs.checkout-error",
                        null,
                        ApplicationMessage.ERROR);
			}
		}
	}

	@Override
	public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
		return null;
	}
}