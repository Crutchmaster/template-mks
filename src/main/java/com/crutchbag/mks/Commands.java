package com.crutchbag.mks;

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
    public String ping() {
        return "pong";
    }

    @MQCommand
    public String getName() {
        return "{\"appName\":\""+mks.getName()+"\"}";
    }

    @MQCommand
    public String getCommandsList() {
        return mks.getCommandsList();
    }
    
    @MQCommand
    public String getCommandArgs(String a) {
        return mks.getCommandArgs(a);
    }
    
    @MQCommand
    public String getCommandsWithArgs() {
    	return mks.getCommandsWithArgs();
    }

    @MQCommand
    public String sendAndRecTest(String queue, String msg) {
        return mks.sendAndReceive(queue, msg);
    }

}
