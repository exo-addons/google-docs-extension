package org.exoplatform.googledocs.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import javax.jcr.Node;

import org.exoplatform.container.xml.InitParams;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

public class GoogleDriveService {

	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();

	private String serviceEmail;
	private String accountEmail;
	private String privateKeyFilePath;

	private Drive driveService;

	public GoogleDriveService(InitParams initParams) {
		this.serviceEmail = initParams.getValueParam("serviceEmail").getValue();
		this.accountEmail = initParams.getValueParam("accountEmail").getValue();
		this.privateKeyFilePath = initParams.getValueParam("privateKeyFilePath").getValue();
	}

	private Drive getDriveService() throws IOException, GeneralSecurityException {

		if (driveService == null) {
			GoogleCredential credential = new GoogleCredential.Builder()
				.setTransport(HTTP_TRANSPORT) //
				.setJsonFactory(JSON_FACTORY) //
				.setServiceAccountId(this.serviceEmail) //
				.setServiceAccountScopes(DriveScopes.DRIVE) //
				.setServiceAccountPrivateKeyFromP12File(new java.io.File(this.privateKeyFilePath)) //
				.build();

			driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).build();
		}

		return driveService;
	}

	/**
	 * Add a file in Google Drive
	 * 
	 * @param fileNode Node of the file to add
	 * @return ID of the added file
	 * @throws Exception
	 */
	public File addFileInGoogleDrive(Node fileNode) throws Exception {

		Drive driveService = getDriveService();

		String mimeType = fileNode.getNode("jcr:content").getProperty("jcr:mimeType").getString();
		InputStream documentIS = fileNode.getNode("jcr:content").getProperty("jcr:data").getStream();

		// Insert a file
		File body = new File();
		body.setTitle(fileNode.getName());
		body.setMimeType(mimeType);

		// TODO files are created on file system, handle it right...
		java.io.File fileContent = new java.io.File(fileNode.getName());
		OutputStream out = new FileOutputStream(fileContent);
		byte buf[] = new byte[1024];
		int len;
		while ((len = documentIS.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		documentIS.close();

		FileContent mediaContent = new FileContent(mimeType, fileContent);

		Insert insert = driveService.files().insert(body, mediaContent);
		insert.getMediaHttpUploader().setDirectUploadEnabled(true);
		File file = insert.setConvert(true).execute();

		return file;
	}

	/**
	 * Get document content from Google Drive
	 * 
	 * @param documentId
	 *            Id of the document in Google Drive
	 * @param mimeType
	 *            Mimetype of the document
	 * @return InputStream of the content of the document
	 * @throws IOException
	 */
	public File getFile(String fileId) throws IOException, GeneralSecurityException {
		Drive driveService = getDriveService();

		return driveService.files().get(fileId).execute();
	}

	/**
	 * Get file content from Google Drive
	 * 
	 * @param fileId Id of the file in Google Drive
	 * @param mimeType Mimetype of the file
	 * @return InputStream of the content of the file
	 * @throws IOException
	 */
	public InputStream getFileContent(String fileId, String mimeType) throws IOException, GeneralSecurityException {
		Drive driveService = getDriveService();

		File file = getFile(fileId);
		
		String url = (String) file.getExportLinks().get(mimeType);
		if (url != null && url.length() > 0) {
			HttpResponse resp = driveService.getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
			return resp.getContent();
		} else {
			throw new IOException("No export URL available for the file " + fileId + " with the mimetype " + mimeType);
		}
	}
	
	/**
	 * Share a file with an user
	 * @param fileId
	 * @param userEmail
	 * @throws Exception
	 */
	public void shareFileWith(String fileId, String userEmail) throws Exception {
		Drive driveService = getDriveService();
		
		Permission newPermission = new Permission();

		newPermission.setValue(userEmail);
	    newPermission.setType("user");
	    newPermission.setRole("writer");
		
		driveService.permissions().insert(fileId, newPermission).setSendNotificationEmails(false).execute();
	}

	/**
	 * Share a file with the master Google account
	 * @param fileId
	 * @param userEmail
	 * @throws Exception
	 */
	public void shareFileWithMasterAccount(String fileId) throws Exception {
		shareFileWith(fileId, this.accountEmail);
	}
	
	/**
	 * Remove a file from Google Drive
	 * 
	 * @param fileId The ID of the file to remove
	 * @throws IOException
	 */
	public void removeFileInGoogleDrive(String fileId) throws IOException, GeneralSecurityException {
		Drive driveService = getDriveService();

		driveService.files().delete(fileId).execute();
	}
}
