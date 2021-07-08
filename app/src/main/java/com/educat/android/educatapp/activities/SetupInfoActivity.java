package com.educat.android.educatapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.educat.android.educatapp.R;
import com.educat.android.educatapp.helperClasses.AppController;
import com.educat.android.educatapp.helperClasses.Constants;
import com.educat.android.educatapp.helperClasses.MyLog;
import com.educat.android.educatapp.helperClasses.Utilities;
import com.educat.android.educatapp.setups.Instrument;
import com.educat.android.educatapp.setups.Parameter;
import com.educat.android.educatapp.setups.Setup;
import com.kuleuven.android.kuleuvenlibrary.LibUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SetupInfoActivity extends AppCompatActivity {

    private static final String TAG = "SetupInfoActivity";
    private final Context context = this;
    private ProgressDialog pDialog;

    private LinearLayout setupLinearLayout;
    private ScrollView setupScrollView;
    private TextView backgroundTextView;

    private int setupId;
    private Setup setup;

    private static final String STATE_JSON_RESPONSE = "dataJsonResponse";
    private String stringJsonResponse = "";

    private String jsonTypeInfoList;
    private String jsonParameterInfoList;

    private SharedPreferences setupSharedPreferences;
    private SharedPreferences.Editor setupEditor;
    private AlertDialog.Builder builder;
    private LayoutInflater inflater;

    private static final int LAYOUT_LOADING = 1;
    private static final int LAYOUT_NO_INTERNET = 2;
    private static final int TOAST_NO_INTERNET = 3;
    private static final int TOAST_OFFLINE_DATA = 4;
    private static final int LAYOUT_SETUP = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_info);

        setupSharedPreferences = getSharedPreferences(Constants.SETUP_DATA, MODE_PRIVATE);
        setupEditor = setupSharedPreferences.edit();
        setupEditor.apply();

        builder = new AlertDialog.Builder(this);
        inflater = getLayoutInflater();

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        setupLinearLayout = findViewById(R.id.setup_linear_layout);
        setupScrollView = findViewById(R.id.setup_scroll_view);
        backgroundTextView = findViewById(R.id.background_text_view);

        setupId = getIntent().getIntExtra("SETUP_ID", -1);

        jsonTypeInfoList = setupSharedPreferences.getString(Constants.API_INSTRUMENT_TYPES, "");
        jsonParameterInfoList = setupSharedPreferences.getString(Constants.API_PARAMETERS, "");

        if (savedInstanceState != null){
            stringJsonResponse = savedInstanceState.getString(STATE_JSON_RESPONSE);

            setup = Utilities.parseJsonSetup(context, stringJsonResponse, jsonTypeInfoList, jsonParameterInfoList);

            if (setup != null){
                displaySetup();
            }
        }
        else {
            if (Utilities.checkNetworkConnection(context)){
                handleLayoutChanges(LAYOUT_LOADING);
                getSetupById();
            }
            else {
                handleLayoutChanges(LAYOUT_LOADING);
                stringJsonResponse = setupSharedPreferences.getString(Constants.API_SETUPS_ + setupId, "");
                if (stringJsonResponse != null && !stringJsonResponse.equals("")){

                    setup = Utilities.parseJsonSetup(context, stringJsonResponse, jsonTypeInfoList, jsonParameterInfoList);

                    if (setup != null){
                        displaySetup();
                    }

                    handleLayoutChanges(TOAST_OFFLINE_DATA);
                }
                else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putString(STATE_JSON_RESPONSE, stringJsonResponse);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Creates and executes a request to get a setup with a given ID.
     */
    private void getSetupById() {

        String tag_string_req = "get_setup";

        pDialog.setMessage(getString(R.string.getting_selected_setup_ellipsis));
        showDialog();

        MyLog.d("StringRequest", PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/");

        final StringRequest strReq = new StringRequest(
                Request.Method.GET,
                PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext()).getString(Constants.SETTING_SERVER_API_URL, Constants.API_URL) + "setups/" + setupId + "/",
                response -> {
                    LibUtilities.printGiantLog(TAG, "JSON Response: " + response, false);

                    stringJsonResponse = response;

                    setup = Utilities.parseJsonSetup(context, stringJsonResponse, jsonTypeInfoList, jsonParameterInfoList);

                    if (setup != null){
                        setupEditor.putString(Constants.API_SETUPS_ + setup.getId(), response).apply();
                        displaySetup();
                    }

                    hideDialog();
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
     * Displays the setup.
     */
    private void displaySetup(){

        setTitle(setup.getName());

        setupLinearLayout.removeAllViews();

        // Counts the total of instruments and parameters in the setup.
        int instrumentCount = 0;
        int parameterCount = 0;

        for (int i = 0; i < setup.getInstrumentArrayList().size(); i++){
            instrumentCount++;

            for (int j = 0; j < setup.getInstrumentArrayList().get(i).getParameterArrayList().size(); j++){
                parameterCount++;
            }
        }

        // Initialize View arrays
        View[] instrumentInflatedLayouts = new View[instrumentCount];
        View[] typeInflatedLayouts = new View[instrumentCount];
        View[] parameterInflatedLayouts = new View[parameterCount];
        View[] instrumentInflatedContainers = new View[instrumentCount];
        View[] typeInflatedContainers = new View[instrumentCount];
        View[] parametersInflatedContainers = new View[instrumentCount];
        LinearLayout[] instrumentContainers = new LinearLayout[instrumentCount];
        LinearLayout[] typeContainers = new LinearLayout[instrumentCount];
        LinearLayout[] parametersContainers = new LinearLayout[instrumentCount];

        // Setup
        View setupInflatedLayout = inflater.inflate(R.layout.linear_layout_setup_info, null);
        ((TextView) setupInflatedLayout.findViewById(R.id.text_view_top)).setText(R.string.setup);
        ((TextView) setupInflatedLayout.findViewById(R.id.text_view_bottom)).setText(String.format("%s (%s)", setup.getName(), setup.getId()));
        (setupInflatedLayout.findViewById(R.id.info_image_view)).setOnClickListener(v ->
                builder.setView(Utilities.createSetupDetailsLinearLayout(context, inflater, Constants.SETUP, setup, -1, -1))
                        .setCancelable(true)
                        .create()
                        .show()
        );
        setupLinearLayout.addView(setupInflatedLayout);

        int instrumentIndex = 0;
        int parameterIndex = 0;

        Instrument instrument;
        Parameter parameter;

        for (int i = 0; i < setup.getInstrumentArrayList().size(); i++){

            instrument = setup.getInstrumentArrayList().get(i);

            final int finalI = i;

            // Instrument
            instrumentInflatedContainers[instrumentIndex] = inflater.inflate(R.layout.linear_layout_container, null);
            instrumentContainers[instrumentIndex] = instrumentInflatedContainers[instrumentIndex].findViewById(R.id.container);
            instrumentContainers[instrumentIndex].setBackgroundColor(Color.parseColor("#eeeeee"));
            setupLinearLayout.addView(instrumentInflatedContainers[instrumentIndex]);

            instrumentInflatedLayouts[instrumentIndex] = inflater.inflate(R.layout.linear_layout_setup_info, null);
            ((TextView) instrumentInflatedLayouts[instrumentIndex].findViewById(R.id.text_view_top)).setText(R.string.instrument);
            ((TextView) instrumentInflatedLayouts[instrumentIndex].findViewById(R.id.text_view_bottom)).setText(String.format("%s (%s)", instrument.getName(), instrument.getId()));
            (instrumentInflatedLayouts[instrumentIndex].findViewById(R.id.info_image_view)).setOnClickListener(v ->
                    builder.setView(Utilities.createSetupDetailsLinearLayout(context, inflater, Constants.INSTRUMENT, setup, finalI, -1))
                            .setCancelable(true)
                            .create()
                            .show()
            );
            instrumentContainers[instrumentIndex].addView(instrumentInflatedLayouts[instrumentIndex]);

            // Type
            typeInflatedContainers[instrumentIndex] = inflater.inflate(R.layout.linear_layout_container, null);
            typeContainers[instrumentIndex] = typeInflatedContainers[instrumentIndex].findViewById(R.id.container);
            typeContainers[instrumentIndex].setBackgroundColor(Color.parseColor("#dddddd"));
            instrumentContainers[instrumentIndex].addView(typeInflatedContainers[instrumentIndex]);

            typeInflatedLayouts[instrumentIndex] = inflater.inflate(R.layout.linear_layout_setup_info, null);
            ((TextView) typeInflatedLayouts[instrumentIndex].findViewById(R.id.text_view_top)).setText(R.string.type);
            ((TextView) typeInflatedLayouts[instrumentIndex].findViewById(R.id.text_view_bottom)).setText(instrument.getType().getName());
            (typeInflatedLayouts[instrumentIndex].findViewById(R.id.info_image_view)).setOnClickListener(v ->
                    builder.setView(Utilities.createSetupDetailsLinearLayout(context, inflater, Constants.TYPE, setup, finalI, -1))
                            .setCancelable(true)
                            .create()
                            .show()
            );
            typeContainers[instrumentIndex].addView(typeInflatedLayouts[instrumentIndex]);

            // Parameters
            parametersInflatedContainers[instrumentIndex] = inflater.inflate(R.layout.linear_layout_container, null);
            parametersContainers[instrumentIndex] = parametersInflatedContainers[instrumentIndex].findViewById(R.id.container);
            parametersContainers[instrumentIndex].setBackgroundColor(Color.parseColor("#dddddd"));
            instrumentContainers[instrumentIndex].addView(parametersInflatedContainers[instrumentIndex]);

            for (int j = 0; j < instrument.getParameterArrayList().size(); j++){

                parameter = instrument.getParameterArrayList().get(j);

                final int finalJ = j;

                parameterInflatedLayouts[parameterIndex] = inflater.inflate(R.layout.linear_layout_setup_info, null);
                ((TextView) parameterInflatedLayouts[parameterIndex].findViewById(R.id.text_view_top)).setText(R.string.parameter);
                if (parameter.getValueDescription().equals("")){
                    if (parameter.getDataType() == 5 && parameter.getValue() != null) { // Half of a MAC address, represented in the database by a float equivalent
                        ((TextView) parameterInflatedLayouts[parameterIndex].findViewById(R.id.text_view_bottom)).setText(String.format("%s: %s", parameter.getName(), String.format("%s", Utilities.floatToMac(parameter.getValue()))));
                    }
                    else {
                        ((TextView) parameterInflatedLayouts[parameterIndex].findViewById(R.id.text_view_bottom)).setText(String.format("%s: %s", parameter.getName(), parameter.getValue()));
                    }
                }
                else {
                    ((TextView) parameterInflatedLayouts[parameterIndex].findViewById(R.id.text_view_bottom)).setText(String.format("%s: %s (%s)", parameter.getName(), parameter.getValue(), parameter.getValueDescription()));
                }
                (parameterInflatedLayouts[parameterIndex].findViewById(R.id.info_image_view)).setOnClickListener(v ->
                        builder.setView(Utilities.createSetupDetailsLinearLayout(context, inflater, Constants.PARAMETER, setup, finalI, finalJ))
                                .setCancelable(true)
                                .create()
                                .show()
                );
                parametersContainers[instrumentIndex].addView(parameterInflatedLayouts[parameterIndex]);
                parameterIndex++;
            }

            instrumentIndex++;

        }

        if (getSharedPreferences(Constants.PERMISSIONS_CACHE, MODE_PRIVATE).getBoolean(Constants.PERMISSION_DEBUG_CONSOLE, false)) {
            // Setup
            View rawInflatedLayout = inflater.inflate(R.layout.linear_layout_setup_info, null);
            ((TextView) rawInflatedLayout.findViewById(R.id.text_view_top)).setText("RAW");
            ((TextView) rawInflatedLayout.findViewById(R.id.text_view_bottom)).setText(Utilities.bytesToHex(Utilities.convertSetupToRawData(context, setup)));
            (rawInflatedLayout.findViewById(R.id.info_image_view)).setVisibility(View.GONE);
            setupLinearLayout.addView(rawInflatedLayout);
        }

        handleLayoutChanges(LAYOUT_SETUP);

        calculateParametersForDataStream(setup);
    }

    /**
     * Calculates the expected data length for the data stream.
     *
     * @param setup to be analyzed
     */
    private void calculateParametersForDataStream(Setup setup){

        List<Float> dataOrder = Collections.synchronizedList(new ArrayList<>());
        int dataLength = 0;

        for (Instrument instrument : setup.getInstrumentArrayList()){

            for (Parameter parameter : instrument.getParameterArrayList()){

                if (parameter.getId() == Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE){

                    switch (Float.floatToIntBits(parameter.getValue())){

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_NONE:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_NONE);
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1);
                            dataLength += 5;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2);
                            dataLength += 6;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3);
                            dataLength += 5;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_9AXIS_ROT_VEC_0XB1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_9AXIS_ROT_VEC_0XB1);
                            dataLength += 26;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_6AXIS_0XB2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_IMU_6AXIS_0XB2);
                            dataLength += 12;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_MIN_DATA_0XC1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_MIN_DATA_0XC1);
                            dataLength += 17;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_STATUS_0XC2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_STATUS_0XC2);
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_DATA_STATUS_0XC3:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_GPS_DATA_STATUS_0XC3);
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D1_0XD1__US:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D1_0XD1__US);
                            dataLength += 2;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D2_0XD2__IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D2_0XD2__IR);
                            dataLength += 1;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D3_0XD3__US_IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D3_0XD3__US_IR);
                            dataLength += 5;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D4_0XD4__US_2IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D4_0XD4__US_2IR);
                            dataLength += 6;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D5_0XD5__US_3IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D5_0XD5__US_3IR);
                            dataLength += 7;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D6_0XD6__4IR:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D6_0XD6__4IR);
                            dataLength += 6;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D7_0XD7__4IR_ONLY_CALCULATED_VALUE:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D7_0XD7__4IR_ONLY_CALCULATED_VALUE);
                            dataLength += 2;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D8_0XD8__US_3IR_ONLY_CALCULATED_VALUE:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_CAN_DISTANCE_NODES_D8_0XD8__US_3IR_ONLY_CALCULATED_VALUE);
                            dataLength += 2;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_REAL_TIME_CLOCK_RTC__0XE1);
                            dataLength += 8;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT_0XF1:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT_0XF1);
                            dataLength += 2;
                            dataLength++;
                            break;

                        case (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT__SENSOR_ACTIVATE_BITS_0XF2:
                            dataOrder.add(Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_USB_AD_AS_INSTRUMENT__SENSOR_ACTIVATE_BITS_0XF2);
                            dataLength += 3;
                            dataLength++;
                            break;

                        default:
                    }
                }
            }
        }

        dataLength++;

        byte[] streamingLength = Utilities.shortToByteArray((short) dataLength);
        byte numberOfPackets = (byte) (dataLength / 243);
        if (dataLength % 243 != 0){
            numberOfPackets++;
        }

        MyLog.d("CalculateLength", "Streaming length: " + Utilities.shortFromByteArray(streamingLength) + ", Number of packets: " + numberOfPackets + ", Data order length: " + dataOrder.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh_list) {
            if (Utilities.checkNetworkConnection(context)) {
                handleLayoutChanges(LAYOUT_LOADING);
                getSetupById();
            } else {
                handleLayoutChanges(LAYOUT_LOADING);
                String savedJson = setupSharedPreferences.getString(Constants.API_SETUPS_ + setupId, "null");
                if (savedJson != null && !savedJson.equals("null")) {

                    setup = Utilities.parseJsonSetup(context, savedJson, jsonTypeInfoList, jsonParameterInfoList);

                    if (setup != null) {
                        displaySetup();
                    }

                    handleLayoutChanges(TOAST_OFFLINE_DATA);
                } else {
                    handleLayoutChanges(LAYOUT_NO_INTERNET);
                }
            }
        }
        return true;
    }

    /**
     * Handles the layout changes depending on the provided layout ID.
     *
     * @param layout id determining the layout
     */
    private void handleLayoutChanges(int layout) {
        switch (layout) {
            case LAYOUT_LOADING:
                setupScrollView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.loading_ellipsis);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case LAYOUT_NO_INTERNET:
                setupScrollView.setVisibility(View.GONE);
                backgroundTextView.setText(R.string.no_internet_access);
                backgroundTextView.setVisibility(View.VISIBLE);
                break;

            case TOAST_NO_INTERNET:
                Utilities.displayToast(context, getString(R.string.no_internet_access));
                break;

            case TOAST_OFFLINE_DATA:
                Utilities.displayToast(context, getString(R.string.no_internet_access_offline_data));
                break;

            case LAYOUT_SETUP:
                backgroundTextView.setVisibility(View.GONE);
                setupScrollView.setVisibility(View.VISIBLE);
                break;
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
