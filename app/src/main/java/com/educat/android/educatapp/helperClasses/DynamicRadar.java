package com.educat.android.educatapp.helperClasses;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.educat.android.educatapp.R;
import com.educat.android.educatapp.setups.Instrument;
import com.educat.android.educatapp.setups.Parameter;
import com.educat.android.educatapp.setups.Setup;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * DynamicRadar
 *
 * Extension of the Android View class to display the OAS.
 */
public class DynamicRadar extends View {

    private static final String TAG = "DynamicRadar";

    private static Paint greyPaint;
    private static Paint blackPaint;
    private static Paint[] segmentPaints;

    private Setup streamSetup;

    private ArrayList<RadarSegment> radarSegments = new ArrayList<>();
    private static float[] measuredDistances;
    private boolean segmentsLoaded = false;
    private boolean firstDraw = true;
    private byte sensorsStatus = 0x00;

    private static final float OAS_MAX_DISTANCE = 255f;
    private static final int OAS_PARTS = 5;

    private float constant_out;
    private float constant_inn;

    private float maximumSpeed;
    private float slopeStart;
    private float slopePercentage;
    private float slopeEnd;

    private int actualSpeedValueIndex = -1;

    public DynamicRadar(Context context) {
        super(context);
        initPaint();
    }

    public DynamicRadar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public DynamicRadar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    public DynamicRadar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaint();
    }

    /**
     * Initializes the different paint colors for the radar.
     */
    private void initPaint(){
        greyPaint = new Paint();
        greyPaint.setColor(getResources().getColor(R.color.radar_gray));
        greyPaint.setStyle(Paint.Style.FILL);
        greyPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        blackPaint = new Paint();
        blackPaint.setColor(Color.BLACK);
        blackPaint.setStyle(Paint.Style.FILL);
        blackPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * Initializes the radar, iterates through the instrument list to find the OAS,
     * using the OAS parameters it iterates through the instruments again to find the
     * instruments attached to the OAS.
     * For each instrument connected to the OAS, a radar segment is created.
     *
     * @param setup contains the OAS and distance sensors
     */
    public void initOas(Setup setup){

        streamSetup = Utilities.generateStreamSetup(setup);

        ArrayList<Float> distanceInstrumentIds = new ArrayList<>();
        boolean oasFound = false;

        for (Instrument instrument : setup.getInstrumentArrayList()){
            for (Parameter parameter : instrument.getParameterArrayList()){
                if (parameter.getId() == Constants.SETUP_PRM_SOFTWARE_FUNCTION){
                    if (Float.floatToIntBits(parameter.getValue()) == (int) Constants.SETUP_PRM_SOFTWARE_FUNCTION_option_OAS_WITH_4_SENSORS){
                        oasFound = true;
                        break;
                    }
                }
            }

            if (oasFound){
                for (Parameter parameter : instrument.getParameterArrayList()) {
                    switch (parameter.getId()){
                        case Constants.SETUP_PRM_INSTRUMENT_1_ID:
                        case Constants.SETUP_PRM_INSTRUMENT_2_ID:
                        case Constants.SETUP_PRM_INSTRUMENT_3_ID:
                        case Constants.SETUP_PRM_INSTRUMENT_4_ID:
                        case Constants.SETUP_PRM_INSTRUMENT_5_ID:
                        case Constants.SETUP_PRM_INSTRUMENT_6_ID:
                        case Constants.SETUP_PRM_INSTRUMENT_7_ID:
                        case Constants.SETUP_PRM_INSTRUMENT_8_ID:
                            distanceInstrumentIds.add(parameter.getValue());
                            break;

                        case Constants.SETUP_PRM_OAS_SLOPE_START:
                            slopeStart = parameter.getValue();
                            break;

                        case Constants.SETUP_PRM_OAS_SLOPE_PERCENTAGE:
                            slopePercentage = parameter.getValue();
                            break;

                        case Constants.SETUP_PRM_OAS_SLOPE_END:
                            slopeEnd = parameter.getValue();
                            break;
                    }
                }
            }

            if (instrument.getOutputDataType() == (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1 || instrument.getOutputDataType() == (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2 || instrument.getOutputDataType() == (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3) {
                for (Parameter parameter : instrument.getParameterArrayList()) {
                    if (parameter.getId() == Constants.SETUP_PRM_MAXIMUM_SPEED) {
                        if (parameter.getValue() != null) {
                            maximumSpeed = parameter.getValue() / 0.0036f;
                        }
                        else {
                            maximumSpeed = 12f / 0.0036f;
                        }
                        break;
                    }
                }
            }
        }

        MyLog.d(TAG, "distanceInstrumentIds.size(): " + distanceInstrumentIds.size());

        for (Instrument instrument : streamSetup.getInstrumentArrayList()){

            if (instrument.getOutputDataType() == (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_DX2_OUTPUT_0XA1 || instrument.getOutputDataType() == (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_PG_OUTPUT_0XA2 || instrument.getOutputDataType() == (int) Constants.SETUP_PRM_DATA_OUTPUT_DATATYPE_option_JOYSTICK_LINX_OUTPUT_0XA3) {
                actualSpeedValueIndex = instrument.getVariableArrayList().get(0).getValueIndex();
            }

            for (Float f : distanceInstrumentIds){

                if (f.intValue() == instrument.getId()){

                    float x = 0;
                    float y = 0;
                    float r = 0;
                    float boundaryCalibration = 0;

                    for (Parameter parameter : instrument.getParameterArrayList()){

                        switch (parameter.getId()){
                            case Constants.SETUP_PRM_X:
                                x = parameter.getValue();
                                break;

                            case Constants.SETUP_PRM_Y:
                                y = parameter.getValue();
                                break;

                            case Constants.SETUP_PRM_R:
                                r = parameter.getValue();
                                break;

                            case Constants.SETUP_PRM_DISTANCE_CALIBRATION:
                                boundaryCalibration = parameter.getValue();
                                break;
                        }

                    }

                    radarSegments.add(new RadarSegment(x, y, r, instrument.getId(), instrument.getVariableArrayList().get(0).getValueIndex(), boundaryCalibration));

                    MyLog.d(TAG, "x = " + x + ", y = " + y + ", r = " + r + ", ID = " + instrument.getId() + ", Value index = " + instrument.getVariableArrayList().get(0).getValueIndex() + ", PWC boundary calibration = " + boundaryCalibration);

                    break;
                }

            }
        }

        segmentPaints = new Paint[radarSegments.size()];
        for (int i = 0; i < segmentPaints.length; i++){
            segmentPaints[i] = new Paint();
            segmentPaints[i].setStyle(Paint.Style.FILL);
            segmentPaints[i].setFlags(Paint.ANTI_ALIAS_FLAG);
            segmentPaints[i].setColor(getResources().getColor(R.color.transparant_gray));
        }

        measuredDistances = new float[radarSegments.size()];

        MyLog.d(TAG, "radarSegments.size(): " + radarSegments.size());

        segmentsLoaded = true;
    }

    /**
     * Adds the new data to the radar, if it's initialized.
     *
     * @param bigDecimalArrayList array list containing the measured values
     */
    public void addData(ArrayList<BigDecimal> bigDecimalArrayList){
        if (segmentsLoaded){
            updateDistances(bigDecimalArrayList);
        }
    }

    /**
     * Updates the two dimensional array to check which parts of the radar need to be activated.
     * Also updates the paint array, determining the color of the each radar segment.
     *
     * @param bigDecimalArrayList containing the measured distances as floating points
     */
    private void updateDistances(ArrayList<BigDecimal> bigDecimalArrayList){
    
        int bigDecimalArrayListSize = bigDecimalArrayList.size();
        int index;
        BigDecimal bigDecimal;
        float currentSpeed = 0f;    
    
        bigDecimal = bigDecimalArrayList.get(bigDecimalArrayListSize - actualSpeedValueIndex);
        if (bigDecimal != null){
            currentSpeed = bigDecimal.floatValue();
        }

        float distanceUnit = OAS_MAX_DISTANCE / (float) OAS_PARTS;

        float alarmDistance;

        if (currentSpeed / maximumSpeed * 100f < slopePercentage) {
            alarmDistance = slopeStart;
        }
        else {
            alarmDistance = (slopeEnd - slopeStart) / (100f - slopePercentage) * (currentSpeed / maximumSpeed * 100f - slopePercentage) + slopeStart;
        }                
        
        for (int i = radarSegments.size() - 1; i >= 0; i--){

            index = bigDecimalArrayListSize - radarSegments.get(i).getValueIndex();

            if (index >= 0) {

                bigDecimal = bigDecimalArrayList.get(index);
                if (bigDecimal != null){
                    measuredDistances[i] = bigDecimal.floatValue() - radarSegments.get(i).getBoundaryCalibration();
                    if (measuredDistances[i] > OAS_MAX_DISTANCE) { measuredDistances[i] = OAS_MAX_DISTANCE; }
                }
                else {
                    measuredDistances[i] = 0.0f;
                }

                // speed dependent
                if (measuredDistances[i] == 0.0f){
                    segmentPaints[i].setColor(getResources().getColor(R.color.transparant_gray));
                }
                else if (measuredDistances[i] - alarmDistance < distanceUnit){
                    segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - alarmDistance - distanceUnit * 0f) / distanceUnit, getResources().getColor(R.color.radar_red), getResources().getColor(R.color.radar_orange)));
                }
                else if (measuredDistances[i] - alarmDistance < distanceUnit * 2f){
                    segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - alarmDistance - distanceUnit * 1f) / distanceUnit, getResources().getColor(R.color.radar_orange), getResources().getColor(R.color.radar_yellow)));
                }
                else if (measuredDistances[i] - alarmDistance < distanceUnit * 3f){
                    segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - alarmDistance - distanceUnit * 2f) / distanceUnit, getResources().getColor(R.color.radar_yellow), getResources().getColor(R.color.radar_light_green)));
                }
                else if (measuredDistances[i] - alarmDistance < distanceUnit * 4f){
                    segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - alarmDistance - distanceUnit * 3f) / distanceUnit, getResources().getColor(R.color.radar_light_green), getResources().getColor(R.color.radar_dark_green)));
                }
                else if (measuredDistances[i] - alarmDistance < distanceUnit * 5f){
                    segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - alarmDistance - distanceUnit * 4f) / distanceUnit, getResources().getColor(R.color.radar_dark_green), getResources().getColor(R.color.radar_gray)));
                }
                else {
                    segmentPaints[i].setColor(getResources().getColor(R.color.transparant_gray));
                }

                // speed independent
