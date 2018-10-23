package com.crutchbag.mks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MksControl {

    public class Call {
        public Object object;
        public Method method;
        public Call(Object obj, Method mt) {
            object = obj; method = mt;
        }
    }

    public class Arg {
        public String stringValue;
        public Integer intValue;
        public Boolean boolValue;
        public Double floatValue;
        public Arg() {
            this.clear();
        }
        public void clear() {
            stringValue = null;
            intValue = null;
            boolValue = null;
            floatValue = null;
        }
        public void set(String s, Integer i, Boolean b, Double d) {
            stringValue = s;
            intValue = i;
            boolValue = b;
            floatValue = d;
        }
    }

    private CommandReturn ret = new CommandReturn();
    private HashMap<String, Call> commandsList;
    private ArrayList<Arg> argList;
    private ObjectMapper om = new ObjectMapper();

    public MksControl() {
        commandsList = new HashMap<String, Call>();
        argList = new ArrayList<Arg>(10);
        for (int i = 0; i < 10; i++) argList.add(new Arg());
    }

    public void feed(Object tp) {
        Method[] ms = tp.getClass().getMethods();
        for (Method m : ms) {
            if (m.isAnnotationPresent(MQCommand.class)) {
                commandsList.put(m.getName(), new Call(tp, m));
            }
        }
    }

    public String getCommandList() {
        ObjectNode obj =  om.createObjectNode();
        ArrayNode arr = om.createArrayNode();
        for (String s : commandsList.keySet()) arr.add(s);
        obj.putArray("commands").addAll(arr);
        return obj.toString();
    }

    public String parseError(int index, String value, String asWhat) {
        return "Parameter #"+index+" value '"+value+"' won't parsed as "+asWhat+"\n";
    }

    public CommandReturn control(String msg) {
        String commandStr = null;
        ArrayList<String> args = new ArrayList<String>(10);
        Object retObj = "";
        try {
            JsonNode in = om.readTree(msg);
            commandStr = in.fieldNames().next();
            if (in.get(commandStr).isArray()) {
                for(JsonNode arg : in.get(commandStr)) {
                    args.add(arg.asText());
                }
            }
        } catch (Exception e) {
            return ret.err("Json parsing error. Json is:"+msg);
        }

        Call c = commandsList.get(commandStr);
        if (c == null) return ret.err("Command not found:"+commandStr);

        int cnt = c.method.getParameterCount();
        if (cnt <= args.size()) {
            Arg arg;
            for (int i = 0; i < cnt; i++) {
                String ps = args.get(i);
                Integer pi = null;
                Double pd = null;
                try {pi = Integer.parseInt(ps);} catch (Exception e) {};
                try {pd = Double.parseDouble(ps);} catch (Exception e) {};

                arg = argList.get(i);
                arg.clear();
                arg.boolValue = Boolean.parseBoolean(ps);
                arg.stringValue = ps;
                arg.intValue = pi;
                arg.floatValue = pd;
            }

            boolean error = false;
            String errStr = "";
            Vector<Object> sendArgs = new Vector<Object>();
            Class<?>[] types = c.method.getParameterTypes();
            int parIndex = 0;
            for (Class<?> t : types) {
                arg = argList.get(parIndex);
                if (t.equals(String.class)) {
                    sendArgs.add(arg.stringValue);
                } else if (t.equals(Boolean.class) || t.getName().equals("boolean")) {
                    sendArgs.add(arg.boolValue);
                } else if (t.equals(Integer.class) ||
                        t.getName().equals("int") || t.getName().equals("long")) {
                    if (arg.intValue != null) {
                        sendArgs.add(arg.intValue);
                    } else {
                        error = true;
                        errStr += parseError(parIndex, arg.stringValue, "integer");
                    }
                } else if (t.equals(Double.class) ||
                        t.getName().equals("float") || t.getName().equals("double")) {
                    if (arg.floatValue != null) {
                        sendArgs.add(arg.floatValue);
                    } else {
                        error = true;
                        errStr += parseError(parIndex, arg.stringValue, "double");
                    }
                }
                parIndex++;
            }

            ret.err(errStr);
            if (!error) {
                try {
                    retObj = c.method.invoke(c.object, sendArgs.toArray());
                } catch (Exception e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String trace = errors.toString();
                    return ret.err("Error when calling " + commandStr + ":"
                            + e.getClass().getName() + " - " + e.getMessage()
                            + "\nStackTrace:\n" + trace);
                }
                return ret.ok(retObj.toString());
            }
            return ret;
        }
        return ret.err("Need more args for "+commandStr+"("+cnt+" vs "+args.size()+")");
    }

}
