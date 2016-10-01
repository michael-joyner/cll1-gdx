package com.cherokeelessons.cll2ev1.screens;

import java.util.HashMap;

import com.badlogic.gdx.utils.Logger;

@SuppressWarnings("serial")
public class RefCounts extends HashMap<String, Integer>{
	private final Logger log = new Logger(this.getClass().getSimpleName());
	public RefCounts() {
		log.setLevel(Logger.NONE);
	}
	public void inc(String key) {
		synchronized (this) {
			if (!containsKey(key)){
				put(key,0);
			}
			put(key,get(key)+1);
			log.info(key+": "+get(key));
		}
	}
	public void dec(String key) {
		synchronized (this) {
			if (!containsKey(key)){
				put(key,0);
			}
			put(key,Math.max(0, get(key)-1));
			log.info(key+": "+get(key));
		}
	}
}