//            if (measuredDistances[i] == 0.0f){
//                segmentPaints[i].setColor(getResources().getColor(R.color.transparant_gray));
//            }
//            else if (measuredDistances[i] < distanceUnit){
//                segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - distanceUnit * 0f) / distanceUnit, getResources().getColor(R.color.radar_red), getResources().getColor(R.color.radar_orange)));
//            }
//            else if (measuredDistances[i] < distanceUnit * 2f){
//                segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - distanceUnit * 1f) / distanceUnit, getResources().getColor(R.color.radar_orange), getResources().getColor(R.color.radar_yellow)));
//            }
//            else if (measuredDistances[i] < distanceUnit * 3f){
//                segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - distanceUnit * 2f) / distanceUnit, getResources().getColor(R.color.radar_yellow), getResources().getColor(R.color.radar_light_green)));
//            }
//            else if (measuredDistances[i] < distanceUnit * 4f){
//                segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - distanceUnit * 3f) / distanceUnit, getResources().getColor(R.color.radar_light_green), getResources().getColor(R.color.radar_dark_green)));
//            }
//            else if (measuredDistances[i] < distanceUnit * 5f){
//                segmentPaints[i].setColor((int) Utilities.evaluateArgb((measuredDistances[i] - distanceUnit * 4f) / distanceUnit, getResources().getColor(R.color.radar_dark_green), getResources().getColor(R.color.radar_gray)));
//            }
//            else {
//                segmentPaints[i].setColor(getResources().getColor(R.color.transparant_gray));
//            }

            }

        }

        postInvalidate();
    }

    public byte updateActiveSensors(float x, float y, byte sensorsStatus) {

        this.sensorsStatus = sensorsStatus;

        if (segmentsLoaded) {

            for (int i = 0; i < radarSegments.size(); i++) {

                RadarSegment radarSegment = radarSegments.get(i);

                if (checkPoint(radarSegment.getCx(), radarSegment.getCy(), constant_inn, constant_out, radarSegment.getStartAngle(), radarSegment.getSweepAngle(), x, y)) {

                    if((this.sensorsStatus >> i & 1) != 1) {
                        // sets the bit at position i
                        this.sensorsStatus |= 1 << i;
                    }
                    else {
                        // clears the bit at position i
                        this.sensorsStatus &= ~(1 << i);
                    }

                    MyLog.d(TAG, "updateActiveSensors: sensor " + (i+1) + " status changed to " + ((this.sensorsStatus >> i & 1) == 1));

                    break;
                }
            }
        }

        postInvalidate();

        return this.sensorsStatus;
    }

    private boolean checkPoint(float cx, float cy, float rInn, float rOut, float startAngle,
                               float sweepAngle, float x, float y) {

        float relX = x - cx;
        float relY = y - cy;

        float startX = (float) Math.cos(Math.toRadians(startAngle));
        float startY = (float) Math.sin(Math.toRadians(startAngle));

        float endX = (float) Math.cos(Math.toRadians(startAngle + sweepAngle));
        float endY = (float) Math.sin(Math.toRadians(startAngle + sweepAngle));

        if (!areClockwise(startX, startY, relX, relY) && areClockwise(endX, endY, relX, relY) && isBetweenRadii(relX * relX + relY * relY, rInn, rOut)) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean areClockwise(float v1x, float v1y, float v2x, float v2y) {
        return -v1x * v2y + v1y * v2x > 0;
    }

    private boolean isBetweenRadii(float radiusSquared, float rInn, float rOut) {
       return radiusSquared >= rInn * rInn && radiusSquared <= rOut * rOut;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float size = Math.min(getWidth(), getHeight());
        float base = size / 20F;
        constant_out = size / 5;
        constant_inn = size / 20;
        float x0 = getWidth() / 2F;
        float y0 = getHeight() / 2F;
        float drawableOffset = size * 0.4F;

        Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.wheelchair2, null);
        d.setBounds((int) (x0 - drawableOffset), (int) (y0 - drawableOffset), (int) (x0 + drawableOffset), (int) (y0 + drawableOffset));
        d.draw(canvas);

//        MyLog.d(TAG, "Segments loaded: " + segmentsLoaded);

        if (segmentsLoaded){

            if (firstDraw) {

                float x, y, cx, cy, inn, angle, sweepAngle, startAngle;
                double cosinus, sinus;

                for (int i = 0; i < radarSegments.size(); i++) {

                    RadarSegment radarSegment = radarSegments.get(i);

                    // determine the starting point for this instrument
                    x = x0 + base * radarSegment.getX();
                    y = y0 - base * radarSegment.getY();

                    angle = radarSegments.get(i).getR() - 90;
                    sweepAngle = 27F;
                    startAngle = angle - sweepAngle / 2F;

                    cosinus = Math.cos(Math.toRadians(angle));
                    sinus = Math.sin(Math.toRadians(angle));

                    cx = (float) (x - base * cosinus);
                    cy = (float) (y - base * sinus);

                    drawArcSegment(canvas, cx, cy, constant_inn, constant_out, startAngle, sweepAngle, greyPaint, null);

                    inn = (constant_out - constant_inn) * measuredDistances[i] / OAS_MAX_DISTANCE + constant_inn;

                    drawArcSegment(canvas, cx, cy, inn, constant_out, startAngle, sweepAngle, segmentPaints[i], null);

                    radarSegment.setDrawParameters(cx, cy, startAngle, sweepAngle, sinus, cosinus);
                }

                firstDraw = false;
            }
            else {

                float inn;

                for (int i = 0; i < radarSegments.size(); i++) {

                    RadarSegment radarSegment = radarSegments.get(i);

                    if ((sensorsStatus >> i & 1) == 1) {
                        drawArcSegment(canvas, (float) (radarSegment.getCx() - 20f * radarSegment.getCosinus()), (float) (radarSegment.getCy() - 20f * radarSegment.getSinus()), constant_inn + 15f, constant_out + 25f, radarSegment.getStartAngle(), radarSegment.getSweepAngle(), blackPaint, null);
                    }

                    drawArcSegment(canvas, radarSegment.getCx(), radarSegment.getCy(), constant_inn, constant_out, radarSegment.getStartAngle(), radarSegment.getSweepAngle(), greyPaint, null);

                    inn = (constant_out - constant_inn) * measuredDistances[i] / OAS_MAX_DISTANCE + constant_inn;

                    drawArcSegment(canvas, radarSegment.getCx(), radarSegment.getCy(), inn, constant_out, radarSegment.getStartAngle(), radarSegment.getSweepAngle(), segmentPaints[i], null);
                }

            }

        }
    }

    /**
     * Limit to make sure the circle segments don't become circles.
     */
    private static final float CIRCLE_LIMIT = 359.9999f;

    /**
     * Draws a thick arc between the defined angles, see {@link Canvas#drawArc} for more.
     * This method is equivalent to
     * <pre><code>
     * float rMid = (rInn + rOut) / 2;
     * paint.setStyle(Style.STROKE); // there's nothing to fill
     * paint.setStrokeWidth(rOut - rInn); // thickness
     * canvas.drawArc(new RectF(cx - rMid, cy - rMid, cx + rMid, cy + rMid), startAngle, sweepAngle, false, paint);
     * </code></pre>
     * but supports different fill and stroke paints.
     *
     * @param canvas view to draw in
     * @param cx horizontal middle point of the oval
     * @param cy vertical middle point of the oval
     * @param rInn inner radius of the arc segment
     * @param rOut outer radius of the arc segment
     * @param startAngle see {@link Canvas#drawArc}
     * @param sweepAngle see {@link Canvas#drawArc}, capped at &plusmn;360
     * @param fill filling paint, can be <code>null</code>
     * @param stroke stroke paint, can be <code>null</code>
     * @see Canvas#drawArc
     */
    private static void drawArcSegment(Canvas canvas, float cx, float cy, float rInn, float rOut, float startAngle,
                                       float sweepAngle, Paint fill, Paint stroke) {
        if (sweepAngle > CIRCLE_LIMIT) {
            sweepAngle = CIRCLE_LIMIT;
        }
        if (sweepAngle < -CIRCLE_LIMIT) {
            sweepAngle = -CIRCLE_LIMIT;
        }

        RectF outerRect = new RectF(cx - rOut, cy - rOut, cx + rOut, cy + rOut);
        RectF innerRect = new RectF(cx - rInn, cy - rInn, cx + rInn, cy + rInn);

        Path segmentPath = new Path();
        double start = Math.toRadians(startAngle);
        segmentPath.moveTo((float)(cx + rInn * Math.cos(start)), (float)(cy + rInn * Math.sin(start)));
        segmentPath.lineTo((float)(cx + rOut * Math.cos(start)), (float)(cy + rOut * Math.sin(start)));
        segmentPath.arcTo(outerRect, startAngle, sweepAngle);
        double end = Math.toRadians(startAngle + sweepAngle);
        segmentPath.lineTo((float)(cx + rInn * Math.cos(end)), (float)(cy + rInn * Math.sin(end)));
        segmentPath.arcTo(innerRect, startAngle + sweepAngle, -sweepAngle);
        if (fill != null) {
            canvas.drawPath(segmentPath, fill);
        }
        if (stroke != null) {
            canvas.drawPath(segmentPath, stroke);
        }
    }

}
