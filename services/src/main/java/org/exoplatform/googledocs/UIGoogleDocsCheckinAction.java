package org.exoplatform.googledocs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.REST.viewer.PDFViewerRESTService;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.googledocs.filter.IsCheckedOutInGoogleDocs;
import org.exoplatform.googledocs.filter.IsSupportedByGoogleDocs;
import org.exoplatform.googledocs.services.GoogleDriveService;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

@ComponentConfig(events = { @EventConfig(listeners = UIGoogleDocsCheckinAction.GoogleDocsCheckinActionListener.class) })
public class UIGoogleDocsCheckinAction extends UIComponent {

	private static final Log log = ExoLogger.getLogger(UIGoogleDocsCheckinAction.class);

	private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new CanAddNodeFilter(), new IsNotLockedFilter(),
			new IsCheckedOutFilter(), new IsNotTrashHomeNodeFilter(), new IsNotInTrashFilter(), new IsNotEditingDocumentFilter(),
			new IsSupportedByGoogleDocs(), new IsCheckedOutInGoogleDocs() });

	@UIExtensionFilters
	public List<UIExtensionFilter> getFilters() {
		return FILTERS;
	}

	public static class GoogleDocsCheckinActionListener extends UIActionBarActionListener<UIGoogleDocsCheckinAction> {

		private GoogleDriveService googleDriveService;

		@Override
		protected void processEvent(Event<UIGoogleDocsCheckinAction> event) throws Exception {

			googleDriveService = (GoogleDriveService) PortalContainer.getInstance().getComponentInstanceOfType(GoogleDriveService.class);

			UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
			Node currentNode = uiExplorer.getCurrentNode();

			try {
				if (currentNode.isNodeType(GoogleDocsConstants.GOOGLE_DRIVE_NODE_TYPE)
						&& currentNode.getProperty(GoogleDocsConstants.GOOGLE_DRIVE_NODE_PROPERTY) != null
						&& !currentNode.getProperty(GoogleDocsConstants.GOOGLE_DRIVE_NODE_PROPERTY).getString().isEmpty()) {

					// get Google Drive file id
					String fileId = currentNode.getProperty(GoogleDocsConstants.GOOGLE_DRIVE_NODE_PROPERTY).getString();

					Node contentNode = currentNode.getNode("jcr:content");
					String mimeType = contentNode.getProperty("jcr:mimeType").getString();

					// store Google Drive document content in the node's data
					InputStream fileInputStream = googleDriveService.getFileContent(fileId, mimeType);
					if (fileInputStream != null) {
						contentNode.getProperty("jcr:data").setValue(fileInputStream);
						currentNode.save();
					}

					// remove the document from Google Drive
					googleDriveService.removeFileInGoogleDrive(fileId);

					// remove mixin
					currentNode.removeMixin(GoogleDocsConstants.GOOGLE_DRIVE_NODE_TYPE);
					currentNode.save();

					// clear corresponding cache entry
					CacheService cacheService = (CacheService) PortalContainer.getInstance().getComponentInstanceOfType(CacheService.class);
					ExoCache<Serializable, Object> pdfCache = cacheService.getCacheInstance(PDFViewerRESTService.class.getName());
					StringBuilder sbFileCacheKey = new StringBuilder();
					sbFileCacheKey.append(((RepositoryImpl) currentNode.getSession().getRepository()).getName()) //
						.append("/") //
						.append(currentNode.getSession().getWorkspace().getName()) //
						.append("/") //
						.append(currentNode.getUUID());

					final String fileCacheKey = sbFileCacheKey.toString();
					
					// use selector to clear only the entries related to this document 
					CachedObjectSelector<Serializable, Object> selector = new CachedObjectSelector<Serializable, Object>() {
						
						public boolean select(Serializable key, ObjectCacheInfo<? extends Object> ocinfo) {
							String skey = key.toString();
							if (skey.toString().startsWith(fileCacheKey)) {
								return true;
							}
							return false;
						}

						public void onSelect(ExoCache<? extends Serializable, ? extends Object> cache, Serializable key,
								ObjectCacheInfo<? extends Object> ocinfo) throws Exception {
							cache.remove(key);
						}

					};
					pdfCache.select(selector);

					event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer.getChild(UIWorkingArea.class));

					log.info("Document " + currentNode.getPath() + " has been succesfully checked in from Google Drive (Google Drive ID : " + fileId + ")");
				} else {
					log.warn("No Google Drive ID available on the document " + currentNode.getPath());
					org.exoplatform.wcm.webui.Utils.createPopupMessage(uiExplorer, "UIActionBar.msg.googledocs.not-in-google-docs", null,
							ApplicationMessage.WARNING);
				}
			} catch (IOException ioe) {
				log.error("Error when checking in document " + currentNode.getPath() + " from Google Drive", ioe);
				org.exoplatform.wcm.webui.Utils.createPopupMessage(uiExplorer, "UIActionBar.msg.googledocs.checkin-error", null, ApplicationMessage.ERROR);
			}
		}
	}
}