package anon.util;

public interface ProgressCapsule {

	
	public final static int PROGRESS_FINISHED = 0;
	public final static int PROGRESS_ONGOING = 1;
	public final static int PROGRESS_ABORTED = 2;
	
	public int getMaximum();
	
	public int getMinimum();
	
	public int getValue();
	
	public int getStatus();
}
