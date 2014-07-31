package com.perforce.svn.prescan;

public class Progress {

	private long end;
	private long rev = 0;
	private int progress = 0;
	
	public Progress(long end) {
		this.end = end;
	}
	
	public void update(int r) {	
		int percent = (int) (rev * 100 / end);
		if (percent > progress) {
			progress = percent;
			System.out.print("Progress: " + percent + "%\r");
		}
		rev = r;
	}

	public boolean done() {
		return (rev > end);
	}
}
