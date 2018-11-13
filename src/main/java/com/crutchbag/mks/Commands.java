package com.crutchbag.mks;

import java.util.List;

public class Commands {

    private Mks mks;
    private Logger log;

    public Commands() {}
    public Commands(Mks mks) {
        this.mks = mks;
        this.log = mks.getLogger();
    }

    public void setApp(Mks a) {mks = a;}

    private String fmt(String f, Object... args) {return String.format(f, args);}

    @MQCommand
    public String setInputQueue(String s) {
        mks.setInputQueueName(s);
        return fmt("{\"setInputQueue\" : \"%s\", \"result\":\"ok\"}", s);
    }

    @MQCommand
    public String setLogQueue(String s) {
        mks.setLogQueueName(s);
        return fmt("{\"setLogQueue\" : \"%s\", \"result\":\"ok\"}", s);
    }

    @MQCommand
    public String setOutQueue(String s) {
        mks.setOutQueueName(s);
        return fmt("{\"setOutQueue\" : \"%s\", \"result\":\"ok\"}", s);
    }

    @MQCommand
    public String test(String a) {
        return fmt("{\"test\" : [\"%s\"]}",a);
    }

    @MQCommand
    public String test2(String a, String b) {
        return fmt("{\"test2\" : [\"arg1\",\"arg2\"]}", a, b);
    }

    @MQCommand
    public String test3(Integer a, Double b, Boolean c) {
        return fmt("{\"test3\" : [\"%s\",\"%s\",\"%s\"]}", a.toString(), b.toString(), c.toString());
    }

    @MQCommand
    public String testList(List<String> t) {
        return fmt("{\"test\" : [\"%s\"]}", t.toString());
    }

    @MQCommand
    public String testLoggerSpeed(Integer c) {
        Integer cnt = c;
        if (cnt < 1) cnt = 1;
        if (cnt < 1000) cnt = 1000;
        for (int i = 0; i < cnt; i++) log.info("msg #"+i);
        return log.getLogAndClear();
    }

    @MQCommand
    public String ping() {
        return "{\"pong\" : \"ok\"}";
    }

    @MQCommand
    public String getName() {
        return fmt("{\"appName\":\"%s\"}",mks.getName());
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
