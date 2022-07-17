package com.example.sendmail2;

import android.content.Context;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GmailAPI {

	
	/*
	1.Get code : 
https://accounts.google.com/o/oauth2/v2/auth?
 scope=https://mail.google.com&
 access_type=offline&
 redirect_uri=http://localhost&
 response_type=code&
 client_id=[Client ID]

2. Get access_token and refresh_token
 curl \
--request POST \
--data "code=[Authentcation code from authorization link]&client_id=[Application Client Id]&client_secret=[Application Client Secret]&redirect_uri=http://localhost&grant_type=authorization_code" \
https://accounts.google.com/o/oauth2/token

3.Get new access_token using refresh_token
curl \
--request POST \
--data "client_id=[your_client_id]&client_secret=[your_client_secret]&refresh_token=[refresh_token]&grant_type=refresh_token" \
https://accounts.google.com/o/oauth2/token
	
*/
	private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String user = "me";
	static Gmail service = null;
	private static File filePath = new File("/raw/credentials.json");

	private static String REFRESH_TOKEN="1//04DBM8TCw9uEWCgYIARAAGAQSNwF-L9IrONRZbIl3mMedO2h4a51CfWE0f6QS0ghRunr8JB1ayi95seY3vniDIr4sMinJJHBG_zE";


	public static void getMailBody(String searchString) throws IOException {

		// Access Gmail inbox

		Gmail.Users.Messages.List request = service.users().messages().list(user).setQ(searchString);

		ListMessagesResponse messagesResponse = request.execute();
		request.setPageToken(messagesResponse.getNextPageToken());

		// Get ID of the email you are looking for
		String messageId = messagesResponse.getMessages().get(0).getId();

		Message message = service.users().messages().get(user, messageId).execute();

		// Print email body

		String emailBody = StringUtils
				.newStringUtf8(Base64.decodeBase64(message.getPayload().getParts().get(0).getBody().getData()));

		System.out.println("Email body : " + emailBody);

	}

	public static String isToString(InputStream is) {
		final int bufferSize = 1024;
		final char[] buffer = new char[bufferSize];
		final StringBuilder out = new StringBuilder();
		Reader in = null;
		try {
			in = new InputStreamReader(is, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		for (; ; ) {
			int rsz = 0;
			try {
				rsz = in.read(buffer, 0, buffer.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (rsz < 0)
				break;
			out.append(buffer, 0, rsz);
		}
		return out.toString();
	}

	public static Gmail getGmailService(Context context) throws IOException, GeneralSecurityException {

		// Read credentials.json

		InputStream in = context.getResources().openRawResource(R.raw.credentials);
		Reader reader= new InputStreamReader(in);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Credential builder

		Credential authorize = new GoogleCredential.Builder().setTransport(GoogleNetHttpTransport.newTrustedTransport())
				.setJsonFactory(JSON_FACTORY)
				.setClientSecrets(clientSecrets.getDetails().getClientId(),
						clientSecrets.getDetails().getClientSecret())
				.build().setAccessToken(refreshAccessToken(context)).setRefreshToken(REFRESH_TOKEN);

		// Create Gmail service
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, authorize)
				.setApplicationName(GmailAPI.APPLICATION_NAME).build();

		return service;
	}

	private static String getAccessToken() {

		try {
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("grant_type", "refresh_token");
			params.put("client_id", "337247275082-qu0d26nci2lkol6bd66ccm3846vmviio.apps.googleusercontent.com"); //Replace this
			params.put("client_secret", "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"); //Replace this
			params.put("refresh_token", REFRESH_TOKEN); //Replace this

			StringBuilder postData = new StringBuilder();
			for (Map.Entry<String, Object> param : params.entrySet()) {
				if (postData.length() != 0) {
					postData.append('&');
				}
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");

			URL url = new URL("https://accounts.google.com/o/oauth2/token");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setRequestMethod("POST");
			con.getOutputStream().write(postDataBytes);

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				buffer.append(line);
			}

			JSONObject json = new JSONObject(buffer.toString());
			String accessToken = json.getString("access_token");
			return accessToken;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static String refreshAccessToken(Context context) throws IOException {

		InputStream in = context.getResources().openRawResource(R.raw.credentials);
		Reader reader= new InputStreamReader(in);
		JsonObject JsonObj = JsonParser.parseReader(reader).getAsJsonObject();
		JsonObject webObj= JsonObj.get("web").getAsJsonObject();
		String clientID= webObj.get("client_id").getAsString();
		String clientSecret= webObj.get("client_secret").getAsString();
		TokenResponse response = new GoogleRefreshTokenRequest(
				new NetHttpTransport(),
				new GsonFactory(),
				REFRESH_TOKEN,
				clientID,
				clientSecret)
				.execute();
//		System.out.println("Access token: " + response.getAccessToken());

		return response.getAccessToken();
	}

}
