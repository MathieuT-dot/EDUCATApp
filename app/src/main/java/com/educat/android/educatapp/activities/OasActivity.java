package com.educat.android.educatapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.educat.android.educatapp.R;
import com.educat.android.educatapp.helperClasses.AppController;
import com.educat.android.educatapp.helperClasses.Constants;
import com.educat.android.educatapp.helperClasses.DynamicRadar;
import com.educat.android.educatapp.helperClasses.MessageCodes;
import com.educat.android.educatapp.helperClasses.MyLog;
import com.educat.android.educatapp.helperClasses.Utilities;
import com.educat.android.educatapp.services.UsbAndTcpService;
import com.educat.android.educatapp.setups.Setup;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittingClasses.SubmittingQuestionnaire;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import eltos.simpledialogfragment.SimpleDialog;

/**
 * OasActivity
 *
 * This activity displays the data from the distance sensors, received from the DMU,
 * the radar now shows the distances in a continuous way.
 */
public class OasActivity extends AppCompatActivity implements SimpleDialog.OnDialogResultListener {

    private static final String TAG = "OasActivity";
    private final Context context = this;

    private SharedPreferences defaultSharedPreferences;
    private SharedPreferences.Editor defaultEditor;
    private SharedPreferences questionnairesSharedPreferences;
    private SharedPreferences submissionsSharedPreferences;
    private SharedPreferences.Editor submissionsEditor;

    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    private DynamicRadar radar;

    private Button dailyDiaryButton;

    private Button buzzerButton;
    private Button hapticButton;
    private Button visualButton;

    private boolean buzzerBoolean = false;
    private boolean hapticBoolean = false;
    private boolean visualBoolean = false;
    private byte sensorsStatus;

    private PointF lastTouchLocation;

    private Boolean savedAnswersPresent = false;
    private SubmittingQuestionnaire savedAnswers;

    private static final String OPEN_QUESTIONNAIRE_DIALOG_ONLINE = "dialogTagOpenQuestionnaireOnline";
    private static final String OPEN_QUESTIONNAIRE_DIALOG_OFFLINE = "dialogTagOpenQuestionnaireOffline";

    private Messenger mService = null;
    private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private SimpleDateFormat sdfDateAndTime;
    private SimpleDateFormat sdfDateAndTimeLaravel;

    private String stringJsonResponse;
    private Long latestDailyDiary = null;
    private int currentColorFilter;
    private static final int DELAY_IN_MILLIS = 6 * 60 * 60 * 1000;
    private static final int GRADIENT_IN_MILLIS = 2 * 60 * 60 * 1000;
//    private static final int DELAY_IN_MILLIS = 10 * 60 * 1000;
//    private static final int GRADIENT_IN_MILLIS = 30 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oas);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultEditor = defaultSharedPreferences.edit();
        defaultEditor.apply();

        questionnairesSharedPreferences = getSharedPreferences(Constants.QUESTIONNAIRES_DATA, MODE_PRIVATE);

        submissionsSharedPreferences = getSharedPreferences(Constants.SUBMISSIONS_DATA, MODE_PRIVATE);
        submissionsEditor = submissionsSharedPreferences.edit();
        submissionsEditor.apply();

        sdfDateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdfDateAndTimeLaravel = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdfDateAndTimeLaravel.setTimeZone(TimeZone.getTimeZone("UTC"));

        SharedPreferences setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        radar = findViewById(R.id.radar_view);

