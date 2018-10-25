package com.crutchbag.mks;

import java.util.List;

public class Commands {

    private Mks mks;

    public Commands() {}
    public Commands(Mks mks) {this.mks = mks;}

    public void setApp(Mks a) {mks = a;}

    @MQCommand
    public String setControlQueue(String s) {
        mks.setControlQueueName(s);
        return "Ok, control queue is "+s+" now.";
    }

    @MQCommand
    public String setLogQueue(String s) {
        mks.setLogQueueName(s);
        return "Ok, log queue is "+s+" now.";
    }

    @MQCommand
    public String setOutQueue(String s) {
        mks.setOutQueueName(s);
        return "Ok, out queue is "+s+ " now.";
    }

    @MQCommand
    public String test(String a) {
        return "Run test: arg:"+a;
    }

    @MQCommand
    public String test2(String a, String b) {
        return "Run test2: arg1:"+a+" arg2:"+b;
    }

    @MQCommand
    public String test3(Integer a, Double b, Boolean c) {
        return "Run test3: arg1:"+a+" arg2:"+b+" arg3:"+c;
    }

    @MQCommand
    public String testList(List<String> t) {
        String str = "";
        for (String s : t) {str += s + ";";}
        return "Run testList: arg:"+str;
    }

    @MQCommand
    public String ping() {
        return "pong";
    }

    @MQCommand
    public String getName() {
        return "{\"appName\":\""+mks.getName()+"\"}";
    }

    @MQCommand
    public String getCommands() {
        return mks.getCommands();
    }

    @MQCommand
    public String sendAndRecTest(String queue, String msg) {
        return mks.sendAndReceive(queue, msg);
    }

}
