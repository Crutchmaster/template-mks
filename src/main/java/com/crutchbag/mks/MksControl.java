package com.crutchbag.mks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public class Pair<A, B> {
        public A a;
        public B b;
        public Pair(A a, B b) {
            this.a = a; this.b = b;
        }
        public Pair<A, B> set(A pa, B pb) {
            a = pa;
            b = pb;
            return this;
        }
    }

    private CommandReturn ret = new CommandReturn();
    private HashMap<String, Call> commandsList;
    private HashMap<String, String> argMap;
    private ObjectMapper om = new ObjectMapper();
    private Mks mks;
    private static final String JSON_ERROR_MESSAGE = "Json parsing error. Expected {\"command\":\"<command name>, \"args\" : \"[<arguments>]\"}\n";


    public MksControl(Mks mks) {
        this.mks = mks;
        commandsList = new HashMap<String, Call>();
        argMap = new HashMap<String, String>();
        argMap.put("java.lang.Integer", "int");
        argMap.put("int", "int");
        argMap.put("long", "int");
        argMap.put("java.lang.String", "string");
        argMap.put("java.lang.Double", "float");
        argMap.put("float", "float");
        argMap.put("double", "float");
        argMap.put("java.lang.Boolean", "bool");
        argMap.put("boolean", "bool");
    }

    private String fmt(String f, Object... x) {return String.format(f, x);}

    public void feed(Object tp) {
        Method[] ms = tp.getClass().getMethods();
        for (Method m : ms) {
            if (m.isAnnotationPresent(MQCommand.class)) {
                commandsList.put(m.getName(), new Call(tp, m));
            }
        }
    }

    //TODO return list, move json to Commands.
    public String getCommandList() {
        ObjectNode obj =  om.createObjectNode();
        ArrayNode arr = om.createArrayNode();
        for (String s : commandsList.keySet()) arr.add(s);
        obj.putArray("commands").addAll(arr);
        return obj.toString();
    }

    public CommandReturn control(String msg) {
        String commandStr = null;
        ArrayNode args = null;
        Object retObj = "";
        try {
            JsonNode in = om.readTree(msg);
            JsonNode cmdArgs = in.get("args");
            if (cmdArgs==null) {
                args = om.createArrayNode();
            } else if (!cmdArgs.isArray() && !cmdArgs.isObject()) {
                args = om.createArrayNode();
                args.add(cmdArgs);
            } else if (cmdArgs.isArray()) {
                args = (ArrayNode)cmdArgs;
            }
            JsonNode commandNode = in.get("command");

            if (commandNode!=null && commandNode.isTextual()) {
                commandStr = commandNode.asText();
            } else {
                return ret.err(JSON_ERROR_MESSAGE.concat("Input message is:"+msg+"\n"));
            }
        } catch (Exception e) {
            return ret.err(JSON_ERROR_MESSAGE.concat("Input message is:"+msg+"\n"));
        }

        Call c = commandsList.get(commandStr);
        if (c == null) return ret.err("Command not found:"+commandStr);
        Pair<List<Object>, String> argsRet = buildArgs(c.method, args);
        if (argsRet.a != null) {
            try {
                retObj = c.method.invoke(c.object, argsRet.a.toArray());
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
        return ret.err(argsRet.b);
    }

    private Pair<List<Object>, String> buildArgs(Method m, ArrayNode args) {
        Pair<List<Object>, String> rt = new Pair<List<Object>, String>(new ArrayList<Object>(), "");
        int cnt = m.getParameterCount();
        if (cnt < args.size()) {
            return rt.set(null, fmt("Need more args (%d vs %d)", cnt, args.size()));
        }
        Class<?>[] types = m.getParameterTypes();
        int i = 0;
        String typeName = "";
        Pair<Object, String> rs;
        for (Class<?> t : types) {
            JsonNode arg = args.get(i);
            if (arg == null) return rt.set(null, fmt("Input argument #is null.",i));
            if (t.isAssignableFrom(List.class)) {
                if (!arg.isArray()) {
                    return rt.set(null, fmt("Argument #%d is array, but input argument is not array.\n"
                            + "Input argument:", i, arg.toString()));
                }
                ParameterizedType pt = (ParameterizedType)m.getGenericParameterTypes()[i];
                Type[] parTypes = pt.getActualTypeArguments();
                if (parTypes.length != 1) {
                    return rt.set(null,
                            fmt("Argument #%d: only one generic type allowed\n. Type is %s",
                                    i, pt.getTypeName()));
                }
                typeName = parTypes[0].getTypeName();
                List<Object> parArray = new ArrayList<Object>();
                for (JsonNode e : (ArrayNode)arg) {
                    rs = parseArg(typeName, e);
                    if (rs.a == null) return rt.set(null, fmt("Argument #%d %s", i, rs.b));
                    parArray.add(rs.a);
                }
                rt.a.add(parArray);
            } else {
                typeName = t.getName();
                rs = parseArg(typeName, arg);
                if (rs.a == null) return rt.set(null, fmt("Argument #%d %s", i, rs.b));
                rt.a.add(rs.a);
            }
            i++;
        }
        return rt;
    }

    private Pair<Object, String> parseArg(String type, JsonNode arg) {
        Object retObj = null;
        Pair<Object, String> rt = new Pair<Object, String>(retObj, "");
        if (arg.isArray() || arg.isObject()) {
            return rt.set(null,
                    fmt(" input argument for %s is object or array\nInput argument:%s"
                            ,type, arg.toString()));
        }
        String parseMethod = argMap.get(type);
        if (parseMethod == null) return rt.set(null, "no parser for class of "+type);
        String str = arg.asText();
        switch (parseMethod) {
        case "string" :
            rt.a = str;
            break;
        case "bool":
            rt.a = arg.asBoolean();
            break;
        case "float" :
            try {rt.a = Double.valueOf(str);}
            catch (Exception e) {
                return rt.set(null, fmt(" parse error for type %s\nInput argument:%s",type,str));
            }
            break;
        case "int" :
            try {rt.a = Integer.valueOf(str);}
            catch (Exception e) {
                return rt.set(null, fmt(" parse error for type %s\nInput argument:%s",type,str));
            }
            break;
        default :
            return rt.set(null, fmt(" parse method %s for %s is not implemented.", parseMethod, type));
        }
        return rt;
    }

}
