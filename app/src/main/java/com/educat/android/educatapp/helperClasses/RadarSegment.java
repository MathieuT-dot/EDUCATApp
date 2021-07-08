package com.educat.android.educatapp.helperClasses;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * RadarSegment
 *
 * Class that defines one segment of the
 * {@link com.educat.android.educatapp.helperClasses.DynamicRadar}
 */
public class RadarSegment implements Parcelable {

    private float x;
    private float y;
    private float r;
    private int instrumentId;
    private int valueIndex;
    private float boundaryCalibration;

    private float cx;
    private float cy;
    private float startAngle;
    private float sweepAngle;
    private double sinus;
    private double cosinus;

    /**
     * Creates a RadarSegment.
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param r rotation
     */
    public RadarSegment(float x, float y, float r) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.valueIndex = 0;
    }

    public RadarSegment(float x, float y, float r, int instrumentId) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.instrumentId = instrumentId;
        this.valueIndex = 0;
    }

    public RadarSegment(float x, float y, float r, int instrumentId, int valueIndex, float boundaryCalibration) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.instrumentId = instrumentId;
        this.valueIndex = valueIndex;
        this.boundaryCalibration = boundaryCalibration;
    }


    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getR() {
        return r;
    }

    public void setR(float r) {
        this.r = r;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(int instrumentId) {
        this.instrumentId = instrumentId;
    }

    public int getValueIndex() {
        return valueIndex;
    }

    public void setValueIndex(int valueIndex) {
        this.valueIndex = valueIndex;
    }

    public float getBoundaryCalibration() {
        return boundaryCalibration;
    }

    public void setBoundaryCalibration(float boundaryCalibration) {
        this.boundaryCalibration = boundaryCalibration;
    }

    public float getCx() {
        return cx;
    }

    public void setCx(float cx) {
        this.cx = cx;
    }

    public float getCy() {
        return cy;
    }

    public void setCy(float cy) {
        this.cy = cy;
    }

    public float getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(float startAngle) {
        this.startAngle = startAngle;
    }

    public float getSweepAngle() {
        return sweepAngle;
    }

    public void setSweepAngle(float sweepAngle) {
        this.sweepAngle = sweepAngle;
    }

    public double getSinus() {
        return sinus;
    }

    public void setSinus(double sinus) {
        this.sinus = sinus;
    }

    public double getCosinus() {
        return cosinus;
    }

    public void setCosinus(double cosinus) {
        this.cosinus = cosinus;
    }

    public void setDrawParameters(float cx, float xy, float startAngle, float sweepAngle, double sinus, double cosinus) {
        this.cx = cx;
        this.cy = xy;
        this.startAngle = startAngle;
        this.sweepAngle = sweepAngle;
        this.sinus = sinus;
        this.cosinus = cosinus;
    }

    private RadarSegment(Parcel in) {
        x = in.readFloat();
        y = in.readFloat();
        r = in.readFloat();
        instrumentId = in.readInt();
        valueIndex = in.readInt();
        boundaryCalibration = in.readFloat();
        cx = in.readFloat();
        cy = in.readFloat();
        startAngle = in.readFloat();
        sweepAngle = in.readFloat();
        sinus = in.readDouble();
        cosinus = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeFloat(r);
        dest.writeInt(instrumentId);
        dest.writeInt(valueIndex);
        dest.writeFloat(boundaryCalibration);
        dest.writeFloat(cx);
        dest.writeFloat(cy);
        dest.writeFloat(startAngle);
        dest.writeFloat(sweepAngle);
        dest.writeDouble(sinus);
        dest.writeDouble(cosinus);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<RadarSegment> CREATOR = new Parcelable.Creator<RadarSegment>() {
        @Override
        public RadarSegment createFromParcel(Parcel in) {
            return new RadarSegment(in);
        }

        @Override
        public RadarSegment[] newArray(int size) {
            return new RadarSegment[size];
        }
    };
}