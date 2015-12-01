package ro.unitbv.fmi.jobs;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import ro.unitbv.fmi.help.MRDPUtils;
import ro.unitbv.fmi.help.MedianCommentCountTouple;

public class MedianCommentCountJob {

	public static class MedianCommentCountMapper extends Mapper<Object, Text, IntWritable, IntWritable> {
		private IntWritable outHour = new IntWritable();
		private IntWritable outCommentLength = new IntWritable();

		private final static SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

		public void map(Object key, Text value, Context context) {
			Map<String, String> parsed = MRDPUtils.xmlToMap(value.toString());

			String strDate = parsed.get("CreationDate");
			String text = parsed.get("Text");

			if (strDate == null || text == null) {
				return;
			}

			try {
				Date creationDate = frmt.parse(strDate);

				outHour.set(creationDate.getHours());
				outCommentLength.set(text.length());

				context.write(outHour, outCommentLength);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static class MedianCommentCountReducer
			extends Reducer<IntWritable, IntWritable, IntWritable, MedianCommentCountTouple> {
		private MedianCommentCountTouple result = new MedianCommentCountTouple();
		private List<Float> commentLengths = new ArrayList<Float>();

		public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			float sum = 0;
			float count = 0;
			commentLengths.clear();
			result.setStandardDeviation(0.0f);

			for (IntWritable val : values) {
				commentLengths.add((float) val.get());
				sum += val.get();
				++count;
			}

			Collections.sort(commentLengths);

			if (count % 2 == 0) {
				result.setMedian(
						(commentLengths.get((int) count / 2 - 1) + commentLengths.get((int) count / 2)) / 2.0f);
			} else {
				result.setMedian(commentLengths.get((int) count / 2));
			}

			float mean = sum / count;
			float sumOfSquares = 0.0f;

			for (Float f : commentLengths) {
				sumOfSquares += (f - mean) * (f - mean);
			}

			result.setStandardDeviation((float) Math.sqrt(sumOfSquares / (count - 1)));
			context.write(key, result);
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage CommentWordCount <in> <out>");
			System.exit(2);
		}

		Job job = new Job(conf, "StackOverflow Median and Standard Deviation Comment Count");
		job.setJarByClass(MedianCommentCountJob.class);
		job.setMapperClass(MedianCommentCountMapper.class);
		job.setReducerClass(MedianCommentCountReducer.class);

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(IntWritable.class);

		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(MedianCommentCountTouple.class);

		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

}
