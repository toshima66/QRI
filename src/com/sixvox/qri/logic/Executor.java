package com.sixvox.qri.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

public class Executor {
	private static Logger l = Logger
			.getLogger(Executor.class.getCanonicalName());

	Path qrpath;
	String command;
	LinkedHashMap<String, String> args;

	public Executor() {}

	public Executor(Path qrpath, String urlscheme) {
		this.qrpath = qrpath;
		this.command = urlscheme.replaceAll(".*qri://", "").replaceAll("/\\?.*", "");
		String query = urlscheme.replaceAll(".*\\?", "");
		this.args = splitQuery(query);
	}

	public LinkedHashMap<String, String> splitQuery(String query) {
		LinkedHashMap<String, String> query_pairs = new LinkedHashMap<String, String>();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			String value = pair.substring(idx + 1);
			if (value.equals("$QR_PATH")) {
				value = this.qrpath.toString();
			}
			query_pairs.put(pair.substring(0, idx), value);
		}
		return query_pairs;
	}

	public void execute() throws IOException {
		l.info("execute: " + toString());
		List<String> list = new ArrayList<String>();
		list.add(command);
		list.addAll(args.values());
		ProcessBuilder pb = new ProcessBuilder(list);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		String output = getPrints(p);

		l.info("戻り値：" + p.exitValue());
		l.info("出力：" + output);
	}

	public static String getPrints(Process p) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (InputStream is = p.getInputStream(); BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
			for (;;) {
				String line = br.readLine();
				if (line == null) { break; }
				sb.append(line);
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Executor [qrpath=" + qrpath + ", command=" + command
				+ ", args=" + args + "]";
	}

}
