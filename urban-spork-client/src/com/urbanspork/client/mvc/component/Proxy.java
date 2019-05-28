package com.urbanspork.client.mvc.component;

import java.awt.TrayIcon.MessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.client.Client;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.config.ServerConfig;

import io.netty.util.internal.StringUtil;

public class Proxy {

	private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

	private static Thread launcher;

	public static void launchClient() {
		ServerConfig config = Resource.config.getCurrent();
		if (config != null) {
			launcher = new Thread(() -> {
				try {
					Client.launch(Resource.config);
				} catch (InterruptedException e) {
					Thread thread = Thread.currentThread();
					logger.info("[{}-{}] was interrupted by relaunch", thread.getName(), thread.getId());
				} catch (Exception e) {
					logger.error(StringUtil.EMPTY_STRING, e);
				}
			});
			launcher.setName("Client-Launcher");
			launcher.setDaemon(true);
			launcher.start();
			logger.debug("[{}-{}] start", launcher.getName(), launcher.getId());
			String message = Resource.config.getCurrent().toString();
			Tray.displayMessage("Proxy is running", message, MessageType.INFO);
			Tray.setToolTip(message);
		} else {
			Tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
		}
	}

	public static void relaunchClient() {
		if (launcher != null) {
			launcher.interrupt();
		}
		launchClient();
	}

	public static Thread getLauncher() {
		return launcher;
	}

}
