package global.beetech.google.sheet;

import static com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.load;
import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;
import static com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance;
import static com.google.api.services.sheets.v4.SheetsScopes.DRIVE;
import static com.google.api.services.sheets.v4.SheetsScopes.DRIVE_FILE;
import static com.google.api.services.sheets.v4.SheetsScopes.SPREADSHEETS;
import static com.google.api.services.sheets.v4.SheetsScopes.SPREADSHEETS_READONLY;
import static java.lang.System.err;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GoogleSheet {

	private static final String APPLICATION_NAME = "195936116391-je0vm5btcdnt0de83tet2mdss10lagpf.apps.googleusercontent.com";
	private static String ID_SHEET;
	private static String SHEET_RANGE;
	
	private static final File DATA_STORE_DIR = new File(getProperty("user.home"),
			".credentials/sheets.googleapis.com-java-quickstart");

	private static FileDataStoreFactory DATA_STORE_FACTORY;
	private static final JsonFactory JSON_FACTORY = getDefaultInstance();

	private static HttpTransport HTTP_TRANSPORT;
	private static final List<String> SCOPES = asList(SPREADSHEETS_READONLY, SPREADSHEETS, DRIVE, DRIVE_FILE);
	private static Sheets sheet = null;
	public static List<List<Object>> VALUES;

	static {
		try {
			HTTP_TRANSPORT = newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	public static void login(String sheetId, String range) throws IOException {
		ID_SHEET = sheetId;
		SHEET_RANGE = range;
		authorize();
	}

	private static void authorize() throws IOException {

		InputStream in = GoogleSheet.class.getClassLoader().getResourceAsStream("client_secret.json");
		GoogleClientSecrets clientSecrets = load(JSON_FACTORY, new InputStreamReader(in));

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
		System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
		sheet = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
				.build();
	}

	public static Sheets getSheetsService() {
		return sheet;
	}

	public static List<List<Object>> getValues(String tab) throws IOException {
		return getSheetsService().spreadsheets()
				.values()
				.get(ID_SHEET, "'" + tab + "'!".concat(SHEET_RANGE))
				.execute()
				.getValues();
	}
	
	public static Integer getLast(String tab) throws IOException {
		return sheet.spreadsheets()
				.values()
				.get(ID_SHEET, "'" + tab + "'!".concat(SHEET_RANGE))
				.execute()
				.getValues()
				.size() + 1;
	}

	
	public static Map<Integer, List<String>> getSheet(String tab)  {
		List<List<Object>> sheet = new ArrayList();
		try {
			sheet = getValues(tab);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<Integer, List<String>> rows = new HashMap<>();

		for (int i = 1; i < sheet.size(); i++) {
			List<?> row = sheet.get(i);
			Object[] data = row.toArray();
			rows.put( i, asList(data).stream().map(l -> l.toString()).collect(toList()));
		}

		return rows;
	}

	
	public static void setValue(String index, String value, String tab) {

		try {
			List<String> bory = asList(value);
			ValueRange valueRange = new ValueRange();
			String range = "" + tab + "!" + index;
			valueRange.set("values", asList(bory));
			Update request = sheet.spreadsheets()
					.values()
					.update(ID_SHEET, range, valueRange);
			request.setValueInputOption("USER_ENTERED");
			request.execute();
		} catch (Exception e) {
			err.println(e.getMessage());
			try {
				sleep(5000);
				authorize();
				setValue(index, value, tab);
			} catch (IOException | InterruptedException e1) {
				err.println(e1.getMessage());
			}
		}
	}
}