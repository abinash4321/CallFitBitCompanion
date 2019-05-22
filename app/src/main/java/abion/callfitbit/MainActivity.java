package abion.callfitbit;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    ServerSocket httpServerSocket;
    StringBuilder StoreContacts ;
    StringBuilder SelectedContacts;
    Cursor cursor;
    String name, phonenumber;
    String prevName;
    String prevNumber;
    public  static final int RequestPermissionCode  = 1 ;
    private static final int MAKE_CALL_PERMISSION_REQUEST_CODE = 2;
    private String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS};
    TextView status;
    TextView note;
    String[] listItems;
    boolean[] checkedItems;
    ArrayList<Integer> mUserItems = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView)findViewById(R.id.text_view);
        note = (TextView)findViewById(R.id.text_view2);
        SelectedContacts = new StringBuilder();
        StoreContacts = new StringBuilder();
        //infoIp.setText(getIpAddress() + ":"+ HttpServerThread.HttpServerPORT + "\n");
        //HttpServerThread httpServerThread = new HttpServerThread();
        //httpServerThread.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(arePermissionsEnabled()){
                //                    permissions granted, continue flow normally
            }else{
                requestMultiplePermissions();
            }
        }
        note.setText("Notes:"+"\n"+"* This app needs to be run in background to make call and sync contacts"
        +"\n"+"* This app doesn't save the contacts."+"\n"+"Please select contacts first before click on Start.(This is applicable if you want to sync contacts");
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean arePermissionsEnabled(){
        for(String permission : permissions){
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestMultiplePermissions(){
        List<String> remainingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(permission);
            }
        }
        requestPermissions(remainingPermissions.toArray(new String[remainingPermissions.size()]), 101);
    }
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 101){
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    if(shouldShowRequestPermissionRationale(permissions[i])){
                        /*new AlertDialog.Builder(this)
                                .setMessage("Your error message here")
                                .setPositiveButton("Allow", requestMultiplePermissions)
                                .setNegativeButton("Cancel", dialog.dismiss)
                                .create()
                                .show();*/
                        showMessageOKCancel("Permission required to start service",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                requestMultiplePermissions();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                dialog.dismiss();
                                                break;
                                        }


                                    }
                                });
                    }
                    return;
                }
            }
            //all is good, continue flow
        }
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Ok", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    public void startService(View view){
        HttpServerThread httpServerThread = new HttpServerThread();
        httpServerThread.start();
        //EnableRuntimePermission();
        //EnableRuntimeCallPermission();
        status.setText("Service Started...");
    }
    public void stopService(View view){
        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
                status.setText("Service Stopped...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void selectContacts(View view){
        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);
        while (cursor.moveToNext()) {

            name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            phonenumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            if(name.equals(prevName)) {
                if(!phonenumber.equals(prevNumber)) {
                    StoreContacts.append(name + ":" + phonenumber +",");


                }
            }
            else if(name.equals(prevName) && phonenumber.equals(prevNumber)) {

            }
            else{

                StoreContacts.append(name + ":" + phonenumber +",");

            }

            prevName = name;
            prevNumber = phonenumber;

        }

        cursor.close();
        listItems = StoreContacts.toString().split(",");
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle("Select Contact to Sync");
        mBuilder.setMultiChoiceItems(listItems, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                if(isChecked){
                    mUserItems.add(position);
                }else{
                    mUserItems.remove((Integer.valueOf(position)));
                }
            }
        });

        mBuilder.setCancelable(false);
        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                    String item = "";
                    for (int i = 0; i < mUserItems.size(); i++) {
                        item = item + listItems[mUserItems.get(i)];
                        if (i != mUserItems.size() - 1) {
                            item = item + ", ";
                        }
                    }
                    SelectedContacts.append(item);
            }
        });


        mBuilder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        mBuilder.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                for (int i = 0; i < checkedItems.length; i++) {
                    checkedItems[i] = false;
                    mUserItems.clear();
                    SelectedContacts.append("");
                }
            }
        });


        AlertDialog mDialog = mBuilder.create();
        mDialog.show();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private class HttpServerThread extends Thread {

        static final int HttpServerPORT = 8888;

        @Override
        public void run() {
            Socket socket = null;
            try {
                httpServerSocket = new ServerSocket(HttpServerPORT);

                while(true){
                    socket = httpServerSocket.accept();
                    HttpResponseThread httpResponseThread =
                            new HttpResponseThread(
                                    socket,
                                    "hello");
                    httpResponseThread.start();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }


    }
    public void EnableRuntimePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this,
                Manifest.permission.READ_CONTACTS))
        {

            Toast.makeText(MainActivity.this,"This permission allows us to Access CONTACTS", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode);

        }

    }
    public void EnableRuntimeCallPermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this,
                Manifest.permission.CALL_PHONE))
        {

            Toast.makeText(MainActivity.this,"This permission required to make phone call", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.CALL_PHONE}, MAKE_CALL_PERMISSION_REQUEST_CODE);

        }
    }



    private class HttpResponseThread extends Thread {

        Socket socket;
        String h1;

        HttpResponseThread(Socket socket, String msg){
            this.socket = socket;
            h1 = msg;
            //StoreContacts = new StringBuilder();

        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void run() {
            BufferedReader is;
            PrintWriter os;
            String request;
            StringBuilder finalCntList = new StringBuilder();
            //JSONArray jContactData = new JSONArray();

            try {
                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = is.readLine();
                if(request.contains("contacts"))
                {

                    int totalLen = 0, nameLen = 0, numberLen = 0;
                    String [] finallist = SelectedContacts.toString().split(",");
                    if(finallist.length > 100) {
                        Toast.makeText(MainActivity.this, mUserItems.size() + " items selected. Maximum 100 items can be selected", Toast.LENGTH_LONG);
                    }
                    else {
                        for (int i = 0; i < finallist.length; i++) {
                            String cntname = finallist[i].split(":")[0];
                            nameLen = cntname.length() < 31 ? cntname.length() : 30;
                            if (nameLen == 30) {
                                cntname = cntname.substring(0, 30);
                            }
                            String cntphonenumber = finallist[i].split(":")[1];
                            numberLen = cntphonenumber.length() < 21 ? cntphonenumber.length() : 20;
                            if (numberLen == 20) {
                                cntphonenumber = cntphonenumber.substring(0, 20);
                            }
                            totalLen++;
                            finalCntList.append(String.format("%02d", nameLen) + cntname + String.format("%02d", numberLen) + cntphonenumber);
                        }
                        finalCntList.insert(0, String.format("%04d", totalLen));
                    }
                }
                if(request.contains("call"))
                {
                    String qs = request.split("/")[1];
                    String num = qs.split("&")[1].split(" ")[0];
                    //String s = num[0];
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:"+num));
                    if(arePermissionsEnabled()) {
                        startActivity(callIntent);
                    }

                }
                os = new PrintWriter(socket.getOutputStream(), true);
                //String response = StoreContacts.toString();
                String response = finalCntList.toString();

                os.print("HTTP/1.0 200" + "\r\n");
                os.print("Content type: text/plain" + "\r\n");
                os.print("Content length: " + response.length() + "\r\n");
                os.print("\r\n");
                os.print(response);
                os.flush();
                socket.close();



            } catch (IOException e) {
                // TODO Auto-generated catch block
                Toast.makeText(MainActivity.this,"Select Contacts First",Toast.LENGTH_LONG);
                e.printStackTrace();
            }

            return;
        }
    }
}
