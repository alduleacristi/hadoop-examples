package ro.unitbv.fmi.help;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class MedianCommentCountTouple implements Writable {
	private float median;
	private float standardDeviation;

	public float getMedian() {
		return median;
	}

	public void setMedian(float median) {
		this.median = median;
	}

	public float getStandardDeviation() {
		return standardDeviation;
	}

	public void setStandardDeviation(float standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	public void write(DataOutput out) throws IOException {
		out.writeFloat(median);
		out.writeFloat(standardDeviation);

	}

	public void readFields(DataInput in) throws IOException {
		median = in.readFloat();
		standardDeviation = in.readFloat();
	}

	@Override
	public String toString() {
		return median + "\t" + standardDeviation;
	}
	
	
}
