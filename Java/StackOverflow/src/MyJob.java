import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class MyJob {

    public static class MyMapper
            extends Mapper<Object, Text, Text, IntWritable>{

        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            String xml = value.toString();
            Row row;
            try {
                Unmarshaller unmarshaller = JAXBContext.newInstance(Row.class).createUnmarshaller();
                StringReader reader = new StringReader(xml);
                row = (Row) unmarshaller.unmarshal(reader);
            } catch (Exception exception) {
                return;
            }

            LocalDate date = row.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long days = ChronoUnit.DAYS.between(date, LocalDate.now());

            String group = (double)row.getReputation()/(double)days > 1 ? "active" : "inactive";

            context.write(new Text(group), new IntWritable(1));
        }
    }

    public static class  MyReducer
            extends Reducer<Text,IntWritable,Text,IntWritable> {
        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context
        ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "active users");
        job.setJarByClass(MyJob.class);
        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}