        String jsonSetupInMemory = setupSharedPreferences.getString("setup_in_memory", "");
        if (jsonSetupInMemory != null && !jsonSetupInMemory.equals("")){
            Setup memorySetup = Utilities.parseJsonSetup(context, jsonSetupInMemory, jsonTypeInfoList, jsonParameterInfoList);
            if (memorySetup != null){

                sensorsStatus = 0x00;
                if (UsbAndTcpService.isRunning()) {
                    sensorsStatus = UsbAndTcpService.getSensorsStatus();
                }

                radar.initOas(memorySetup);
                radar.updateActiveSensors(0, 0, sensorsStatus);
                radar.setOnTouchListener(radarTouchListener);
                radar.setOnClickListener(radarClickListener);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        radar.updateActiveSensors(0, 0, sensorsStatus);
                        radar.postInvalidate();;
                    }
                }, 250);
            }
        }

        currentColorFilter = getResources().getColor(R.color.default_button_color);

        dailyDiaryButton =findViewById(R.id.daily_diary_button);
        dailyDiaryButton.setOnClickListener(v -> {
            dailyDiaryButton.setEnabled(false);

            final Intent intent = new Intent(context, QuestionPerQuestionActivity.class);
            intent.putExtra("QUESTIONNAIRE_ID", "54");

            Bundle bundle = new Bundle();
            bundle.putParcelable("INTENT", intent);

            if (Utilities.checkNetworkConnection(context)){
                if (savedAnswersPresent && savedAnswers.getId() == 54 ){
                    SimpleDialog.build()
                            .title(R.string.open_questionnaire)
                            .msg(R.string.do_you_want_to_load_answers)
                            .pos(R.string.yes)
                            .neg(R.string.no)
                            .extra(bundle)
                            .show(OasActivity.this, OPEN_QUESTIONNAIRE_DIALOG_ONLINE);
                }
                else {
                    intent.putExtra("LOAD_SAVED_ANSWERS", false);
                    intent.putExtra("DOWNLOAD_JSON", true);
                    startActivity(intent);
                }
            }
            else {
                if (questionnairesSharedPreferences.contains("questionnaire_" + "54")) {
                    if (savedAnswersPresent && savedAnswers.getId() == 54 ){
                        SimpleDialog.build()
                                .title(R.string.open_questionnaire)
                                .msg(R.string.do_you_want_to_load_answers)
                                .pos(R.string.yes)
                                .neg(R.string.no)
                                .extra(bundle)
                                .show(OasActivity.this, OPEN_QUESTIONNAIRE_DIALOG_OFFLINE);
                    }
                    else {
                        intent.putExtra("LOAD_SAVED_ANSWERS", false);
                        intent.putExtra("DOWNLOAD_JSON", false);
                        startActivity(intent);
                    }

                }
                else {
                    dailyDiaryButton.setEnabled(true);
                    Utilities.displayToast(context, getString(R.string.no_internet_access));
                }
            }
        });

        buzzerButton = findViewById(R.id.buzzer_button);
        buzzerButton.setOnClickListener(v ->
        {
            buzzerBoolean = !buzzerBoolean;
            updateBuzzer();
        });

        hapticButton = findViewById(R.id.haptic_button);
        hapticButton.setOnClickListener(v ->
        {
            hapticBoolean = !hapticBoolean;
            updateHaptic();
        });

        visualButton = findViewById(R.id.visual_button);
        visualButton.setOnClickListener(v ->
        {
            visualBoolean = !visualBoolean;
            updateVisual();
        });

        CheckIfServiceIsRunning();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (questionnairesSharedPreferences.contains("answers_json")){
            savedAnswersPresent = true;
            MyLog.d(TAG, "Getting the saved answers from SharedPreferences");
            String answers_json = questionnairesSharedPreferences.getString("answers_json", "null");
            savedAnswers = (new Gson()).fromJson(answers_json, SubmittingQuestionnaire.class);
        }
        else {
            savedAnswersPresent = false;
        }

        buzzerBoolean = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_OAS_BUZZER, false);
        updateBuzzer();

        hapticBoolean = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_OAS_HAPTIC, false);
        updateHaptic();

        visualBoolean = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.SETTING_OAS_VISUAL, false);
        updateVisual();

        dailyDiaryButton.setEnabled(true);

        // TODO add alternatives (offline and submissions is missing, offline and submissions waiting for upload, ...)
        if (Utilities.checkNetworkConnection(context)){
            getSubmittedQuestionnaires();
        }
        else {
            stringJsonResponse = submissionsSharedPreferences.getString("submissions_list_json", "");
            if (stringJsonResponse != null && !stringJsonResponse.equals("")){
                getLatestDailyDiary(stringJsonResponse);
            }
        }
    }

    View.OnTouchListener radarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            // save the X,Y coordinates
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                lastTouchLocation = new PointF(event.getX(), event.getY());
            }

            // let the touch event pass on to whoever needs it
            return false;
        }
    };

    View.OnClickListener radarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (UsbAndTcpService.isRunning()) {
                sensorsStatus = UsbAndTcpService.getSensorsStatus();
            }
            sensorsStatus = radar.updateActiveSensors(lastTouchLocation.x, lastTouchLocation.y, sensorsStatus);
            if (UsbAndTcpService.isRunning()) {
                UsbAndTcpService.setSensorsStatus(sensorsStatus);
            }
        }
    };

    /**
     * Updates the state of the buzzer alert.
     * It enables the buzzer alert, updates the color of the button to
     * reflect the current state and alerts the user about the volume.
     */
    private void updateBuzzer() {
        if (buzzerBoolean)
        {
            buzzerButton.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC);
            buzzerButton.setText(String.format("%s%s", getString(R.string.buzzer_colon), getString(R.string.active)));
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_OAS_BUZZER, true).apply();

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int currentVolumePercentage = 100 * currentVolume/maxVolume;

            MyLog.d("VOLUME", "currentVolume: " + currentVolume + ", maxVolume: " + maxVolume + ", currentVolumePercentage: " + currentVolumePercentage);

            SimpleDialog.build()
                    .title(R.string.warning)
                    .msg(getString(R.string.alarm_volume_warning, currentVolumePercentage))
                    .show(this);
        }
        else {
            buzzerButton.getBackground().clearColorFilter();
            buzzerButton.setText(String.format("%s%s", getString(R.string.buzzer_colon), getString(R.string.inactive)));
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_OAS_BUZZER, false).apply();
        }
    }

    /**
     * Updates the state of the haptic alert.
     * It enables the haptic alert and updates the color of the button to
     * reflect the current state.
     */
    private void updateHaptic() {
        if (hapticBoolean)
        {
            hapticButton.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC);
            hapticButton.setText(String.format("%s%s", getString(R.string.haptic_colon), getString(R.string.active)));
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_OAS_HAPTIC, true).apply();
        }
        else {
            hapticButton.getBackground().clearColorFilter();
            hapticButton.setText(String.format("%s%s", getString(R.string.haptic_colon), getString(R.string.inactive)));
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_OAS_HAPTIC, false).apply();
        }
    }

    /**
     * Updates the state of the visual alert.
     * It enables the visual alert and updates the color of the button to
     * reflect the current state.
     */
    private void updateVisual() {
        if (visualBoolean)
        {
            visualButton.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.SRC);
            visualButton.setText(String.format("%s%s", getString(R.string.visual_colon), getString(R.string.active)));
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_OAS_VISUAL, true).apply();
            radar.setVisibility(View.VISIBLE);
        }
        else {
            visualButton.getBackground().clearColorFilter();
            visualButton.setText(String.format("%s%s", getString(R.string.visual_colon), getString(R.string.inactive)));
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Constants.SETTING_OAS_VISUAL, false).apply();
            radar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            doUnbindService();
        }
        catch (Throwable t) {
            MyLog.e(TAG, "Failed to unbind from the service", t);
        }
    }

    /**
     * Handler to handle the incoming data from the USB service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MessageCodes.USB_MSG_SEND_DATA) {
                ArrayList<BigDecimal> bigDecimalArrayList = (ArrayList<BigDecimal>) msg.getData().getSerializable("stream_data");
                if (bigDecimalArrayList != null && bigDecimalArrayList.size() > 0){
                    radar.addData(bigDecimalArrayList);
                    checkLatestDailyDiary();
                }
            } else {
                super.handleMessage(msg);
            }
        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {

        if (OPEN_QUESTIONNAIRE_DIALOG_ONLINE.equals(dialogTag)){
            Intent intent = extras.getParcelable("INTENT");
            if (intent != null){
                switch (which){
                    case BUTTON_POSITIVE:
                        intent.putExtra("LOAD_SAVED_ANSWERS", true);
                        intent.putExtra("DOWNLOAD_JSON", true);
                        startActivity(intent);
                        return true;
                    case BUTTON_NEGATIVE:
                        intent.putExtra("LOAD_SAVED_ANSWERS", false);
                        intent.putExtra("DOWNLOAD_JSON", true);
                        startActivity(intent);
                        return true;
                }
            }
        }

        if (OPEN_QUESTIONNAIRE_DIALOG_OFFLINE.equals(dialogTag)){
            Intent intent = extras.getParcelable("INTENT");
            if (intent != null){
                switch (which){
                    case BUTTON_POSITIVE:
                        intent.putExtra("LOAD_SAVED_ANSWERS", true);
                        intent.putExtra("DOWNLOAD_JSON", false);
                        startActivity(intent);
                        return true;
                    case BUTTON_NEGATIVE:
                        intent.putExtra("LOAD_SAVED_ANSWERS", false);
                        intent.putExtra("DOWNLOAD_JSON", false);
                        startActivity(intent);
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates a new connection to the USB service.
     */
    private ServiceConnection mConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
