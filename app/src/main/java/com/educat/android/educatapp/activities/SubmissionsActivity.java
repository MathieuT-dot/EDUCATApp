package com.educat.android.educatapp.activities;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.educat.android.educatapp.R;
import com.educat.android.educatapp.adapters.SubmissionsAdapter;
import com.educat.android.educatapp.helperClasses.AppController;
import com.educat.android.educatapp.helperClasses.Constants;
import com.educat.android.educatapp.helperClasses.MyLog;
import com.educat.android.educatapp.helperClasses.NewJsonLib;
import com.educat.android.educatapp.helperClasses.Utilities;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Answer;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Question;
import com.kuleuven.android.kuleuvenlibrary.getQuestionnaireClasses.Questionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaire;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireAnswer;
import com.kuleuven.android.kuleuvenlibrary.submittedQuestionnaireClasses.SubmittedQuestionnaireQuestion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

import crl.android.pdfwriter.PDFWriter;
import crl.android.pdfwriter.StandardFonts;

/**
 * SubmittedQuestionnairesActivity
 *
 * Activity to display submitted questionnaires in a ListView
 */
public class SubmissionsActivity extends AppCompatActivity {

    private static final String TAG = "SubmissionsActivity";
    private final Context context = this;

    private ProgressDialog pDialog;

    private ListView submittedQuestionnairesListView;
    private TextView backgroundTextView;

    private String stringJsonResponse = "";
    private String stringJsonUserList = "";

    private SharedPreferences submissionsSharedPreferences;
    private SharedPreferences.Editor submissionsEditor;

    private static final int LAYOUT_LOADING = 1;
    private static final int LAYOUT_NO_INTERNET = 2;
    private static final int TOAST_NO_INTERNET = 3;
    private static final int TOAST_OFFLINE_LIST = 4;
    private static final int LAYOUT_LIST = 5;
    private static final int LAYOUT_NO_OFFLINE_DATA = 6;
    private static final int NO_SUBMISSIONS = 7;

    private Date filterStartDate;
    private boolean filterStartDateIsEnabled = false;
    private Date filterEndDate;
    private boolean filterEndDateIsEnabled = false;
    private int filterQuestionnaireId = -1;
    private boolean filterQuestionnaireIdIsEnabled = false;
    private String filterUserName = "";
    private boolean filterUsernameIsEnabled = false;

    private SimpleDateFormat sdfDateAndTime;
    private SimpleDateFormat sdfDateAndTimeLaravel;

    private ArrayList<SubmittedQuestionnaire> submissionsArrayList;
    private int downloadPdfIndex = 0;
    private int submittedQuestionnaireId;
    private String username = "";
    private SubmittedQuestionnaire submittedQuestionnaire;
    private Questionnaire questionnaire;
    private boolean alternateNumbering = false;

