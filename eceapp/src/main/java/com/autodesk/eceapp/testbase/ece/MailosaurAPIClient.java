package com.autodesk.eceapp.testbase.ece;

import com.autodesk.eceapp.utilities.ResourceFileLoader;
import com.autodesk.testinghub.core.utils.AssertUtils;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.YamlUtil;
import com.mailosaur.MailosaurClient;
import com.mailosaur.MailosaurException;
import com.mailosaur.models.Message;
import com.mailosaur.models.MessageSearchParams;
import com.mailosaur.models.SearchCriteria;
import java.io.IOException;
import java.util.Map;

/**
 * A class to handle connection and requests to a mailosaur server for email testing
 */
public class MailosaurAPIClient {

  private final MailosaurClient mailosaur;
  private final MessageSearchParams params = new MessageSearchParams();

  public MailosaurAPIClient() {
    Map<String, String> mailosaurCredentials = (Map<String, String>) ResourceFileLoader.getMailosaurCredentialsYaml();

    String apiKey = ProtectedConfigFile.decrypt(mailosaurCredentials.get("encryptedAPIKey"));
    String serverId = ProtectedConfigFile.decrypt(mailosaurCredentials.get("encryptedServerId"));

    mailosaur = new MailosaurClient(apiKey);

    params.withServer(serverId);
  }

  /**
   * Get the body of the last email sent to a recipient
   *
   * @param recipient - Recipient email
   * @return - Email body
   */
  public String getMessageBody(String recipient) {
    SearchCriteria criteria = new SearchCriteria();
    criteria.withSentTo(recipient);

    try {
      Message message = mailosaur.messages().get(params, criteria);
      return message.html().body();
    } catch (IOException | MailosaurException e) {
      AssertUtils.fail("Failed to receive oxygen registration verify account email");
      throw new RuntimeException(e);
    }
  }
}