//            displayToast(context, "USB service attached");
            try {
                Message msg = Message.obtain(null, MessageCodes.USB_MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
//            displayToast(context, "USB service disconnected");
        }
    };

    /**
     * Creates and executes a request to get a list of the submitted questionnaires.
     */
    private void getSubmittedQuestionnaires(){
        String tag_string_req = "sbm_list_questionnaire";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    submissionsEditor.putString("submissions_list_json", response).apply();

                    stringJsonResponse = response;

                    getLatestDailyDiary(stringJsonResponse);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
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
     * Parses the JSON containing the list of submitted questionnaires and if not null, also the
     * list of users.
     * Also displays the list of submitted questionnaires after a successful parse.
     *
     * @param response JSON containing the list of submitted questionnaires
     */
    private ArrayList<SubmittedQuestionnaire> parseJsonResponse(String response) {
        try {
            JSONObject jObjSubmissions = new JSONObject(response);

            JSONArray jSubmissionsArray = jObjSubmissions.getJSONArray("data");

            final ArrayList<SubmittedQuestionnaire> submissionsArrayList = new ArrayList<>();

            for (int i = 0; i < jSubmissionsArray.length(); i++){

                JSONObject currentSubmission = jSubmissionsArray.getJSONObject(i);

                int id = currentSubmission.getInt("id");
                int questionnaireId = currentSubmission.getInt("questionnaire_id");
                int userId = currentSubmission.getInt("user_id");

                String createdAt = currentSubmission.getString("created_at");
                String updatedAt = currentSubmission.getString("updated_at");

                long unixMillis = 0L;
                long origMillis = 0L;
                if (!createdAt.equals("null")) {
                    try {
                        unixMillis = sdfDateAndTimeLaravel.parse(createdAt).getTime();
                        origMillis = unixMillis;
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                long lastEditMillis = 0L;
                if (!updatedAt.equals("null")) {
                    try {
                        lastEditMillis = sdfDateAndTimeLaravel.parse(updatedAt).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (!createdAt.equals("null")) {
                    try {
                        createdAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(createdAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (!updatedAt.equals("null")) {
                    try {
                        updatedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(updatedAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String startedAt = currentSubmission.getString("started_at");
                if (!startedAt.equals("null")) {
                    try {
                        startedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(startedAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String finishedAt = currentSubmission.getString("finished_at");
                if (!finishedAt.equals("null")) {
                    try {
                        finishedAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(finishedAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String deletedAt = currentSubmission.getString("deleted_at");
                String createdBy = currentSubmission.getString("created_by");
                String updatedBy = currentSubmission.getString("updated_by");
                String deletedBy = currentSubmission.getString("deleted_by");

                JSONObject jQuestionnaire = currentSubmission.getJSONObject("questionnaire");

                int questionnaireGroupId = jQuestionnaire.getInt("questionnaire_group_id");
                int version = jQuestionnaire.getInt("version");

                String titleEn = jQuestionnaire.getString("name_en");
                String titleNl = jQuestionnaire.getString("name_nl");
                String titleFr = jQuestionnaire.getString("name_fr");
                String descriptionEn = jQuestionnaire.getString("description_en");
                String descriptionNl = jQuestionnaire.getString("description_nl");
                String descriptionFr = jQuestionnaire.getString("description_fr");

                String title;
                String description;

                switch (Locale.getDefault().getLanguage()){
                    case "nl":
                        title = titleNl;
                        description = descriptionNl;
                        break;

                    case "fr":
                        title = titleFr;
                        description = descriptionFr;
                        break;

                    default:
                        title = titleEn;
                        description = descriptionEn;
                }

                if ( getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1) == userId || getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX, false))
                {
                    if ((questionnaireId != 42 && questionnaireId != 44 && questionnaireId != 49)  || getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)){
                        submissionsArrayList.add(new SubmittedQuestionnaire(id, questionnaireId, questionnaireGroupId, version, title, description, userId, createdAt, updatedAt, unixMillis, origMillis, lastEditMillis));
                    }
                }
            }

            Collections.sort(submissionsArrayList, (s1, s2) -> {

                try {
                    Date mDate1 = sdfDateAndTime.parse(s1.getDate());
                    Date mDate2 = sdfDateAndTime.parse(s2.getDate());
                    return mDate2.compareTo(mDate1);
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

                return 0;
            });

            return submissionsArrayList;

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        return null;
    }

    /**
     * Iterates through the submitted questionnaires array list and gets the time from the latest
     * daily diary.
     *
     * @param response string that contains all the submitted questionnaires
     */
    private void getLatestDailyDiary(String response){

        ArrayList<SubmittedQuestionnaire> submittedQuestionnaireArrayList = parseJsonResponse(response);

        if (submittedQuestionnaireArrayList != null && submittedQuestionnaireArrayList.size() > 0){

            for (int i = 0; i < submittedQuestionnaireArrayList.size(); i++){

                SubmittedQuestionnaire submittedQuestionnaire = submittedQuestionnaireArrayList.get(i);

                if (submittedQuestionnaire.getQnrId() == 54 || submittedQuestionnaire.getQnrId() == 67){

                    if (submittedQuestionnaire.getUserId() == getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1)){

                        latestDailyDiary = submittedQuestionnaire.getUnixMillis();

                        break;
                    }
                }
            }
        }

        checkLatestDailyDiary();
    }

    /**
     * Checks the time at which the latest daily diary was filled in and changes the color of
     * the daily diary button based on that.
     */
    private void checkLatestDailyDiary(){

        int colorFilter;

        if (latestDailyDiary != null){

            long currentMillis = System.currentTimeMillis();
            long difference = currentMillis - latestDailyDiary - DELAY_IN_MILLIS;

            if (difference < 0){
                colorFilter = getResources().getColor(R.color.default_button_color);
            }
            else if (difference < GRADIENT_IN_MILLIS){
                colorFilter = (int) Utilities.evaluateArgb((float) difference / (float) GRADIENT_IN_MILLIS, getResources().getColor(R.color.default_button_color), getResources().getColor(R.color.yellow));
            }
            else {
                colorFilter = getResources().getColor(R.color.yellow);
                dailyDiaryButton.getBackground().setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC);
            }
        }
        else {
            colorFilter = getResources().getColor(R.color.yellow);
        }

        if (currentColorFilter != colorFilter){
            currentColorFilter = colorFilter;
            dailyDiaryButton.getBackground().setColorFilter(currentColorFilter, PorterDuff.Mode.SRC);
        }
    }

    /**
     * Checks if the USB Service is running and if so,
     * it automatically binds to it.
     */
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (UsbAndTcpService.isRunning()) {
            doBindService();
            Utilities.displayToast(context, R.string.connection_to_the_dmu_is_active);

            if (UsbAndTcpService.isMeasurementRunning()){
                setTitle(String.format("%s (meas. ID: %s)", getTitle(), UsbAndTcpService.getMeasurementId()));
            }
        }
        else {
            Utilities.displayToast(context, R.string.no_connection_to_the_dmu);
        }
    }

    /**
     * Binds the activity to the USB Service
     */
    private void doBindService() {
        bindService(new Intent(this, UsbAndTcpService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
//        displayToast(context, "USB service binding");
    }

    /**
     * Unbinds the activity to the USB Service
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
//            displayToast(context, "USB service unbinding");
        }
    }

    /**
     * Lowers the brightness to the minimum of the device.
     */
    private void screenOff(){
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.screenBrightness = 0;
        getWindow().setAttributes(params);
    }

    /**
     * Resets the brightness of the device to the last value.
     */
    private void screenOn(){
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.screenBrightness = -1;
        getWindow().setAttributes(params);
    }
}
