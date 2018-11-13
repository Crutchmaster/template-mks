package com.crutchbag.mks;

import java.util.HashSet;
import java.util.Vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Logger {
    private Vector<ObjectNode> messages = new Vector<ObjectNode>();
    private long time = -1;
    private boolean enable = false;
    private boolean active = false;
    private boolean std = false;
    private HashSet<String> activators = new HashSet<String>();
    private HashSet<String> deactivators = new HashSet<String>();

    private ObjectMapper json = new ObjectMapper();

    //private static final int SBBUFFER = 1024;
    //private StringBuilder sb = new StringBuilder(SBBUFFER);
    //private void sbclear() {sb.delete(0, SBBUFFER-1);}

    private void stdout(String s) {System.out.println(s);}

    public boolean info (String msg) {return send("info",  msg);}
    public boolean warn (String msg) {return send("warn",  msg);}
    public boolean error(String msg) {return send("error", msg);}

    public boolean send(String group, String msg) {
        if (!enable) return false;
        if (!active) active = !activators.isEmpty() && (activators.contains("*") ||
                                                        activators.contains(group) ||
                                                        activators.contains(msg));
        if (!active) return false;
        long acttime = time == -1 ? -1 : System.nanoTime()/1000 - time;
        time = System.nanoTime()/1000;
        ObjectNode ret = json.createObjectNode();
        ret.put("duration", acttime);
        ret.put("group", group);
        ret.put("msg", msg);
        if (!std) messages.add(ret);
             else stdout(ret.toString());
        active = !(!deactivators.isEmpty() && (deactivators.contains("*") ||
                                               deactivators.contains(group) ||
                                               deactivators.contains(msg)));
        //if (!active) time = -1;
        return true;
    }

    private String jsonResponse(String msg) {
        ObjectNode ret = json.createObjectNode();
        ret.put("msg", msg);
        return ret.toString();
    }

    private String getLogJson() {
        ArrayNode ret = json.createArrayNode();
        ret.addAll(messages);
        return ret.toString();
    }

    private void clearLog() {
        messages.clear();
    }

    @MQCommand
    public String addActivator(String s) {activators.add(s); return jsonResponse("Add activator "+s);}
    @MQCommand
    public String addDeactivator(String s) {deactivators.add(s); return jsonResponse("Add deactivator "+s);}
    @MQCommand
    public String clearActivators(String s) {activators.clear(); return jsonResponse("Activators list cleared");}
    @MQCommand
    public String clearDeactivators(String s) {deactivators.clear(); return jsonResponse("Deactivators list cleared");}
    @MQCommand
    public String enableLogger() {this.enable = true; return jsonResponse("Logger enabled");}
    @MQCommand
    public String disableLogger() {this.enable = false; return jsonResponse("Logger disabled");}
    @MQCommand
    public String logToStdout() {this.std = true; return jsonResponse("Log to stdout");}
    @MQCommand
    public String logToList() {this.std = false; return jsonResponse("Log to list");}

    @MQCommand
    public String getActivators() {
        ArrayNode ret = json.createArrayNode();
        for (String s : activators) ret.add(s);
        return ret.toString();
    }

    @MQCommand
    public String getDeactivators() {
        ArrayNode ret = json.createArrayNode();
        for (String s : deactivators) ret.add(s);
        return ret.toString();
    }

    @MQCommand
    public String getLogAndClear() {
        String ret = getLogJson();
        clearLog();
        return ret;
    }
}
