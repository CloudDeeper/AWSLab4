package com.example.chihmin.awslab;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;

import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.example.chihmin.awslab.R;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.GetRecordsRequest;
import com.amazonaws.services.kinesis.model.GetRecordsResult;
import com.amazonaws.services.kinesis.model.GetShardIteratorRequest;
import com.amazonaws.services.kinesis.model.GetShardIteratorResult;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.Shard;
import com.amazonaws.services.sqs.AmazonSQSClient;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static MobileAnalyticsManager analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Amazon Cognito credentials provider
        final CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:ff7e1f6d-07d1-4e8a-becc-3eebcb81746e", // Amazon Cognito Identity Pool ID
                Regions.US_EAST_1 // Region: N.Virgina
        );

        //  you need to add two button in your activity
        //  with name "button", "button2" respectively

        Button SQSButton = (Button) findViewById(R.id.button);
        Button MAButton = (Button) findViewById(R.id.button2);
        /*
        /********************** Lab 4.1 - Cognito **********************/
        // Cognito testing
        SQSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  you will need permission to access INTERNET
                //  remember to update your AndroidManifest.xml

                // network operation should never run on main thread, create a new thread!
                Thread jobThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AmazonSQSClient sqsClient = new AmazonSQSClient(credentialsProvider);
                        String sqsQueueURL = "https://sqs.us-west-2.amazonaws.com/605921438370/Input";
                        String sqsMessage =
                                "https://us-east-1-aws-training.s3.amazonaws.com/arch-static-assets/static/20120728-DSC01265-L.jpg\n" +
                                        "https://us-east-1-aws-training.s3.amazonaws.com/arch-static-assets/static/20120728-DSC01267-L.jpg\n" +
                                        "https://us-east-1-aws-training.s3.amazonaws.com/arch-static-assets/static/20120728-DSC01292-L.jpg\n" +
                                        "https://us-east-1-aws-training.s3.amazonaws.com/arch-static-assets/static/20120728-DSC01315-L.jpg\n" +
                                        "https://us-east-1-aws-training.s3.amazonaws.com/arch-static-assets/static/20120728-DSC01337-L.jpg";

                        sqsClient.sendMessage(sqsQueueURL, sqsMessage);
                    }
                });
                jobThread.start();
            }
        });
        /***************************************************************/

        /********************** Lab 4.1 - MobileAnalytics **********************/
        // Initialize the Amazon MobileAnalytics

        //  you will need permission to check ACCESS_NETWORK_STATE
        //  remember to update your AndroidManifest.xml

        try {
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    this.getApplicationContext(),
                    "ce9fe21089944d3fb60377b90ab063d3", //Amazon Mobile Analytics App ID
                    "us-east-1:ff7e1f6d-07d1-4e8a-becc-3eebcb81746e" //Amazon Cognito Identity Pool ID
            );
        } catch (InitializationException ex) {
            Log.e(this.getClass().getName(), "Failed to initialize Amazon Mobile Analytics", ex);
        }

        // MobileAnalytics testing
        MAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                String dateString = dateFormat.format(date);
                AnalyticsEvent clientClickEvent = analytics.getEventClient().createEvent("ClickCount")
                        .withAttribute("MAClick", dateString);
                analytics.getEventClient().recordEvent(clientClickEvent);
                analytics.getEventClient().submitEvents();
            }
        });
        /***********************************************************************/

        /********************** Lab 4.2 Kinesis **********************/
        // Initialize Kinesis Stream Client
        final AmazonKinesisClient kinesisClient = new AmazonKinesisClient(credentialsProvider);

        //  if your Kinesis stream is not in Oregon,
        //  you will have to change this line

        kinesisClient.setRegion(Region.getRegion(Regions.US_EAST_1));

        // Kinesis testing
        Thread kinesisThreadProducer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();
                        String dateString = dateFormat.format(date);
                        PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
                        putRecordsRequest.setStreamName("CP_mobilestream");
                        // produce 500 records to stream per 0.5 secs
                        List<PutRecordsRequestEntry> putRecordsRequestEntryList = new ArrayList<>();
                        for (int i = 0; i < 500; ++i) {
                            PutRecordsRequestEntry putRecordsRequestEntry = new PutRecordsRequestEntry();
                            String data = "[" + i + "]: " + dateString;
                            putRecordsRequestEntry.setData(ByteBuffer.wrap(data.getBytes()));
                            putRecordsRequestEntry.setPartitionKey(data);
                            putRecordsRequestEntryList.add(putRecordsRequestEntry);
                        }
                        putRecordsRequest.setRecords(putRecordsRequestEntryList);
                        kinesisClient.putRecords(putRecordsRequest);
                        Thread.sleep(500);
                        System.out.println("kinesis produce: " + dateString);
                    }
                } catch (InterruptedException e) {
                    // if interrupt, do nothing and terminate the thread
                }
            }
        });

        Thread kinesisThreadConsumer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
                    describeStreamRequest.setStreamName("CP_mobilestream");
                    describeStreamRequest.setExclusiveStartShardId(null);
                    describeStreamRequest.setLimit(1);
                    DescribeStreamResult describeStreamResult = kinesisClient.describeStream(describeStreamRequest);
                    List<Shard> shards = describeStreamResult.getStreamDescription().getShards();
                    Shard shard = shards.get(0);
                    GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest();
                    getShardIteratorRequest.setStreamName("CP_mobilestream");
                    getShardIteratorRequest.setShardId(shard.getShardId());
                    getShardIteratorRequest.setShardIteratorType("TRIM_HORIZON");
                    GetShardIteratorResult getShardIteratorResult = kinesisClient.getShardIterator(getShardIteratorRequest);
                    String shardIterator = getShardIteratorResult.getShardIterator();
                    // consumes 2000 records from stream per 2 secs
                    while (true) {
                        GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
                        getRecordsRequest.setLimit(2000);
                        getRecordsRequest.setShardIterator(shardIterator);
                        GetRecordsResult getRecordsResult = kinesisClient.getRecords(getRecordsRequest);
                        Thread.sleep(2000);
                        System.out.println("Kinesis consume: " + getRecordsResult.toString());
                        shardIterator = getRecordsResult.getNextShardIterator();
                    }
                } catch (InterruptedException e) {
                    // if interrupt, do nothing and terminate the thread
                }
            }
        });

        kinesisThreadProducer.start();
        kinesisThreadConsumer.start();
        /*************************************************************/

    }

    /**********************
     * Lab 4.1 - MobileAnalytics
     **********************/
    @Override
    protected void onPause() {
        super.onPause();
        if (analytics != null) {
            analytics.getSessionClient().pauseSession();
            analytics.getEventClient().submitEvents();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (analytics != null) {
            analytics.getSessionClient().resumeSession();
        }
    }
    /***********************************************************************/
}
