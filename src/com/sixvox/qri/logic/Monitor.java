package com.sixvox.qri.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.google.zxing.Result;

public class Monitor implements Runnable {

	private static Logger l = Logger
			.getLogger(Monitor.class.getCanonicalName());

	/**
	 * 監視先ディレクトリ
	 */
	public String dirName;

	/**
	 * イベントごとのウェイト時間(秒)
	 */
	public int waitCnt;

	/**
	 * オプションの修飾子
	 */
	private WatchEvent.Modifier[] extModifiers = {};

	/**
	 * 実行部
	 */
	@Override
	public void run() {
		try {
			Path dirPath = new File(dirName).toPath();
			l.info(String.format("監視先: %s, 待機時間: %d\n", dirName, waitCnt));

			// ディレクトリが属するファイルシステムを得る
			FileSystem fs = dirPath.getFileSystem();

			// ファイルシステムに対応する監視サービスを構築する.
			// (一つのサービスで複数の監視が可能)
			try (WatchService watcher = fs.newWatchService()) {
				watch(dirPath, watcher);
			}
		} catch (RuntimeException | IOException ex) {
			ex.printStackTrace();
		}
		l.info("スレッドの終了");
	}

	private void watch(Path dirPath, WatchService watcher) throws IOException {
		// ディレクトリに対して監視サービスを登録する.
		WatchKey watchKey = dirPath.register(watcher, new Kind[] {
				StandardWatchEventKinds.ENTRY_CREATE, // 作成
				StandardWatchEventKinds.ENTRY_MODIFY, // 変更
				StandardWatchEventKinds.ENTRY_DELETE, // 削除
				StandardWatchEventKinds.OVERFLOW }, // 特定不能時
				extModifiers); // オプションの修飾子、不要ならば空配列

		// 監視が有効であるかぎり、ループする.
		// (監視がcancelされるか、監視サービスが停止した場合はfalseとなる)
		while (watchKey.isValid()) {
			try {
				// スレッドの割り込み = 終了要求を判定する.
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}

				// ファイル変更イベントが発生するまで待機する.
				WatchKey detecedtWatchKey = watcher.poll(500,
						TimeUnit.MILLISECONDS);
				if (detecedtWatchKey == null) {
					continue;
				}

				// イベント発生元を判定する
				if (detecedtWatchKey.equals(watchKey)) {
					for (WatchEvent<?> event : detecedtWatchKey.pollEvents()) {
						// 追加・変更・削除対象のファイルを取得する.
						// (ただし、overflow時などはnullとなることに注意)
						Path file = (Path) event.context();
						l.info(event.kind() + ": count=" + event.count()
								+ ": path=" + file);
						// 処理開始
						if (StandardWatchEventKinds.ENTRY_CREATE.equals(event
								.kind())
								|| StandardWatchEventKinds.ENTRY_MODIFY
										.equals(event.kind())) {
							logic(new File(dirPath.toString() + "/"
									+ file.getFileName()).toPath());
						}
					}
				}

				// イベントのハンドリングに時間がかかるケースを
				// Sleepでエミュレートする.
				// (この間のファイル変更イベントを取りこぼすか否かを確かめられる)
				for (int cnt = 0; cnt < waitCnt; cnt++) {
					l.info(String.format("%d/%d...\r", cnt + 1, waitCnt));
					Thread.sleep(1000);
				}

				// イベントの受付を再開する.
				detecedtWatchKey.reset();

			} catch (InterruptedException ex) {
				// スレッドの割り込み = 終了要求なので監視をキャンセルしループを終了する.
				l.info("監視のキャンセル");
				watchKey.cancel();
			}
		}
	}

	private void logic(Path path) throws InterruptedException {
		try {
			QRDecode qrd = new QRDecode();
			Result result = qrd.decode(path.toUri());
			if (result == null) {
				l.info("画像ファイルでないか、バーコードでないファイルです。: " + path);
				return;
			}

			String message = result.getText().trim();
			l.info("バーコード・メッセージ: [" + message + "]");
			// message の先頭に変な文字が入っているので startsWith は使えない
			String keyword = "qri://";
			if (!message.contains(keyword)) {
				l.info("バーコードを受信しましたが対象外のメッセージでした。" + message);
				return ;
			}

			// 処理を追い越すことがあるようなので、ファイル転送が終わるまでしばらく待つ
			l.info("処理開始まで5秒待機します。");
//			TimeUnit.SECONDS.sleep(5L);//TODO debug

			// 処理開始
			l.info("処理開始。");
			Executor exe = new Executor(path, message);
			exe.execute();
		} catch (IOException e) {
			l.log(Level.WARNING, "qrコードのデコードに失敗しました", e);
		}
	}
	
	@Test
	public void test001() throws Exception {
		String str = "qri://cmd/sendmail.rb/?$1=今から帰ります&$2=info@6vox.com&$3=$QR_PATH";
		System.out.println(str.startsWith("qri://"));
		List<String> list = new ArrayList<String>();
		list.add("java");
		list.add("-version");
		ProcessBuilder pb = new ProcessBuilder(list);

		pb.redirectErrorStream(true);

		Process p = pb.start();

		printInputStream(p.getInputStream());

//		p.waitFor();
		System.out.println("戻り値：" + p.exitValue());
	}
	public static void printInputStream(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			for (;;) {
				String line = br.readLine();
				if (line == null) break;
				System.out.println(line);
			}
		} finally {
			br.close();
		}
	}
}