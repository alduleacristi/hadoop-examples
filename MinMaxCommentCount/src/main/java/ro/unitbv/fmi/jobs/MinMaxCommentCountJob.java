package ro.unitbv.fmi.jobs;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import ro.unitbv.fmi.help.MinMaxCountTouple;

public class MinMaxCommentCountJob {
	public static class MinMaxCommentCountMapper extends Mapper<Object, Text, Text, MinMaxCountTouple> {
		private Text outUserId = new Text();
		private MinMaxCountTouple outTuple = new MinMaxCountTouple();

		private final static SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			Map<String, String> parsed = MRDPUtils.xmlToMap(value.toString());

			String strDate = parsed.get("CreationDate");
			String userId = parsed.get("UserId");

			try {
				if (strDate == null || userId == null) {
					return;
				}

				Date creationDate = frmt.parse(strDate);
				outTuple.setMin(creationDate);
				outTuple.setMax(creationDate);
				outTuple.setCount(1);

				outUserId.set(userId);
				context.write(outUserId, outTuple);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

	public static class MinMaxCommentCountReducer extends Reducer<Text, MinMaxCountTouple, Text, MinMaxCountTouple> {
		private MinMaxCountTouple result = new MinMaxCountTouple();

		public void reduce(Text key, Iterable<MinMaxCountTouple> values, Context context)
				throws IOException, InterruptedException {
			result.setMax(null);
			result.setMin(null);
			result.setCount(0);
			int sum = 0;

			for (MinMaxCountTouple val : values) {
				if (result.getMin() == null || val.getMin().compareTo(result.getMin()) < 0) {
					result.setMin(val.getMin());
				}

				if (result.getMax() == null || val.getMax().compareTo(result.getMax()) > 0) {
					result.setMax(val.getMax());
				}

				sum += val.getCount();
			}

			result.setCount(sum);
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

		Job job = new Job(conf, "StackOverflow Min Max Comment Count");
		job.setJarByClass(MinMaxCommentCountJob.class);
		job.setMapperClass(MinMaxCommentCountMapper.class);
		job.setReducerClass(MinMaxCommentCountReducer.class);
		job.setCombinerClass(MinMaxCommentCountReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(MinMaxCountTouple.class);

		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

}
