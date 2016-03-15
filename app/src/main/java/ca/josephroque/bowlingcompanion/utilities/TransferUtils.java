package ca.josephroque.bowlingcompanion.utilities;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Joseph Roque on 2016-03-13. Provides methods and constants for enabling the transferring of data to a new
 * device.
 */
public final class TransferUtils {

    /** Identifies output from this class in Logcat. */
    @SuppressWarnings("unused")
    private static final String TAG = "TransferUtils";

    /** URL to upload or download data to/from. */
    private static final String TRANSFER_SERVER_URL = "http://10.0.2.2:8080/";

    /** Time to wait before closing connection. */
    public static final int CONNECTION_TIMEOUT = 1000 * 10;
    /** Time to wait before closing connection, if previous connections failed. */
    public static final int CONNECTION_EXTENDED_TIMEOUT = 1000 * 25;

    /** Represents an error in which the server is currently unavailable. User should try again later. */
    public static final String ERROR_UNAVAILABLE = "UNAVAILABLE";
    /** Represents a timeout error. */
    public static final String ERROR_TIMEOUT = "TIMEOUT";
    /** Represents an error in which a connection was cancelled. */
    public static final String ERROR_CANCELLED = "CANCELLED";
    /** Represents an IO error. */
    public static final String ERROR_IO_EXCEPTION = "IO";
    /** Represents an out of memory error during upload/download. */
    public static final String ERROR_OUT_OF_MEMORY = "OOM";
    /** Represents an error in which the database file could not be found for upload. */
    public static final String ERROR_FILE_NOT_FOUND = "MIA";
    /** Represents an incorrect URL error. */
    public static final String ERROR_MALFORMED_URL = "URL";
    /** Represents any other error which may occur during upload/download. */
    public static final String ERROR_EXCEPTION = "ERROR";

    /** Max buffer size during data transfer. */
    public static final int MAX_BUFFER_SIZE = 1024;

    /** The only acceptable response for success from the server. */
    public static final int SUCCESS_RESPONSE = 200;

    /**
     * Returns the URL for GET requests to check the status of the server.
     *
     * @return URL for server status check
     */
    public static String getStatusEndpoint() {
        return TRANSFER_SERVER_URL + "status";
    }

    /**
     * Returns the URL for POST requests to upload bowler data.
     *
     * @return URL for data upload
     */
    public static String getUploadEndpoint() {
        return TRANSFER_SERVER_URL + "upload";
    }

    /**
     * Returns the URL for GET requests to download user data.
     *
     * @param key unique key which represents data.
     * @return URL for data download.
     */
    public static String getDownloadEndpoint(String key) {
        return TRANSFER_SERVER_URL + "download?key=" + key;
    }

    /**
     * Performs a GET request to check the status of the server and if uploading or downloading data is possible. Should
     * NOT be run on the main thread.
     *
     * @return {@code true} if the app should continue with uploading/downloading data, {@code false} otherwise.
     */
    public static boolean getServerStatus() {
        try {
            URL url = new URL(getStatusEndpoint());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(CONNECTION_TIMEOUT);

            int responseCode = connection.getResponseCode();
            if (responseCode == SUCCESS_RESPONSE) {
                StringBuilder responseMsg = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    responseMsg.append(line);
                    line = reader.readLine();
                }
                reader.close();

                String response = responseMsg.toString().trim().toUpperCase();
                Log.d(TAG, "Transfer server status response: " + response);

                // The server is only ready to accept uploads if it responds with "OK"
                return response.equals("OK");
            } else {
                Log.e(TAG, "Invalid response getting server status: " + responseCode);
            }
        } catch (MalformedURLException ex) {
            Log.e(TAG, "Error parsing URL. This shouldn't happen.", ex);
        } catch (IOException ex) {
            Log.e(TAG, "Error opening or closing connection.", ex);
        }

        return false;
    }

    /**
     * Default private constructor.
     */
    private TransferUtils() {
        // does nothing
    }
}
