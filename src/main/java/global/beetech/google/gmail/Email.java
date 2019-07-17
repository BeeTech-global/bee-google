package global.beetech.google.gmail;

import static com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.load;
import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;
import static com.google.api.client.json.gson.GsonFactory.getDefaultInstance;
import static com.google.api.client.util.Base64.decodeBase64;
import static com.google.api.services.gmail.GmailScopes.GMAIL_METADATA;
import static com.google.api.services.gmail.GmailScopes.all;
import static java.io.File.separator;
import static java.lang.System.exit;
import static java.lang.System.getProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;

public class Email {

	public static final String APPLICATION_NAME = "GMAIL Remessa";
	protected FileDataStoreFactory DATA_STORE_FACTORY;
	protected JsonFactory JSON_FACTORY;
	protected List<Message> messages;
	protected java.io.File DATA_STORE_DIR = null;
	protected HttpTransport HTTP_TRANSPORT;
	protected Gmail service;

	public Email() {
		try {
			 messages = new ArrayList<Message>();
			 JSON_FACTORY = getDefaultInstance();
			
			service = getGmailService();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Gmail getGmailService() throws IOException {
		DATA_STORE_DIR = new java.io.File(getProperty("user.home"), ".credentials/gmail-java");
		try {
			HTTP_TRANSPORT = newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			exit(1);
		}
		Credential credential = authorize();
		return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	}

	private Credential authorize() throws IOException {

		InputStream in = getClass().getResourceAsStream("/client_secret.json");

		GoogleClientSecrets clientSecrets = load(JSON_FACTORY, new InputStreamReader(in));
		Set<String> scopes = all();
		Set<String> scopesAll = new HashSet<>();

		scopes.stream().filter(s -> !s.contentEquals(GMAIL_METADATA)).forEach(s -> scopesAll.add(s));

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, scopesAll).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
		System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
		return credential;
	}

	protected void listLabels(Gmail service, String userId) throws IOException {
		ListLabelsResponse response = service.users().labels().list(userId).execute();
		List<Label> labels = response.getLabels();
		for (Label label : labels) {
			System.out.println(service.getApplicationName() + " - " + label.toPrettyString());
		}
	}

	protected void getAttachments(String path, String userId, String messageId) throws IOException {
		getAttachments(path, userId, messageId, "pdf");
	}

	protected void getAttachments(String path, String userId, String messageId, String extension) throws IOException {

		Message message = service.users().messages().get(userId, messageId).execute();
		List<MessagePart> parts = message.getPayload().getParts();
		MessagePartBody attachPart;

		for (MessagePart part : parts) {
			if (part.getFilename() != null) {
				File f = new File(path + separator
						+ part.getFilename().replaceAll(" ", "").replaceAll("-", "").replaceAll("/", ""));
				if (!f.exists() && part.getFilename().length() > 0) {
					String filename = part.getFilename().replaceAll(" ", "").replaceAll("-", "").replaceAll("/", "");
					String attId = part.getBody().getAttachmentId();

					try {
						attachPart = service.users().messages().attachments().get(userId, messageId, attId).execute();
					} catch (Exception e) {
						System.out.println("Erro no " + filename);
						e.printStackTrace();
						return;
					}
					FileOutputStream fileOutFile = null;

					filename = filename.toLowerCase().replace(".", "").replace(",", "");
					filename = filename.replace(extension, ".".concat(extension));
					try {
						byte[] fileByteArray = decodeBase64(attachPart.getData());
						fileOutFile = new FileOutputStream(path + separator + filename);
						fileOutFile.write(fileByteArray);
					} catch (Exception e) {

					} finally {
						if (fileOutFile != null)
							fileOutFile.close();
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	public List<Message> listMessages(String userId, String filter) throws IOException {

		ListMessagesResponse response = service.users().messages().list(userId).setQ(filter).execute();
		messages.clear();
		while (response.getMessages() != null) {
			messages.addAll(response.getMessages());
			if (response.getNextPageToken() != null) {
				String pageToken = response.getNextPageToken();
				response = service.users().messages().list(userId).setQ(filter).setPageToken(pageToken).execute();
			} else {
				break;
			}
		}
		int msg_count = 0;
		for (Message message : messages) {
			msg_count++;
		}
		System.out.println("Found " + msg_count + " emails. " + filter);
		return messages;

	}

	public void delete(String userId, String messageId) throws IOException {
		service.users().messages().trash(userId, messageId).execute();
	}

}
