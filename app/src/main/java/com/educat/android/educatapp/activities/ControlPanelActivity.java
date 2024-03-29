package com.educat.android.educatapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.educat.android.educatapp.R;
import com.educat.android.educatapp.adapters.StringListAdapter;
import com.educat.android.educatapp.helperClasses.AppController;
import com.educat.android.educatapp.helperClasses.Constants;
import com.educat.android.educatapp.helperClasses.MessageCodes;
import com.educat.android.educatapp.helperClasses.MyLog;
import com.educat.android.educatapp.helperClasses.StatusCodes;
import com.educat.android.educatapp.helperClasses.Utilities;
import com.educat.android.educatapp.services.UsbAndTcpService;
import com.educat.android.educatapp.setups.Setup;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.form.Spinner;

/**
 * ControlPanelActivity
 *
 * Activity with control panel for USB communication and measurements
 */
public class ControlPanelActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "ControlPanelActivity";
    private final Context context = this;
    private ProgressDialog pDialog;

    private SharedPreferences defaultSharedPreferences;

    private LinearLayout logLinearLayout, statusLinearLayout;

    private Button initButton, sendSetupButton;
    private Button startStreamButton, stopStreamButton;
    private Button startMeasurementButton, stopMeasurementButton;

    private TextView connectionStatusTextView, setupNameTextView;
    private TextView measurementNameTextView, measurementTimeTextView;
    private TextView missingCycleCountersTextView, databaseConnectionTextView;

    private ImageView connectionStatusImageView, databaseConnectionImageView;

    private static SimpleDateFormat timeFormatter;

    private ListView listView;
    private ArrayList<String> arrayList;
    private ArrayList<String> pauseArrayList;
    private StringListAdapter adapter;

    private Messenger mService = null;
    private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private int sendWhat = 0;
    private static final int SEND_JSON = 1;
    private static final int SEND_RAW = 2;

    public static final int PICK_SETUP_REQUEST = 1;

    private static final String NEW_MEASUREMENT_DIALOG = "dialogTagNewMeasurement";
    private static final String STORE_OR_DELETE_DIALOG = "dialogTagStoreOrDelete";

    private SharedPreferences setupSharedPreferences;
    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    private static final String
            USER_ID = "userId",
            SETUP_ID = "setupId",
            DESCRIPTION = "description";

    private String[] userNameArray;
    private int[] userIdArray;
    private String[] setupNameArray;
    private int[] setupIdArray;

    private Menu menu;
    private boolean showStatus = true;
    private boolean showLog = false;
    private boolean dataViewIsRunning = true;
    private boolean showDetailedLog = false;

    private int currentUserId = -1;
    private int currentUserIndex = -1;
    private int currentSetupId = -1;
    private int currentSetupIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        timeFormatter = new SimpleDateFormat("hh:mm:ss,SSS", Locale.getDefault());

        logLinearLayout = findViewById(R.id.log_linear_layout);
        statusLinearLayout = findViewById(R.id.status_linear_layout);

        if (savedInstanceState != null){
            arrayList = savedInstanceState.getStringArrayList("LOG_ARRAY");
        }
        else {
            arrayList = new ArrayList<>();
        }

        pauseArrayList = new ArrayList<>();

        adapter = new StringListAdapter(this, arrayList);

        if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)) {
            listView = findViewById(R.id.list);
            listView.setAdapter(adapter);
        }

        connectionStatusTextView = findViewById(R.id.usb_connection_textView);
        setupNameTextView = findViewById(R.id.setup_name_textView);
        measurementNameTextView = findViewById(R.id.measurement_name_textView);
        measurementTimeTextView = findViewById(R.id.measurement_time_textView);
        missingCycleCountersTextView = findViewById(R.id.missing_cycle_counters_textView);
        databaseConnectionTextView = findViewById(R.id.database_connection_textView);

        connectionStatusImageView = findViewById(R.id.usb_connection_imageView);
        databaseConnectionImageView = findViewById(R.id.database_connection_imageView);

        initButton = findViewById(R.id.init_button);
        initButton.setOnClickListener(v -> {
            // Sends the message to initialise communication (config and ask address).
            sendMessageToService(MessageCodes.USB_MSG_INIT);
        });

        sendSetupButton = findViewById(R.id.send_setup_button);
        sendSetupButton.setOnClickListener(v -> {
            sendWhat = SEND_RAW;
            pickSetup();
        });

        startStreamButton = findViewById(R.id.start_stream_button);
        startStreamButton.setOnClickListener(v -> {
            // Sends the message to start the streaming of the data.
            sendMessageToService(MessageCodes.USB_MSG_START_STREAM);
        });

        stopStreamButton = findViewById(R.id.stop_stream_button);
        stopStreamButton.setOnClickListener(v -> {
            // Sends the message to stop the streaming of the data.
            sendMessageToService(MessageCodes.USB_MSG_STOP_STREAM);
        });

        startMeasurementButton = findViewById(R.id.start_measurement_button);
        startMeasurementButton.setOnClickListener(v ->{
            getUserList();
        });

        stopMeasurementButton = findViewById(R.id.stop_current_measurement_button);
        stopMeasurementButton.setOnClickListener(v -> {
            // Sends the message to stop the measurement.
            sendMessageToService(MessageCodes.USB_MSG_STOP_MEASUREMENT);
        });

        updateUi();

        CheckIfServiceIsRunning();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putStringArrayList("LOG_ARRAY", arrayList);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        showDetailedLog = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false);
    }

    /**
     * Handler to handle the incoming data from the USB service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MessageCodes.USB_MSG_SEND_STRING:
                    String str1 = msg.getData().getString("str1");
                    addData(str1);
                    break;
                case MessageCodes.USB_MSG_CURRENT_SETUP:
                    showCurrentSetup();
                    break;
                case MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STARTED:
                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_ONLY_ENABLE_RELEVANT_BUTTONS, true)) {
                        startStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(true);
                    }
                    else {
                        startStreamButton.setEnabled(true);
                        startMeasurementButton.setEnabled(true);
                        stopMeasurementButton.setEnabled(true);
                    }
                    break;
                case MessageCodes.USB_MSG_MANUAL_MEASUREMENT_STOPPED:
                    if (defaultSharedPreferences.getBoolean(Constants.SETTING_ONLY_ENABLE_RELEVANT_BUTTONS, true)) {
                        startMeasurementButton.setEnabled(true);
                        stopMeasurementButton.setEnabled(false);
                    }
                    else {
                        startMeasurementButton.setEnabled(true);
                        stopMeasurementButton.setEnabled(true);
                    }
                    break;
                case MessageCodes.USB_MSG_STORE_OR_DELETE:
                    showStoreOrDelete();
                    break;
                case MessageCodes.USB_MSG_STATUS_UPDATE:
                    updateUi();
                    break;
                case MessageCodes.USB_MSG_UI_FEEDBACK:
                    updateFeedback(msg.getData());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void updateUi() {
        if (UsbAndTcpService.isRunning()) {
            if (defaultSharedPreferences.getBoolean(Constants.SETTING_ONLY_ENABLE_RELEVANT_BUTTONS, true)) {
                switch (UsbAndTcpService.getStatusCode()) {
                    case StatusCodes.UTS_NOT_INIT:
                        initButton.setEnabled(true);
                        sendSetupButton.setEnabled(false);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_INIT:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(false);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_ADDRESS:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(true);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_WATCHDOG:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(true);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_NO_SETUP:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(true);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_UNLOCKED_SETUP:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(true);
                        startStreamButton.setEnabled(true);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_LOCKED_SETUP:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(true);
                        startStreamButton.setEnabled(true);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(true);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_STREAM_BUSY:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(false);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(true);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    case StatusCodes.UTS_MEASUREMENT_BUSY:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(false);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(true);
                        break;
                    case StatusCodes.UTS_NEW_SETUP:
                        initButton.setEnabled(false);
                        sendSetupButton.setEnabled(true);
                        startStreamButton.setEnabled(false);
                        stopStreamButton.setEnabled(false);
                        startMeasurementButton.setEnabled(false);
                        stopMeasurementButton.setEnabled(false);
                        break;
                    default:
                        initButton.setEnabled(true);
                        sendSetupButton.setEnabled(true);
                        startStreamButton.setEnabled(true);
                        stopStreamButton.setEnabled(true);
                        startMeasurementButton.setEnabled(true);
                        stopMeasurementButton.setEnabled(true);
                        break;
                }
            }
            else {
                initButton.setEnabled(true);
                sendSetupButton.setEnabled(true);
                startStreamButton.setEnabled(true);
                stopStreamButton.setEnabled(true);
                startMeasurementButton.setEnabled(true);
                stopMeasurementButton.setEnabled(true);
            }
        }
        else {
            initButton.setEnabled(false);
            sendSetupButton.setEnabled(false);
            startStreamButton.setEnabled(false);
            stopStreamButton.setEnabled(false);
            startMeasurementButton.setEnabled(false);
            stopMeasurementButton.setEnabled(false);
        }
    }

    private void updateFeedback(Bundle b) {

        if (b.getLong("cycle_counter") > 0) {

            long totalSecs = b.getLong("cycle_counter") / 50;

            long hours = totalSecs / 3600;
            long minutes = (totalSecs % 3600) / 60;
            long seconds = totalSecs % 60;

            if (hours > 0) {
                measurementTimeTextView.setText(String.format("%02dh:%02dm:%02ds", hours, minutes, seconds));
            }
            else if (minutes > 0) {
                measurementTimeTextView.setText(String.format("%02dm:%02ds", minutes, seconds));
            }
            else {
                measurementTimeTextView.setText(String.format("%02ds", seconds));
            }
        }
        else {
            measurementTimeTextView.setText("/");
        }

        if (b.getInt("missing_cycle_counters") >= 0) {
            missingCycleCountersTextView.setText("" + b.getInt("missing_cycle_counters"));
        }
        else {
            missingCycleCountersTextView.setText("/");
        }

        switch (b.getInt("connection_status")) {
            case StatusCodes.UNKNOWN:
                connectionStatusTextView.setText("/");
                connectionStatusImageView.setVisibility(View.GONE);
                break;
            case StatusCodes.PENDING:
                connectionStatusTextView.setText("Pending");
                connectionStatusImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.yellow_light));
                connectionStatusImageView.setVisibility(View.VISIBLE);
                break;
            case StatusCodes.ACTIVE:
                connectionStatusTextView.setText("Active");
                connectionStatusImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.green_light));
                connectionStatusImageView.setVisibility(View.VISIBLE);
                break;
            case StatusCodes.INACTIVE:
                connectionStatusTextView.setText("Inactive!");
                connectionStatusImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.red_light));
                connectionStatusImageView.setVisibility(View.VISIBLE);
                break;
        }

        switch (b.getInt("database_connection")) {
            case StatusCodes.UNKNOWN:
                databaseConnectionTextView.setText("/");
                databaseConnectionImageView.setVisibility(View.GONE);
                break;
            case StatusCodes.PENDING:
                databaseConnectionTextView.setText("Pending");
                databaseConnectionImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.yellow_light));
                databaseConnectionImageView.setVisibility(View.VISIBLE);
                break;
            case StatusCodes.ACTIVE:
                databaseConnectionTextView.setText("Active");
                databaseConnectionImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.green_light));
                databaseConnectionImageView.setVisibility(View.VISIBLE);
                break;
            case StatusCodes.INACTIVE:
                databaseConnectionTextView.setText("Inactive!");
                databaseConnectionImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.red_light));
                databaseConnectionImageView.setVisibility(View.VISIBLE);
                break;
        }

        if (b.containsKey("setup_name")) {
            if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false) && b.containsKey("setup_id")) {
                setupNameTextView.setText(b.getString("setup_name") + " (" + b.getInt("setup_id") + ")");
            }
            else {
                setupNameTextView.setText(b.getString("setup_name"));
            }
        }

        if (b.containsKey("measurement_name")) {
            if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false) && b.containsKey("measurement_id") && !b.getString("measurement_name").equals("/")) {
                measurementNameTextView.setText(b.getString("measurement_name") + " (" + b.getInt("measurement_id") + ")");
            }
            else {
                measurementNameTextView.setText(b.getString("measurement_name"));
            }
        }
    }

    /**
     * Creates a new connection to the USB service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            addDataWithMillis("Attached.");
            try {
                Message msg = Message.obtain(null, MessageCodes.USB_MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                updateUi();
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            addDataWithMillis("Disconnected.");
        }
    };

    /**
     * Checks if the USB Service is running and if so,
     * it automatically binds to it.
     */
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (UsbAndTcpService.isRunning()) {
            doBindService();
            Utilities.displayToast(context, R.string.connection_to_the_dmu_is_active);
        }
        else {
            addDataWithMillis("UsbAndTcpService is not running");
            Utilities.displayToast(context, R.string.no_connection_to_the_dmu);
        }
    }

    /**
     * Binds the activity to the USB service.
     */
    private void doBindService() {
        bindService(new Intent(this, UsbAndTcpService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        addDataWithMillis("Binding.");
    }

    /**
     * Unbinds the activity to the USB service.
     */
    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MessageCodes.USB_MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            addDataWithMillis("Unbinding.");
        }
    }

    /**
     * Sends a message to the USB service.
     *
     * @param messageId ID of the message to send
     */
    private void sendMessageToService(int messageId) {
        if (mIsBound){
            if (mService != null){
                try {
                    Message msg = Message.obtain(null, messageId);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    MyLog.e(TAG, "RemoteException: %s", e);
                }
            }
        }
    }

    /**
     * Sends a message to the USB service containing an integer.
     *
     * @param messageId ID of the message to send
     * @param arg1 integer to send
     */
    private void sendMessageWithIntegerToService(int messageId, int arg1){
        if (mIsBound){
            if (mService != null){
                try {
                    Message msg = Message.obtain(null, messageId);
                    msg.arg1 = arg1;
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    MyLog.e(TAG, "RemoteException: %s", e);
                }
            }
        }
    }

    /**
     * Sends a message to the USB service containing a bundle.
     *
     * @param messageId ID of the message to send
     * @param data bundle to send
     */
    private void sendMessageWithBundleToService(int messageId, Bundle data){
        if (mIsBound){
            if (mService != null){
                try {
                    Message msg = Message.obtain(null, messageId);
                    msg.setData(data);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    MyLog.e(TAG, "RemoteException: %s", e);
                }
            }
        }
    }

    /**
     * Adds the current time to the data and adds it to the log view
     *
     * @param s data to add
     */
    private void addDataWithMillis(String s){
        addData(timeFormatter.format(new Date(System.currentTimeMillis())) + " | " +  s);
    }

    /**
     * Adds data to the log view.
     *
     * @param s data to add
     */
    private void addData(String s) {
        pauseArrayList.add(s);

        if (dataViewIsRunning){

            while (pauseArrayList.size() > 0){
                arrayList.add(pauseArrayList.remove(0));
            }

            while (arrayList.size() > 100){
                arrayList.remove(0);
            }

            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideDialog();
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            MyLog.e(TAG, "Failed to unbind from the service", t);
        }
    }

    /**
     * Display a dialog of the setup in the main board and in the memory of the Android device.
     */
    private void showCurrentSetup(){

        String jsonSetupInMemory = setupSharedPreferences.getString("setup_in_memory", "");
        int currentSetupId = setupSharedPreferences.getInt("current_setup_id", 0);
        int currentSetupVersion = setupSharedPreferences.getInt("current_setup_version", 0);

        String messageMemory = "";
        String messageCurrent = "";

        if (jsonSetupInMemory != null && !jsonSetupInMemory.equals("")){
            Setup setup = Utilities.parseJsonSetup(context, jsonSetupInMemory, jsonTypeInfoList, jsonParameterInfoList);
            if (setup != null){
                messageMemory = getString(R.string.setup_in_memory_has_id_and_version, setup.getId(), setup.getVersion());
            }
        }
        if (messageMemory.equals("")){
            messageMemory = getString(R.string.no_setup_in_memory);
        }

        if (currentSetupId > 0 && currentSetupVersion > 0){
            messageCurrent = getString(R.string.setup_in_main_board_has_id_and_version, currentSetupId, currentSetupVersion);
        }
        if (messageCurrent.equals("")){
            messageCurrent = getString(R.string.no_setup_in_main_board);
        }

        SimpleDialog.build()
                .title(R.string.setup)
                .msg(messageMemory + " " + messageCurrent)
                .show(this);
    }

    /**
     * Creates and starts an intent to select a setup.
     */
    private void pickSetup() {
        Intent pickSetupIntent = new Intent(context, SetupListActivity.class);
        startActivityForResult(pickSetupIntent, PICK_SETUP_REQUEST);
    }

    /**
     * Show a dialog with the choice to mark the measurement for deletion
     */
    private void showStoreOrDelete(){
        SimpleDialog.build()
                .title(getString(R.string.measurement_stopped))
                .msg(getString(R.string.do_you_want_to_keep_the_measurement))
                .cancelable(false)
                .pos(R.string.yes)
                .neg(R.string.no)
                .show(this, STORE_OR_DELETE_DIALOG);
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (NEW_MEASUREMENT_DIALOG.equals(dialogTag)){
            switch (which){
                case BUTTON_POSITIVE:
                    int userIndex = extras.getInt(USER_ID);
                    int setupIndex = extras.getInt(SETUP_ID);
                    String description = extras.getString(DESCRIPTION);

                    // TODO userIdArray and setupIdArray are null after orientation change!
                    int selectedUserId = userIdArray[userIndex];
                    int selectedSetupId = setupIdArray[setupIndex];

//                    Utilities.displayToast(context, String.format("User ID: %s, Setup ID: %s, Description: %s", selectedUserId, selectedSetupId, description));

                    Bundle data = new Bundle();
                    data.putInt("user_id", selectedUserId);
                    data.putInt("setup_id", selectedSetupId);
                    data.putString("description", description);

                    sendMessageWithBundleToService(MessageCodes.USB_MSG_START_MEASUREMENT, data);

                    return true;

                case BUTTON_NEGATIVE:
                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }
        if (STORE_OR_DELETE_DIALOG.equals(dialogTag)){
            switch (which){
                case BUTTON_POSITIVE:
                    sendMessageWithIntegerToService(MessageCodes.USB_MSG_STORE_OR_DELETE, 1);
                    return true;

                case BUTTON_NEGATIVE:
                    sendMessageWithIntegerToService(MessageCodes.USB_MSG_STORE_OR_DELETE, -1);
                    return true;

                case BUTTON_NEUTRAL:
                case CANCELED:
                    return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){

            if (requestCode == PICK_SETUP_REQUEST){
                setupSharedPreferences.edit().putString("setup_in_memory", data.getStringExtra("json_setup")).apply();

                switch (sendWhat){
                    case SEND_JSON:
                        sendWhat = 0;

                        // Sends the message to send the json of the setup.
                        MyLog.d(TAG, "sendJson");
                        sendMessageToService(MessageCodes.USB_MSG_SETUP_JSON);
                        break;

                    case SEND_RAW:
                        sendWhat = 0;

                        // Sends the message to send the raw of the setup.
                        MyLog.d(TAG, "sendRaw");
                        sendMessageToService(MessageCodes.USB_MSG_SETUP_RAW);
                        break;
                }
            }
        }
    }

    /**
     * Creates and executes a request to get a list of the users (developers only).
     */
    private void getUserList(){
        String tag_string_req = "user_list";

        pDialog.setMessage(getString(R.string.getting_user_list_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    parseJsonUserList(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            Utilities.displayVolleyError(context, e);
            hideDialog();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");
                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private void parseJsonUserList(String jsonUserList){

        currentUserId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1);

        try {
            if (!jsonUserList.equals("")){
                JSONObject jObjUserList = new JSONObject(jsonUserList);

                JSONArray jUsersArray = jObjUserList.getJSONArray("data");

                userNameArray = new String[jUsersArray.length()];
                userIdArray = new int[jUsersArray.length()];

                for (int i = 0; i < jUsersArray.length(); i++){

                    JSONObject currentUser = jUsersArray.getJSONObject(i);

                    userIdArray[i] = currentUser.getInt("id");
                    userNameArray[i] = currentUser.getString("username");

                    if (currentUserId != -1 && currentUserId == userIdArray[i]){
                        currentUserIndex = i;
                    }
                }

                getSetupList();
            }
            else {
                hideDialog();
            }
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }
    }

    /**
     * Creates and executes a request to get the list of setups.
     */
    private void getSetupList(){
        String tag_string_req = "get_setup_list";

        pDialog.setMessage(getString(R.string.getting_setups_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    parseJsonSetupList(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            Utilities.displayVolleyError(context, e);
            hideDialog();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
                headers.put("X-GET-Draft", "0");

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Parses the JSON containing the list of setups.
     *
     * @param jsonSetupList JSON containing the list of setups
     */
    private void parseJsonSetupList(String jsonSetupList){

        Setup setupInMemory = Utilities.parseJsonSetup(context, setupSharedPreferences.getString("setup_in_memory", ""), jsonTypeInfoList, jsonParameterInfoList);
        if (setupInMemory != null) {
            currentSetupId = setupInMemory.getId();
        }

        try {
            JSONObject jObj = new JSONObject(jsonSetupList);

            JSONArray jSetupArray = jObj.getJSONArray("data");

            ArrayList<Setup> setupArrayList = new ArrayList<>();

            for (int i = 0; i < jSetupArray.length(); i++){

                JSONObject jCurrentSetup = jSetupArray.getJSONObject(i);

                int setupId = jCurrentSetup.getInt("id");
                int setupGroupId = jCurrentSetup.getInt("setup_group_id");
                String setupName = jCurrentSetup.getString("name_en");
                int setupHardwareIdentifier = jCurrentSetup.getInt("hw_identifier");
                int setupVersion = jCurrentSetup.getInt("version");
                boolean setupLocked = jCurrentSetup.getInt("locked") == 1;

                if (setupLocked){
                    setupArrayList.add(new Setup(setupId, setupGroupId, setupName, setupHardwareIdentifier, setupVersion, setupLocked));
                }
            }

            setupNameArray = new String[setupArrayList.size()];
            setupIdArray = new int[setupArrayList.size()];

            for (int i = 0; i < setupArrayList.size(); i++){
                Setup currentSetup = setupArrayList.get(i);
                setupNameArray[i] = currentSetup.getName();
                setupIdArray[i] = currentSetup.getId();

                if (currentSetupId != -1 && currentSetupId == setupIdArray[i]){
                    currentSetupIndex = i;
                }
            }

            hideDialog();

            showForm();
        }
        catch (JSONException e){
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            hideDialog();
        }
    }

    private void showForm(){
        SimpleFormDialog.build()
                .title(getString(R.string.start_measurement))
                .msg(getString(R.string.create_a_new_measuremet_before_starting_it))
                .fields(
                        Spinner.plain(USER_ID).label(getString(R.string.select_a_user_colon)).items(userNameArray).placeholder(getString(R.string.select_ellipsis)).preset(currentUserIndex).required(),
                        Spinner.plain(SETUP_ID).label(getString(R.string.select_a_setup_colon)).items(setupNameArray).placeholder(getString(R.string.select_ellipsis)).preset(currentSetupIndex).required(),
                        Input.plain(DESCRIPTION).hint(getString(R.string.description)).inputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD).required()
                )
                .autofocus(false)
                .cancelable(false)
                .pos(R.string.ok)
                .neut(R.string.cancel)
                .show(this, NEW_MEASUREMENT_DIALOG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_control_panel, menu);
        this.menu = menu;

        if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)) {
            this.menu.findItem(R.id.action_show_status).setChecked(showStatus);
            this.menu.findItem(R.id.action_show_log).setChecked(showLog);
            this.menu.findItem(R.id.action_pause_resume).setChecked(dataViewIsRunning);
            this.menu.findItem(R.id.action_show_detailed_log).setChecked(showDetailedLog);
        }
        else {
            this.menu.findItem(R.id.action_show_status).setVisible(false);
            this.menu.findItem(R.id.action_show_log).setVisible(false);
            this.menu.findItem(R.id.action_pause_resume).setVisible(false);
            this.menu.findItem(R.id.action_show_detailed_log).setVisible(false);
        }

        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_show_status) {
            if (showStatus) {
                showStatus = false;
                statusLinearLayout.setVisibility(View.GONE);
                item.setChecked(false);
            }
            else {
                showStatus = true;
                statusLinearLayout.setVisibility(View.VISIBLE);
                item.setChecked(true);
            }
        }
        else if (itemId == R.id.action_show_log) {
            if (showLog) {
                showLog = false;
                logLinearLayout.setVisibility(View.GONE);
                item.setChecked(false);
            }
            else {
                showLog = true;
                logLinearLayout.setVisibility(View.VISIBLE);
                item.setChecked(true);
            }
        }
        else if (itemId == R.id.action_pause_resume) {
            if (dataViewIsRunning) {
                dataViewIsRunning = false;
                setOptionTitle(R.id.action_pause_resume, "Resume log");
                item.setChecked(false);
            } else {
                dataViewIsRunning = true;
                setOptionTitle(R.id.action_pause_resume, "Pause log");
                adapter.notifyDataSetChanged();
                item.setChecked(true);
            }
        } else if (itemId == R.id.action_show_detailed_log) {
            if (showDetailedLog) {
                showDetailedLog = false;
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_SHOW_DETAILED_DATA, false).apply();
                item.setChecked(false);
            } else {
                showDetailedLog = true;
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_SHOW_DETAILED_DATA, true).apply();
                item.setChecked(true);
            }
        }
        return true;
    }

    /**
     * Dynamically change the title of an option in the Action Bar Menu.
     *
     * @param id of the option of which to change the title
     * @param title new title for the option
     */
    private void setOptionTitle(int id, String title) {
        if (menu != null){
            MenuItem item = menu.findItem(id);
            item.setTitle(title);
        }
        else {
            MyLog.e(TAG, "Menu is null!");
        }
    }

    /**
     * Dynamically change the icon of an option in the Action Bar Menu.
     *
     * @param id of the option of which to change the icon
     * @param iconRes resource id of the new icon
     */
    private void setOptionIcon(int id, int iconRes) {
        if (menu != null){
            MenuItem item = menu.findItem(id);
            item.setIcon(iconRes);
        }
        else {
            MyLog.e(TAG, "Menu is null!");
        }
    }

    /**
     * Shows the dialog when it's not already showing.
     */
    private void showDialog() {
        if (!isFinishing() && pDialog != null && !pDialog.isShowing())
            pDialog.show();
    }

    /**
     * Hides the dialog when it's showing.
     */
    private void hideDialog() {
        if (pDialog != null && pDialog.isShowing())
            pDialog.dismiss();
    }
}