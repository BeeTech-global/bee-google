package global.beetech.gmail;

import java.io.IOException;
import java.util.List;

import com.google.api.services.gmail.model.Message;

public class BeetechGmail extends Email {

	public BeetechGmail() {
		super();
	}
	
	public void delete(String userId, String messageId) throws IOException {
		service.users().messages().trash(userId, messageId).execute();
	}
	
	public void downloadEmailAttachments(String directory, String filter, String extension) throws IOException {
		List<Message> msg = listMessages("me", filter);
		if (msg.size() > 0) {
			for (Message message : msg) {
				getAttachments(directory, "me", message.getId(), extension);
				delete("me", message.getId());
			}
		}
	}


}
