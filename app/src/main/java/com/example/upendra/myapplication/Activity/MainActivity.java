package com.example.upendra.myapplication.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.upendra.myapplication.Model.Message;
import com.example.upendra.myapplication.R;
import com.example.upendra.myapplication.Util.Config;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.id.message;

public class MainActivity extends AppCompatActivity {

    private Button btnSend;
    private EditText inputMsg;
    private ListView listViewMessages;
    private ListView listView;

    Context context;
    RequestQueue queue;
    LayoutInflater inflater;
    private MessagesListAdapter mAdapter;
    private ArrayList<Message> messageList;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSend = (Button) findViewById(R.id.btnSend);
        inputMsg = (EditText) findViewById(R.id.inputMsg);
        listView = (ListView) findViewById(R.id.list_view);

        context = getApplicationContext();
        queue = Volley.newRequestQueue(context);
        inflater = getLayoutInflater();
        messageList = new ArrayList<>();

        mAdapter = new MessagesListAdapter(this, messageList);
        listView.setAdapter(mAdapter);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage()
    {
        String msg = inputMsg.getText().toString().trim();

        if( msg.length()==0) {
            Toast.makeText(getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!networkIsAvailable(context))
        {
            Toast.makeText(getApplicationContext(), "Connect to internet", Toast.LENGTH_SHORT).show();
            return;
        }
        inputMsg.setText("");

        Message message = new Message();
        message.setSuccess(1);
        message.setChatBotName(Config.name);
        message.setChatBotID(Integer.parseInt(Config.chatBotID));
        message.setMessage(msg);
        message.setEmotion(null);
        message.setSelf(true);
        messageList.add(message);
        mAdapter.notifyDataSetChanged();

        msg = msg.replace(" ","+");
        sendMessageToServer(msg);
    }

    private void sendMessageToServer(final String msg)
    {
        String url =Config.URL+"?apiKey="+Config.apiKey+"&message="+msg+"&chatBotID="+Config.chatBotID+"&externalID="+Config.externalID;
        JsonObjectRequest jsonObjectRequest= new JsonObjectRequest(Request.Method.GET, url,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null && response.length() > 0) {
                            try {
                                if(response.getInt("success")==1)
                                {
                                    Message message = new Message();
                                    JSONObject m = response.getJSONObject("message");
                                    message.setChatBotName(m.optString("chatBotName", ""));
                                    message.setChatBotID(m.optInt("chatBotID"));
                                    message.setMessage(m.optString("message", ""));
                                    message.setEmotion(m.optString("emotions",null));
                                    message.setSelf(false);

                                    appendMessage(message);
                                }
                                else
                                {
                                    String error=response.getString("errorMessage");
                                    Toast.makeText(getApplicationContext(), "Error: "+error, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // errors
                            Toast.makeText(getApplicationContext(), "Retry Later ", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                        Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();

                        /*if (networkResponse != null)
                        {
                            // HTTP Status Code: 401 Unauthorized
                            Log.d("status", " " + networkResponse.statusCode);
                            if(networkResponse.statusCode==404)
                                Toast.makeText(context,"User Not Found.",Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(context,"Try Later.",Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(context,"Try Again.",Toast.LENGTH_SHORT).show();
                        }*/
                     /*   if (error.getClass().toString().equals("class com.android.volley.NoConnectionError"))
                            Toast.makeText(context,"Please connect to internet.",Toast.LENGTH_SHORT).show();
                        else if (error.getClass().toString().equals("class com.android.volley.AuthFailureError"))
                            Toast.makeText(context,"\"Request cannot be completed. Try again later.",Toast.LENGTH_SHORT).show();
                        else if (error.getClass().toString().equals("class com.android.volley.ServerError"))
                            Toast.makeText(context,"Request cannot be completed. Try again later.",Toast.LENGTH_SHORT).show();
                        else if (error.getClass().toString().equals("class com.android.volley.TimeoutError"))
                            Toast.makeText(context,"Request cannot be completed. Try again later.",Toast.LENGTH_SHORT).show();
                        else {
                            // define other errors
                            Toast.makeText(context,"error",Toast.LENGTH_SHORT).show();
                        }
                        Log.d("check", "" + error.getClass().toString());*/
                    }
                }
        );

        // disabling retry policy so that it won't make
        // multiple http calls
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        jsonObjectRequest.setRetryPolicy(policy);
        jsonObjectRequest.setTag(TAG);
        queue.add(jsonObjectRequest);
    }

    public class MessagesListAdapter extends BaseAdapter {

        private Context context;
        private List<Message> messagesItems;

        public MessagesListAdapter(Context context, List<Message> navDrawerItems) {
            this.context = context;
            this.messagesItems = navDrawerItems;
        }

        @Override
        public int getCount() {
            return messagesItems.size();
        }

        @Override
        public Object getItem(int position) {
            return messagesItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Message m = messagesItems.get(position);

            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            if (messagesItems.get(position).isSelf()) {
                convertView = mInflater.inflate(R.layout.list_item_message_right,
                        null);
            } else {
                convertView = mInflater.inflate(R.layout.list_item_message_left,
                        null);
            }

            TextView lblFrom = (TextView) convertView.findViewById(R.id.lblMsgFrom);
            TextView txtMsg = (TextView) convertView.findViewById(R.id.txtMsg);

            txtMsg.setText(m.getMessage());
            lblFrom.setText(m.getChatBotName());

            return convertView;
        }
    }

    private void appendMessage(final Message m) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                messageList.add(m);
                mAdapter.notifyDataSetChanged();
                // Playing device's notification
                playBeep();
            }
        });
    }


    private boolean networkIsAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public void playBeep() {

        try {
            Uri notification = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
                    notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop () {
        super.onStop();
        if (queue != null) {
            queue.cancelAll(TAG);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
