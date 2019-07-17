package global.beetech.google.drive;

import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;
import static com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance;
import static com.google.api.services.drive.DriveScopes.all;
import static java.lang.String.format;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class GoogleDrive {

	private static final JsonFactory JSON_FACTORY = getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";
	private Drive service;

	public GoogleDrive() {
		try {
			service = getService();
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	private static final java.util.Collection<String> SCOPES = all();

	private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		InputStream in = getClass().getResourceAsStream("/client_secret.json");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
		GoogleAuthorizationCodeFlow flow = getGoogleAuthorizationCodeFlow(HTTP_TRANSPORT, clientSecrets);
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	private GoogleAuthorizationCodeFlow getGoogleAuthorizationCodeFlow(final NetHttpTransport HTTP_TRANSPORT,
			GoogleClientSecrets clientSecrets) throws IOException {
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		return flow;
	}

	private HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
		return new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest httpRequest) throws IOException {
				requestInitializer.initialize(httpRequest);
				httpRequest.setConnectTimeout(3 * 60000);
				httpRequest.setReadTimeout(3 * 60000);
			}
		};
	}

	private Drive getService() throws GeneralSecurityException, IOException {
		final NetHttpTransport HTTP_TRANSPORT = newTrustedTransport();
		return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, setHttpTimeout(this.getCredentials(HTTP_TRANSPORT)))
				.build();
	}

	public List<File> getFiles(String filter) throws IOException {
		String label = format("name contains '%s'", filter);
		FileList result = service.files().list().setQ(label).execute();
		return result.getFiles();
	}

	public void download(String path, String id) throws IOException {
		OutputStream outputStream = new FileOutputStream(path);
		service.files().get(id).executeMediaAndDownloadTo(outputStream);
	}
	
	public String getWebContentLink(String fileId) throws IOException {
	      File file = service.files().get(fileId).execute();
	      return file.getWebContentLink();
	  }
	
	public String upload(String folderId, String url,String name, String type) throws IOException {
		
		File fileMetadata = new File();
		fileMetadata.setName(name);
		fileMetadata.setParents(Collections.singletonList(folderId));
		
		java.io.File filePath = new java.io.File(url);
		FileContent mediaContent = new FileContent(type, filePath);
		File file = service.files()
			.create(fileMetadata, mediaContent)
		    .setFields("id,webContentLink, webViewLink, parents")
		    .execute();
		return file.getWebViewLink();
	}

	public void delete(String id) throws IOException {
		service.files().delete(id).execute();
	}

	public void rename(String fileId, String name) {
		try {
			File file = new File();
			file.setName(name);

			Update update = service.files().update(fileId, file);
			update.setFields("name");

			update.execute();
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}

	public void copy(String originFileId, String copyTitle) {
		File copiedFile = new File();
		copiedFile.setName(copyTitle);
		try {
			service.files().copy(originFileId, copiedFile).execute();
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}
}