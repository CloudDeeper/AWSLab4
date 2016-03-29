package com.team14.lab4;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;

import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
 class MyLocationListener implements LocationListener {
    @Override
    public void onLocationChanged(Location loc) {
        if (loc != null) {
            Log.e(String.valueOf(loc.getLatitude()), String.valueOf(loc.getLongitude()));
            MainActivity.updateLoc(loc);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}

public class MainActivity extends AppCompatActivity {
    private static MobileAnalyticsManager analytics;
    CognitoCachingCredentialsProvider credentialsProvider;
    TextView aniText;
    Animation aniMsgIn;
    Animation aniMsgOut;
    private final static Object mutex = new Object();
    static Location globalLoc;
    public static void updateLoc(Location loc) {
        synchronized(mutex) {
            globalLoc = loc;
        }
    }
    public void transferObserverListener(TransferObserver transferObserver){

        transferObserver.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.e("statechange", state+"");
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent/bytesTotal * 100);
                Log.e("percentage",percentage +"");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("error","error");
            }

        });
    }
    void uploadS3(String path) {
        AmazonS3 s3 = new AmazonS3Client(
                credentialsProvider
        );
        s3.setRegion(Region.getRegion(Regions.DEFAULT_REGION));
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
        File file = new File(path);
        TransferManager tm = new TransferManager(credentialsProvider);
        Upload upload = tm.upload(
//        TransferObserver observer = transferUtility.upload(
                "mobile-analytics-03-28-2016-8d177c5401e946f0ad46da176068b58f",
                "upload/" + file.getName(),    // The key for the uploaded object
                file // The file where the data to upload exists
                //      metadata
        );
        try {
            upload.waitForCompletion();
        }catch (Exception e) {
            e.printStackTrace();
        }
//        long dd = observer.getBytesTotal();
//        long dd2 = observer.getBytesTransferred();
       // aniText.setText(observer.getState().toString());
        aniText.setText(path);
        aniText.startAnimation(aniMsgOut);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode==1 && resultCode == RESULT_OK && null != data) {
            super.onActivityResult(requestCode, resultCode, data);
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            uploadS3(picturePath);
        }
    }
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Amazon Cognito credentials provider
        //  you need to add two button in your activity
        //  with name "button", "button2" respectively
        aniText = (TextView) findViewById(R.id.textView);
        aniMsgIn = new AlphaAnimation(0.0f, 1.0f);
        aniMsgOut = new AlphaAnimation(1.0f, 0.0f);
        aniMsgIn.setDuration(3000);
            aniMsgOut.setDuration(3000);
            aniMsgOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                }

                public void onAnimationStart(Animation a) { }
            public void onAnimationRepeat(Animation a) { }
        });

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:08ad5949-25e2-4c1d-b9bb-7ef5e71f1c26", // Amazon Cognito Identity Pool ID
                Regions.US_EAST_1 // Region: N.Virgina
        );
        Button SQSButton = (Button) findViewById(R.id.button);
        Button downloadButton = (Button) findViewById(R.id.button3);
        final Button MAButton = (Button) findViewById(R.id.button2);
        final Intent imgIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        /***************************************************************/

        /********************** Lab 4.1 - MobileAnalytics **********************/
        // Initialize the Amazon MobileAnalytics

        //  you will need permission to check ACCESS_NETWORK_STATE
        //  remember to update your AndroidManifest.xml

        try {
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    this.getApplicationContext(),
                    "ce9fe21089944d3fb60377b90ab063d3", //Amazon Mobile Analytics App ID
                    "us-east-1:08ad5949-25e2-4c1d-b9bb-7ef5e71f1c26" //Amazon Cognito Identity Pool ID
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
                        .withAttribute("UPLOAD", dateString);
                analytics.getEventClient().recordEvent(clientClickEvent);
                analytics.getEventClient().submitEvents();
            }
        });
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
                        startActivityForResult(imgIntent, 1);
                    }
                });
                jobThread.start();
                MAButton.performClick();
            }
        });
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AmazonS3 s3 = new AmazonS3Client(
                        credentialsProvider
                );
                s3.setRegion(Region.getRegion(Regions.US_EAST_1));
                TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
                File sdPath = null;
                if (Environment.getExternalStorageState().equals(
                        android.os.Environment.MEDIA_MOUNTED))
                {
                    sdPath = Environment.getExternalStorageDirectory();
                }
                File file = new File( sdPath.toString() + "/123.jpg");
                TransferObserver observer = transferUtility.download(
                   "mobile-analytics-03-28-2016-8d177c5401e946f0ad46da176068b58f",
                              "upload/test.jpg",    // The key for the uploaded object
                                file // The file where the data to upload exists
                        //      metadata
                );
                System.out.print(observer.getState().toString());
                transferObserverListener(observer);
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                String dateString = dateFormat.format(date);
                AnalyticsEvent clientClickEvent = analytics.getEventClient().createEvent("ClickCount")
                        .withAttribute("Download", dateString);
                analytics.getEventClient().recordEvent(clientClickEvent);
                analytics.getEventClient().submitEvents();
            }
        });
        /***********************************************************************/
        /********************** Lab 4.2 Kinesis **********************/
        Button kinBtn = (Button) findViewById(R.id.button4);

        final AmazonKinesisClient kinesisClient = new AmazonKinesisClient(credentialsProvider);
        kinesisClient.setRegion(Region.getRegion(Regions.US_EAST_1));
        LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locLis = new MyLocationListener();
        Location loc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Log.e(String.valueOf(loc.getLatitude()), String.valueOf(loc.getLongitude()));
        updateLoc(loc);
        if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1000);
        }
        locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locLis);
        kinBtn.setOnClickListener(new View.OnClickListener() {
            // Initialize Kinesis Stream Client
            @Override
            public void onClick(View v) {

            //  if your Kinesis stream is not in Oregon,
            //  you will have to change this line
            // Kinesis testing

            Thread kinesisThreadProducer = new Thread(new Runnable() {

                    @Override
                    public void run () {
                        try {
                            while (true) {
                                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                Date date = new Date();
                                String dateString = dateFormat.format(date);
                                PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
                                putRecordsRequest.setStreamName("lab4");
                                // produce 500 records to stream per 0.5 secs
                                List<PutRecordsRequestEntry> putRecordsRequestEntryList = new ArrayList<>();
                                PutRecordsRequestEntry putRecordsRequestEntry = new PutRecordsRequestEntry();
                                String data = dateString + " " + String.valueOf(globalLoc.getLatitude())
                                        + "N " + String.valueOf(globalLoc.getLongitude()) + "E";
                                putRecordsRequestEntry.setData(ByteBuffer.wrap(data.getBytes()));
                                putRecordsRequestEntry.setPartitionKey(data);
                                putRecordsRequestEntryList.add(putRecordsRequestEntry);
                                putRecordsRequest.setRecords(putRecordsRequestEntryList);
                                kinesisClient.putRecords(putRecordsRequest);
                                Thread.sleep(2000);
                                Log.e("Info", "kinesis produce: " + data);
                            }
                        } catch (InterruptedException e) {
                            // if interrupt, do nothing and terminate the thread
                        }
                    }
                });
                kinesisThreadProducer.start();
                /*
        Thread kinesisThreadConsumer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
                    describeStreamRequest.setStreamName("lab4");
                    describeStreamRequest.setExclusiveStartShardId(null);
                    describeStreamRequest.setLimit(1);
                    DescribeStreamResult describeStreamResult = kinesisClient.describeStream(describeStreamRequest);
                    List<Shard> shards = describeStreamResult.getStreamDescription().getShards();
                    Shard shard = shards.get(0);
                    GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest();
                    getShardIteratorRequest.setStreamName("lab4");
                    getShardIteratorRequest.setShardId(shard.getShardId());
                    getShardIteratorRequest.setShardIteratorType("TRIM_HORIZON");
                    GetShardIteratorResult getShardIteratorResult = kinesisClient.getShardIterator(getShardIteratorRequest);
                    String shardIterator = getShardIteratorResult.getShardIterator();
                    // consumes 2000 records from stream per 2 secs
                    while (true) {
                        GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
                        getRecordsRequest.setLimit(3000);
                        getRecordsRequest.setShardIterator(shardIterator);
                        GetRecordsResult getRecordsResult = kinesisClient.getRecords(getRecordsRequest);
                        Thread.sleep(3000);
                        System.out.println("Kinesis consume: " + getRecordsResult.toString());
                        shardIterator = getRecordsResult.getNextShardIterator();
                    }
                } catch (InterruptedException e) {
                    // if interrupt, do nothing and terminate the thread
                }
            }
        });
        kinesisThreadConsumer.start();
        */
            }
        });
        /*************************************************************/
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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

    /*
    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.team14.lab4/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.team14.lab4/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }*/
    /***********************************************************************/
}