    private PDFWriter mPDFWriter;
    private int defaultLeftMargin = 50;
    private int questionLeftMargin = 70;
    private int answerLeftMargin = 90;
    private int currentTopPositionFromBottom = 772;
    private int defaultFontSize = 12;
    private int titleFontSize = 20;
    private int defaultInterlinearDistance = 10;
    private int smallInterlinearDistance = 5;
    private int bigInterlinearDistance = 25;
    private static final int A4_WIDTH = 595;
    private static final int A4_HEIGHT = 842;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitted_questionnaires);

        submissionsSharedPreferences = getSharedPreferences(Constants.SUBMISSIONS_DATA, MODE_PRIVATE);
        submissionsEditor = submissionsSharedPreferences.edit();
        submissionsEditor.apply();

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        sdfDateAndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdfDateAndTimeLaravel = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdfDateAndTimeLaravel.setTimeZone(TimeZone.getTimeZone("UTC"));

        submittedQuestionnairesListView = findViewById(R.id.submitted_questionnaires_list_view);
        backgroundTextView = findViewById(R.id.background_text_view);

        if (getIntent().getBooleanExtra("FILTER", false))
        {
            filterStartDate = (Date) getIntent().getSerializableExtra("START_DATE");
            filterStartDateIsEnabled = getIntent().getBooleanExtra("START_DATE_IS_ENABLED", false);
            filterEndDate = (Date) getIntent().getSerializableExtra("END_DATE");
            filterEndDateIsEnabled = getIntent().getBooleanExtra("END_DATE_IS_ENABLED", false);
            filterQuestionnaireId = getIntent().getIntExtra("QUESTIONNAIRE_ID", -1);
            filterQuestionnaireIdIsEnabled = getIntent().getBooleanExtra("QUESTIONNAIRE_IS_ENABLED", false);
            filterUserName = getIntent().getStringExtra("USERNAME");
            filterUsernameIsEnabled = getIntent().getBooleanExtra("USERNAME_IS_ENABLED", false);
        }

        if (savedInstanceState != null)
        {
            if (submissionsSharedPreferences.contains(Constants.API_SUBMISSIONS)){
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
            }
            if (submissionsSharedPreferences.contains(Constants.API_USERS)){
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS, "");
            }

            parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
        }
        else {
            if (Utilities.checkNetworkConnection(context)){
                handleLayoutChanges(LAYOUT_LOADING);
                getSubmittedQuestionnaires();
            }
            else {
                handleLayoutChanges(LAYOUT_LOADING);
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS,"");
                if (stringJsonResponse != null && !stringJsonResponse.equals("")){
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                    handleLayoutChanges(TOAST_OFFLINE_LIST);
                }
                else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        submittedQuestionnairesListView.setEnabled(true);

        if (getIntent().getBooleanExtra("EDIT_WAS_MADE",false)){
            if (Utilities.checkNetworkConnection(context)){
                handleLayoutChanges(LAYOUT_LOADING);
                getSubmittedQuestionnaires();
            }
            else {
                handleLayoutChanges(LAYOUT_LOADING);
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "null");
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS,"");
                if (stringJsonResponse != null && !stringJsonResponse.equals("null")){
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                    handleLayoutChanges(TOAST_OFFLINE_LIST);
                }
                else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
    }

    /**
     * Handles the layout changes depending on the provided layout ID.
     *
     * @param layout id determining the layout
     */
    private void handleLayoutChanges(int layout) {
        switch (layout) {
            case LAYOUT_LOADING:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.loading_ellipsis);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_INTERNET:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_internet_access);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case TOAST_NO_INTERNET:
                Utilities.displayToast(context, getString(R.string.no_internet_access));
                break;

            case TOAST_OFFLINE_LIST:
                Utilities.displayToast(context, getString(R.string.no_internet_access_offline_data));
                break;

            case LAYOUT_LIST:
                backgroundTextView.setVisibility(View.GONE);
                submittedQuestionnairesListView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_OFFLINE_DATA:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_offline_data);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case NO_SUBMISSIONS:
                submittedQuestionnairesListView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_submissions);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Creates and executes a request to get a list of the submitted questionnaires.
     */
    private void getSubmittedQuestionnaires()
    {
        String tag_string_req = "sbm_list_questionnaire";

        pDialog.setMessage(getString(R.string.getting_submitted_questionnaires_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    submissionsEditor.putString(Constants.API_SUBMISSIONS, response).apply();
                    stringJsonResponse = response;

                    if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_USER_INDEX, false)){
                        getUserList();
                    }
                    else {
                        parseAndDisplayJsonResponse(stringJsonResponse, "");
                    }
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

            handleLayoutChanges(LAYOUT_LOADING);
            stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
            stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS,"");
            if (stringJsonResponse != null && !stringJsonResponse.equals("")){
                parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                hideDialog();
                Utilities.displayVolleyError(context, e, Constants.VOLLEY_ERRORS.SHOWING_OFFLINE_DATA);
            }
            else {
                handleLayoutChanges(LAYOUT_NO_INTERNET);
                hideDialog();
                Utilities.displayVolleyError(context, e);
            }

        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Creates and executes a request to get a list of the users (developers only).
     */
    private void getUserList()
    {
        String tag_string_req = "user_list";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "users/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    submissionsEditor.putString(Constants.API_USERS, response).apply();
                    stringJsonUserList = response;
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());
            hideDialog();
            Utilities.displayVolleyError(context, e);
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

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
     * @param userList JSON containing the list of users
     */
    private void parseAndDisplayJsonResponse(String response, String userList) {
        try {
            int ownUserId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("user_id", -1);
            int ownCompanyId = getSharedPreferences(Constants.LOGIN_CACHE, MODE_PRIVATE).getInt("company_id", -1);

            final SparseArray<String> usersSparseArray = new SparseArray<>();

            if (!userList.equals("")){
                JSONObject jObjUserList = new JSONObject(userList);

                JSONArray jUsersArray = jObjUserList.getJSONArray("data");

                for (int i = 0; i < jUsersArray.length(); i++){

                    JSONObject currentUser = jUsersArray.getJSONObject(i);

                    int id = currentUser.getInt("id");
                    String name = currentUser.getString("username");
                    int companyId = currentUser.getInt("company_id");

                    if (companyId == ownCompanyId || getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX, false)) {
                        usersSparseArray.append(id, name);
                    }
                }
            }

            JSONObject jObjSubmissions = new JSONObject(response);

            JSONArray jSubmissionsArray = jObjSubmissions.getJSONArray("data");

            submissionsArrayList = new ArrayList<>();

            for (int i = 0; i < jSubmissionsArray.length(); i++){

                JSONObject currentSubmission = jSubmissionsArray.getJSONObject(i);

                int id = currentSubmission.getInt("id");
                int questionnaireId = currentSubmission.getInt("questionnaire_id");
                int userId = currentSubmission.getInt("user_id");

                int previousSubmissionId = 0;
                if (!currentSubmission.isNull("prev_submission_id")){
                    previousSubmissionId = currentSubmission.getInt("prev_submission_id");
                }

                int nextSubmissionId = 0;
                if (!currentSubmission.isNull("next_submission_id")){
                    nextSubmissionId = currentSubmission.getInt("next_submission_id");
                }

                String createdAt = currentSubmission.getString("created_at");
                Long createdAtMillis = null;
                if (!createdAt.equals("null")) {
                    try {
                        createdAtMillis = sdfDateAndTimeLaravel.parse(createdAt).getTime();
                        createdAt = sdfDateAndTime.format(sdfDateAndTimeLaravel.parse(createdAt));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String updatedAt = currentSubmission.getString("updated_at");
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

                String userName = "";
                if (usersSparseArray.size() > 0){
                    userName = usersSparseArray.get(userId, "");
                }

                if (ownUserId == userId || ((getSharedPreferences(Constants.PERMISSIONS_CACHE, AppCompatActivity.MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX, false) || getSharedPreferences(Constants.PERMISSIONS_CACHE, AppCompatActivity.MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX_COMPANY, false)) && !userName.equals("")))
                {
                    boolean filterStartDateBool = true;
                    boolean filterEndDateBool = true;
                    boolean filterQuestionnaireIdBool = true;
                    boolean filterUsernameBool = true;

                    if (getIntent().getBooleanExtra("FILTER", false))
                    {
                        if (filterStartDateIsEnabled && createdAtMillis != null) {
                            filterStartDateBool = filterStartDate.getTime() < createdAtMillis;
                        }

                        if (filterEndDateIsEnabled && createdAtMillis != null) {
                            filterEndDateBool = createdAtMillis < (filterEndDate.getTime() + 86400000);
                        }

                        if (filterQuestionnaireIdIsEnabled) {
                            filterQuestionnaireIdBool = filterQuestionnaireId == questionnaireId;
                        }

                        if (filterUsernameIsEnabled) {
                            filterUsernameBool = filterUserName.equals(usersSparseArray.get(userId));
                        }
                    }

                    if (filterStartDateBool && filterEndDateBool && filterQuestionnaireIdBool && filterUsernameBool && nextSubmissionId == 0)
                    {
                        submissionsArrayList.add(new SubmittedQuestionnaire(id, questionnaireId, questionnaireGroupId, version, title, description, userId, userName, createdAt, updatedAt, previousSubmissionId, nextSubmissionId));
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

            SubmissionsAdapter adapter = new SubmissionsAdapter(
                    context,
                    submissionsArrayList,
                    (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_USER_INDEX, false) || getSharedPreferences(Constants.PERMISSIONS_CACHE, AppCompatActivity.MODE_PRIVATE).getBoolean(Constants.PERMISSION_SUBMISSION_INDEX_COMPANY, false))
            );

            submittedQuestionnairesListView.setAdapter(adapter);
            submittedQuestionnairesListView.setOnItemClickListener((parent, view, position, id) -> {

                submittedQuestionnairesListView.setEnabled(false);

                SubmittedQuestionnaire submittedQuestionnaire = submissionsArrayList.get(position);

                if (Utilities.checkNetworkConnection(context)){
                    Intent intent = new Intent(context, SubmissionViewerActivity.class);
                    intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(submittedQuestionnaire.getId()));
                    intent.putExtra("DOWNLOAD_JSON", true);

                    if (submittedQuestionnaire.getUserName() != null && !submittedQuestionnaire.getUserName().equals("")  && !submittedQuestionnaire.getUserName().equals("null")) {
                        intent.putExtra("USERNAME", submittedQuestionnaire.getUserName());
                    }

                    startActivity(intent);
                }
                else {
                    if (submissionsSharedPreferences.contains(Constants.API_SUBMISSIONS_ + submittedQuestionnaire.getId())){
                        Intent intent = new Intent(context, SubmissionViewerActivity.class);
                        intent.putExtra("QUESTIONNAIRE_ID", Integer.toString(submittedQuestionnaire.getId()));
                        intent.putExtra("DOWNLOAD_JSON", false);

                        if (submittedQuestionnaire.getUserName() != null && !submittedQuestionnaire.getUserName().equals("")  && !submittedQuestionnaire.getUserName().equals("null")) {
                            intent.putExtra("USERNAME", submittedQuestionnaire.getUserName());
                        }

                        startActivity(intent);
                    }
                    else {
                        submittedQuestionnairesListView.setEnabled(true);
                        handleLayoutChanges(TOAST_NO_INTERNET);
                    }
                }
            });

            if (submissionsArrayList.size() == 0){
                handleLayoutChanges(NO_SUBMISSIONS);
            }
            else {
                handleLayoutChanges(LAYOUT_LIST);
            }

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException Error: " + e.toString() + ", " + e.getMessage());
            Utilities.displayToast(context, "JSONException Error: " + e.toString() + ", " + e.getMessage());
        }

        hideDialog();
    }

    private void downloadAllPdfFiles() {
        alternateNumbering = false;
        if (downloadPdfIndex < submissionsArrayList.size()) {
            pDialog.setMessage("Downloading all pdf files (" + (downloadPdfIndex + 1) + "/" + submissionsArrayList.size() + ")");
            submittedQuestionnaireId = submissionsArrayList.get(downloadPdfIndex).getId();
            getSubmittedQuestionnaire(submittedQuestionnaireId);
        }
        else {
            hideDialog();
        }
    }

    /**
     * Creates and executes a request to get a submitted questionnaire.
     */
    private void getSubmittedQuestionnaire(int submittedQuestionnaireId) {
        String tag_string_req = "sbm_get_questionnaire";

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/" + submittedQuestionnaireId + "/");

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "submissions/" + submittedQuestionnaireId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);
                    parseJsonResponse(response);
                }, e -> {
            MyLog.e(TAG, "Volley Error: " + e.toString() + ", " + e.getMessage() + ", " + e.getLocalizedMessage());

            downloadPdfIndex++;
            downloadAllPdfFiles();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Accept-Language", Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

                return headers;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Parses the JSON containing the submitted questionnaire.
     *
     * @param response JSON containing the submitted questionnaire
     */
    private void parseJsonResponse(String response) {

        try{
            JSONObject jQuestionnaire = new JSONObject(response).getJSONObject("data").getJSONObject("questionnaire");

            questionnaire = NewJsonLib.parseJsonObjectQuestionnaire(context, jQuestionnaire);

            submittedQuestionnaire = NewJsonLib.parseJsonSubmittedQuestionnaire(context, response, questionnaire);

            username = submissionsArrayList.get(downloadPdfIndex).getUserName();

            if (submittedQuestionnaire != null && username != null && !username.equals("")) {
                submittedQuestionnaire.setUserName(username);
            }

            if (submittedQuestionnaire != null) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        createPdfFile();
                    }
                }, 10);
            }

        } catch (JSONException e) {
            MyLog.e(TAG, "JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());
            Utilities.displayToast(context, TAG + " | JSONException " + context.getString(com.kuleuven.android.kuleuvenlibrary.R.string.error_colon) + e.getMessage());

            downloadPdfIndex++;
            downloadAllPdfFiles();
        }
    }

    /**
     * Creates and outputs the PDF file.
     */
    private void createPdfFile() {

        for (Question question : questionnaire.getQuestionsList()) {
            String stringQuestion = question.getQuestion();
            int hashCount = stringQuestion.length() - stringQuestion.replaceAll("#", "").length();

            if (hashCount == 2) {
                alternateNumbering = true;
                break;
            }
        }

        currentTopPositionFromBottom = 772;

        Bitmap bulletBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bullet);

        mPDFWriter = new PDFWriter();

        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_BOLD);
        addText(defaultLeftMargin, currentTopPositionFromBottom, titleFontSize, submittedQuestionnaire.getTitle());
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);

        currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, titleFontSize, defaultInterlinearDistance);
        if (username != null && !username.equals("")) {
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_user_colon, username));
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
        }
        addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.user_id_colon) + submittedQuestionnaire.getUserId());

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.educat_logo_small);
        mPDFWriter.addImageKeepRatio(420, 735, 80, 80, bitmap);

        if (submittedQuestionnaire.getEditDate().equals("null") || submittedQuestionnaire.getDate().equals(submittedQuestionnaire.getEditDate())) {
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_submission_date_colon) + submittedQuestionnaire.getDate());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.started_at_colon) + submittedQuestionnaire.getStartedAt());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.finished_at_colon) + submittedQuestionnaire.getFinishedAt());
        } else {
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_submission_date_colon) + submittedQuestionnaire.getDate());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.list_last_edit_colon) + submittedQuestionnaire.getEditDate());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.started_at_colon) + submittedQuestionnaire.getStartedAt());
            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
            addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, getString(R.string.finished_at_colon) + submittedQuestionnaire.getFinishedAt());
        }

        String descriptionString = submittedQuestionnaire.getDescription();

        if (descriptionString.length() > 0) {
            String[] splitDescription = descriptionString.split(System.lineSeparator());
            for (String s : splitDescription) {
                currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
                addText(defaultLeftMargin, currentTopPositionFromBottom, defaultFontSize, s);
            }
        }

        SubmittedQuestionnaireQuestion submittedQuestionnaireQuestion;
        SubmittedQuestionnaireAnswer submittedQuestionnaireAnswer;

        for (int i = 0; i < submittedQuestionnaire.getQuestionsList().size(); i++) {

            submittedQuestionnaireQuestion = submittedQuestionnaire.getQuestionsList().get(i);

            String questionString = parseAlternateQuestionNumbering(i + 1, submittedQuestionnaireQuestion.getQuestion());
            questionString = questionString.replaceAll("\\/","\\\\/");
            questionString = questionString.replaceAll("\\(","\\\\(");
            questionString = questionString.replaceAll("\\)","\\\\)");
            questionString = questionString.replaceAll("–","-");
            questionString = questionString.replaceAll("’","'");

            String[] splitQuestion = questionString.split(System.lineSeparator());

            for (int k = 0; k < splitQuestion.length; k++) {

                if (k == 0) {
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, bigInterlinearDistance);
                }
                else {
                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, smallInterlinearDistance);
                }
                addText(questionLeftMargin, currentTopPositionFromBottom, defaultFontSize, splitQuestion[k]);
            }

            if (submittedQuestionnaireQuestion.getBulletType() == Question.NO_BULLETS || submittedQuestionnaireQuestion.getBulletType() == Question.RADIO_BUTTONS || submittedQuestionnaireQuestion.getBulletType() == Question.CHECKBOXES) {

                for (int j = 0; j < submittedQuestionnaireQuestion.getAnswersList().size(); j++) {

                    submittedQuestionnaireAnswer = submittedQuestionnaireQuestion.getAnswersList().get(j);

                    String answerValue = submittedQuestionnaireAnswer.getAnswer();

                    // Special case for the "Yes/No Scale"
                    if (submittedQuestionnaireAnswer.getTypeId() == Answer.YES_NO) {
                        if (answerValue.equals("1")) {
                            answerValue = getString(R.string.yes);
                        } else {
                            answerValue = getString(R.string.no);
                        }
                    }

                    if (!submittedQuestionnaireAnswer.getPrefix().equals("null") && !submittedQuestionnaireAnswer.getPrefix().equals("") && !submittedQuestionnaireAnswer.getPrefix().equals(" ") && !submittedQuestionnaireAnswer.getPrefix().trim().equals(":")) {

                        if (!answerValue.equals("") && !answerValue.equals("null")) {
                            switch (submittedQuestionnaireAnswer.getPrefix().substring(submittedQuestionnaireAnswer.getPrefix().length() - 1)) {
                                case ":":
                                case "?":
                                case "!":
                                    answerValue = String.format("%s %s", parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix()), answerValue);
                                    break;

                                default:
                                    answerValue = String.format("%s: %s", parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix()), answerValue);
                            }

                        } else {
                            answerValue = parseAlternateAnswerNumbering(submittedQuestionnaireAnswer.getPrefix());
                        }

                    }

                    currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, defaultFontSize, defaultInterlinearDistance);
                    mPDFWriter.addImageKeepRatio(answerLeftMargin, currentTopPositionFromBottom + 1, 5, 5, bulletBitmap);
                    addText(answerLeftMargin + 10, currentTopPositionFromBottom, defaultFontSize, answerValue);
                }

            }

        }

        int pageCount = mPDFWriter.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            mPDFWriter.setCurrentPage(i);
            mPDFWriter.addText(550, 30, 10, (i + 1) + " / " + pageCount);
        }

        String pdfContent = mPDFWriter.asString();

        OutputStream outputStream;

        String name = submittedQuestionnaireId + "_" + submittedQuestionnaire.getDate().substring(0, 10) + "_" + submittedQuestionnaire.getUserName() + "_" + submittedQuestionnaire.getTitle() + ".pdf";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
                Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                if (uri != null) {
                    outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        outputStream.write(pdfContent.getBytes(crl.android.pdfwriter.StandardCharsets.ISO_8859_1));
                        outputStream.close();
                    }
                }
            }
            else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
                File pdf = new File(imagesDir, name + ".pdf");
                outputStream = new FileOutputStream(pdf);
                outputStream.write(pdfContent.getBytes(crl.android.pdfwriter.StandardCharsets.ISO_8859_1));
                outputStream.close();
            }

            downloadPdfIndex++;
            downloadAllPdfFiles();
        } catch (IOException e) {
            downloadPdfIndex++;
            downloadAllPdfFiles();
        }
    }

    /**
     * Adds text to the PDF file.
     * If the text is too long, the text will be split and printed on the next line.
     */
    private void addText(int leftPosition, int topPositionFromBottom, int fontSize, String text) {
        Paint paint = new TextPaint();
        paint.setTextSize(fontSize);
        paint.setTypeface(Typeface.create("Times New Roman", Typeface.NORMAL));
        float textLength = paint.measureText(text);

        if (leftPosition + textLength > A4_WIDTH - 40) {
            int lastSpace = text.lastIndexOf(' ');
            String truncatedText = text.substring(0, lastSpace);
            textLength = paint.measureText(truncatedText);

            while (leftPosition + textLength > A4_WIDTH - 40) {
                lastSpace = truncatedText.lastIndexOf(' ');
                if (lastSpace == -1) {
                    break;
                }
                truncatedText = truncatedText.substring(0, lastSpace);
                textLength = paint.measureText(truncatedText);
            }

            mPDFWriter.addText(leftPosition, topPositionFromBottom, fontSize, truncatedText);

            currentTopPositionFromBottom = moveCursor(currentTopPositionFromBottom, fontSize, smallInterlinearDistance);
            addText(leftPosition, currentTopPositionFromBottom, defaultFontSize, text.substring(lastSpace + 1));
        }
        else {
            mPDFWriter.addText(leftPosition, topPositionFromBottom, fontSize, text);
        }
    }

    /**
     * Moves the cursor for the PDF writer, creates new page if needed.
     * @return new cursor position
     */
    private int moveCursor(int currentTopPositionFromBottom, int previousFontSize, int interlinearDistance) {
        if (currentTopPositionFromBottom < 80) {
            mPDFWriter.newPage();
            return 772;
        }
        else {
            return currentTopPositionFromBottom - previousFontSize - interlinearDistance;
        }
    }

    /**
     * Creates a question string with the alternate numbering, if applicable, otherwise the
     * standard numbering is used
     *
     * @param questionNumber standard number of question
     * @param stringQuestion the question itself
     * @return question string with numbering
     */
    private String parseAlternateQuestionNumbering(int questionNumber, String stringQuestion) {

        int hashCount = stringQuestion.length() - stringQuestion.replaceAll("#", "").length();

        switch (hashCount){
            case 0:
                if (alternateNumbering) {
                    return stringQuestion;
                }
                else {
                    return String.format("%s. %s", questionNumber, stringQuestion);
                }

            case 2:
                stringQuestion = stringQuestion.substring(stringQuestion.indexOf("#") + 1);
                String alternateNumber = stringQuestion.substring(0, stringQuestion.indexOf("#"));
                stringQuestion = stringQuestion.substring(stringQuestion.indexOf("#") + 1).trim();
                if (alternateNumber.endsWith(".")) {
                    return String.format("%s %s", alternateNumber, stringQuestion);
                }
                else {
                    return String.format("%s. %s", alternateNumber, stringQuestion);
                }

            default:
                return "Error in hash count";
        }
    }

    /**
     * Creates an answer string with the alternate numbering, if applicable
     *
     * @param stringAnswer the answer itself
     * @return answer string with the alternate numbering, if applicable
     */
    private String parseAlternateAnswerNumbering(String stringAnswer) {

        int hashCount = stringAnswer.length() - stringAnswer.replaceAll("#", "").length();

        switch (hashCount){
            case 0:
                return stringAnswer;

            case 2:
                stringAnswer = stringAnswer.substring(stringAnswer.indexOf("#") + 1);
                String alternateNumber = stringAnswer.substring(0, stringAnswer.indexOf("#"));
                stringAnswer = stringAnswer.substring(stringAnswer.indexOf("#") + 1).trim();
                if (alternateNumber.endsWith(".")) {
                    return String.format("%s %s", alternateNumber, stringAnswer);
                }
                else {
                    return String.format("%s. %s", alternateNumber, stringAnswer);
                }

            default:
                return "Error in hash count";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_submitted_questionnaires, menu);
//        menu.findItem(R.id.action_download_all_pdf_files).setVisible(getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false));
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh_list) {
            if (Utilities.checkNetworkConnection(context)) {
                handleLayoutChanges(LAYOUT_LOADING);
                getSubmittedQuestionnaires();
            } else {
                handleLayoutChanges(LAYOUT_LOADING);
                stringJsonResponse = submissionsSharedPreferences.getString(Constants.API_SUBMISSIONS, "");
                stringJsonUserList = submissionsSharedPreferences.getString(Constants.API_USERS, "");
                if (stringJsonResponse != null && !stringJsonResponse.equals("") && stringJsonUserList != null) {
                    parseAndDisplayJsonResponse(stringJsonResponse, stringJsonUserList);
                    handleLayoutChanges(TOAST_OFFLINE_LIST);
                } else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        } else if (itemId == R.id.action_filter_list) {
            Intent intent = new Intent(this, SubmissionsFilterActivity.class);
            if (filterStartDate != null) {
                intent.putExtra("START_DATE", filterStartDate);
            }
            intent.putExtra("START_DATE_IS_ENABLED", filterStartDateIsEnabled);
            if (filterEndDate != null) {
                intent.putExtra("END_DATE", filterEndDate);
            }
            intent.putExtra("END_DATE_IS_ENABLED", filterEndDateIsEnabled);

            intent.putExtra("QUESTIONNAIRE_ID", filterQuestionnaireId);
            intent.putExtra("QUESTIONNAIRE_IS_ENABLED", filterQuestionnaireIdIsEnabled);

            intent.putExtra("USERNAME", filterUserName);
            intent.putExtra("USERNAME_IS_ENABLED", filterUsernameIsEnabled);
            startActivityForResult(intent, 0);
        }
        else if (itemId == R.id.action_download_all_pdf_files) {
            pDialog.setMessage("Downloading all pdf files (" + (downloadPdfIndex + 1) + "/" + submissionsArrayList.size() + ")");
            showDialog();
            downloadAllPdfFiles();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0){

            if (resultCode == RESULT_OK)
            {
                getIntent().putExtra("START_DATE", data.getSerializableExtra("START_DATE"));
                getIntent().putExtra("START_DATE_IS_ENABLED", data.getBooleanExtra("START_DATE_IS_ENABLED", false));
                getIntent().putExtra("END_DATE", data.getSerializableExtra("END_DATE"));
                getIntent().putExtra("END_DATE_IS_ENABLED", data.getBooleanExtra("END_DATE_IS_ENABLED", false));
                getIntent().putExtra("QUESTIONNAIRE_ID", data.getIntExtra("QUESTIONNAIRE_ID", -1));
                getIntent().putExtra("QUESTIONNAIRE_IS_ENABLED", data.getBooleanExtra("QUESTIONNAIRE_IS_ENABLED", false));
                getIntent().putExtra("USERNAME", data.getStringExtra("USERNAME"));
                getIntent().putExtra("USERNAME_IS_ENABLED", data.getBooleanExtra("USERNAME_IS_ENABLED", false));
                getIntent().putExtra("FILTER", true);
                recreate();
            }

            if (resultCode == RESULT_CANCELED)
            {
                getIntent().putExtra("FILTER", false);
                recreate();
            }
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

    @Override
    protected void onDestroy() {
        hideDialog();
        super.onDestroy();
    }
}
