package com.sixvox.qri;

import java.io.Console;
import java.io.IOException;
import java.util.logging.LogManager;

import com.sixvox.qri.logic.Monitor;

public class QRIMain {

	public static void logconfig() {
		try {
			LogManager.getLogManager().readConfiguration(
					QRIMain.class.getResourceAsStream("qri.properties"));
		} catch (IOException | NoClassDefFoundError e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		logconfig();

		// 監視先
		String dirName = "./monitor";
		// イベントごとのウェイト
		int waitCnt = 0;

		// オプションを解析する.
		int mx = args.length;
		for (int idx = 0; idx < mx;) {
			String arg = args[idx];
			dirName = arg;
			break;
		}

		// スレッドの開始
		Monitor monitor = new Monitor();
		monitor.dirName = dirName;
		monitor.waitCnt = waitCnt;
		Thread thread = new Thread(monitor);
		thread.start();

		// エンターキーが押されるまで実行(コンソールがある場合)
		Console cons = System.console();
		if (cons != null) {
			cons.printf("エンターキーで終了.\n");
			cons.readLine();

			// スレッドへの終了要求と終了待機
			thread.interrupt();
		}

		// スレッド終了まで待機
		thread.join();
	}

}
