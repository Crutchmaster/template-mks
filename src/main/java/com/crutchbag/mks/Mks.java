package com.crutchbag.mks;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class Mks {

    //public String id = Integer.toHexString((int)(Math.random()*0x1000000));
    private String name;
    private String logQueueName;
    private String controlQueueIn;
    private String outQueueName;
    private ObjectMapper json = new ObjectMapper();
    //private HashMap<String, Object> args;
    private final String LOG_SUFFIX = "_log";
    private final String IN_SUFFIX = "_control";
    private final String OUT_SUFFIX = "_out";

    @Autowired
    private RabbitTemplate amqp;

    private MksControl mksControl;

    public void log(String str) {
        System.out.println(str);
    }

    public void feed(Object food) {
        mksControl.feed(food);
    }


    @Bean
    public RabbitTemplate amqpTemplate(ConnectionFactory cf) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setReplyTimeout(1000*1000);
        return tpl;
    }

    public Mks(@Value("${name}") String appName) {
        this.name = appName;
        this.logQueueName = appName + LOG_SUFFIX;
        this.controlQueueIn = appName + IN_SUFFIX;
        this.outQueueName = appName + OUT_SUFFIX;
        this.mksControl = new MksControl(this);
        //args = new HashMap<String, Object>();
        //args.put("x-max-length-bytes", new Integer(1024*1024*16)); //16MB
        Commands cmds = new Commands(this);
        this.feed(cmds);
    }

    public void sendLog(String str) {
        amqp.convertAndSend(logQueueName, str);
    }

    public void sendOut(String str) {
        amqp.convertAndSend(outQueueName, str);
    }

    public String sendAndReceive(String appName, String cmdName, Object... args) {
        ObjectNode msg = json.createObjectNode();
        ArrayNode arr = msg.putArray(cmdName);
        for (Object arg : args) arr.add(arg.toString());
        return sendAndReceive(appName + IN_SUFFIX, msg.toString());
    }

    public String sendAndReceive(String queue, String str) {
        Message msg = new Message(str.getBytes(), new MessageProperties());
        return new String(amqp.sendAndReceive(queue, msg).getBody());
    }

    //public String getId() {return id;}
    public String getName() {return name;}
    public void setLogQueueName(String s) {this.logQueueName = s;}
    public void setControlQueueName(String s) {this.controlQueueIn = s;}
    public void setOutQueueName(String s) {this.outQueueName = s;}

    @Bean
    public ConnectionFactory connectionFactory(
            @Value("${amqp_host}") String host,
            @Value("${amqp_user}") String user,
            @Value("${amqp_password}") String password) {
        log("RMQ host:"+host);
        log("App name:"+getName());
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(host);
        connectionFactory.setUsername(user);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @RabbitListener(queues = "#{inputQueue.name}")
    private void cmdReceive(Message message) {
        String msg = new String(message.getBody());
        String reply_to = message.getMessageProperties().getReplyTo();
        CommandReturn ret = mksControl.control(msg);
        if (!ret.error) {
            if (reply_to != null) {
                sendLog("Reply to "+reply_to+" \nMessage:\n"+ret.body);
                Message replymsg = new Message(ret.body.getBytes(), new MessageProperties());
                replymsg.getMessageProperties().setCorrelationId(message.getMessageProperties().getCorrelationId());
                amqp.send(reply_to, replymsg);
            }
            sendOut(ret.body);
            sendLog(ret.log);
            return;
        }
        //sendErr
        sendLog(ret.log);
    }

    public String getCommands() {
        return mksControl.getCommandList();
    }

    @Bean
    private Queue inputQueue() {
        return new Queue(controlQueueIn, false, false, false);
    }

    @Bean
    private Queue genLogQueue() {
        return new Queue(logQueueName, false, false, false);
    }

    @Bean
    private Queue outQueue() {
        return new Queue(outQueueName, false, false, false);
    }



}
