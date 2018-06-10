package com.gabriel.tcc;

import android.app.Application;

public class Preferences{

    private Preferences(){
        this.low = "25";
        this.mid = "50";
        this.high = "75";
        this.stepMax = "2000";
        this.phoneNumber = "31991949309";
        this.time = "10";
        this.stepsInterval = "20";
    }

    private static Preferences instance;

    private String low;
    private String mid;
    private String high;
    private String stepMax;
    private String phoneNumber;
    private String time;
    private String stepsInterval;


    public String getLow() {
        return low;
    }

    public String getMid() {
        return mid;
    }

    public String getHigh() {
        return high;
    }

    public String getstepMax() {
        return stepMax;
    }

    public String getphoneNumber() {
        return phoneNumber;
    }

    public String getTime() {
        return time;
    }

    public String getStepsInterval() {
        return stepsInterval;
    }

    public void setLow(String s){
        this.low = s;
    }

    public void setMid(String s){
        this.mid = s;
    }

    public void setHigh(String s){
        this.high = s;
    }

    public void setstepMax(String s){
        this.stepMax = s;
    }

    public void setphoneNumber(String s){
        this.phoneNumber = s;
    }

    public void setTime(String s){
        this.time = s;
    }

    public void setStepsInterval(String s){
        this.stepsInterval = s;
    }

    public static synchronized Preferences getInstance(){
        if(instance==null){
            instance=new Preferences();
        }
        return instance;
    }
}