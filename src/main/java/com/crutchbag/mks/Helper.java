package com.crutchbag.mks;

import java.util.List;
import com.google.gson.Gson;

class Helper {
	
	private Helper() {
		throw new IllegalStateException("Utility class");
	}
	
	public static class Command {
		String command;
		List<String> args;
		
		public Command(String s, List<String> l) {
			this.clear();
			command = s;
			args = l;
		}
		
		public void clear() {
			command = null;
			args = null;
		}
	}
	
	public static String commandToJSON(Command c) {
		Gson gson = new Gson();
		return gson.toJson(c);
	}
	
	public static String commandListToJSON(List<Command> lc) {
		Gson gson = new Gson();
		return gson.toJson(lc);
	}
	
}
