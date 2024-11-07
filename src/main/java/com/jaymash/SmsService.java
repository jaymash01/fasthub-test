package com.jaymash;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsService {
    public static String CLIENT_ID = "60ddcfa2-9ca1-454b-8ed4-ba5fb32e573a";
    public static String CLIENT_SECRET = "4c0b69a0-f514-45b4-8ad3-18d546a84f6c";
    public static Pattern PHONE_NUMBERS_PATTERN = Pattern.compile("\\[PHONE]:\\s(.*)");
    public static Pattern MESSAGE_PATTERN = Pattern.compile("[\\SMS]:\\s(.*)");

    private int attempts = 1;
    private List<String> phoneNumbers;
    private String message;

    public SmsService() {
        extractDataFromFile();
    }

    private void extractDataFromFile() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("phone_numbers.txt").getFile());

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = PHONE_NUMBERS_PATTERN.matcher(line);
                if (matcher.find()) {
                    phoneNumbers = List.of(matcher.group(1).split(",\\s"));
                }

                matcher = MESSAGE_PATTERN.matcher(line);
                if (matcher.find()) {
                    message = matcher.group(1);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getInputString() {
        List<String> messages = new ArrayList<>();

        for (String phoneNumber : phoneNumbers) {
            String text = message.replace("{PHONE}", phoneNumber);
            String reference = phoneNumber + new Date().getTime();
            messages.add("{\"text\": \"" + text + "\",\"msisdn\": \"" + phoneNumber + "\",\"source\": \"FASTHUB\",\"reference\": \"" + reference + "\"}");
        }

        return "{\"auth\": {\"clientId\": \"" + CLIENT_ID + "\",\"clientSecret\": \"" + CLIENT_SECRET + "\"},\"messages\": [" + String.join(",", messages) + "]}";
    }

    public void sendMessage() {
        try {
            URL url = new URL("https://bulksms.fasthub.co.tz/api/sms/send");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String inputString = getInputString();
            System.out.println(inputString);

            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = inputString.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }

            String response = FileUtils.readFromInputStream(connection.getInputStream());
            System.out.println("Response: " + response);

            JSONObject jsonObject = new JSONObject(response);
            boolean status = jsonObject.getBoolean("status");
            System.out.println("Status: " + status);

            if (!status && attempts < 3) {
                sendMessage();
                attempts++;
            }

            connection.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
