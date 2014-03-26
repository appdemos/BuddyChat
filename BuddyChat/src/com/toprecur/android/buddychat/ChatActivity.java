package com.toprecur.android.buddychat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseQuery.CachePolicy;
import com.parse.ParseUser;
import com.parse.PushService;

public class ChatActivity extends Activity implements OnItemClickListener {
	private static final String TAG = ChatActivity.class.getName();
	EditText chatInput;
	Button sendButton;
	ListView chatList;
	MessageAdapter mAdapter;
	String currentUserId;
	String otherUserId;
	String otherChannelName;
	String userIds[];
	MyCustomReceiver receiver;

	// MessageAdapter mAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_chat);

		// Make channel name out of other user id.
		Intent currentIntent = getIntent();
		otherUserId = currentIntent.getExtras().getString("otherUserId");
		otherChannelName = "channel" + otherUserId;
		// Set the current UserId
		currentUserId = ParseUser.getCurrentUser().getObjectId();

		// set the user ids array
		userIds = new String[] { currentUserId, otherUserId };
		
		ParseAnalytics.trackAppOpened(getIntent());

		chatInput = (EditText) findViewById(R.id.chat_input);
		chatList = (ListView) findViewById(R.id.chat_list);
		mAdapter = new MessageAdapter(this, new ArrayList<ChatMessage>());
		chatList.setAdapter(mAdapter);
		chatList.setOnItemClickListener(this);

		registerReceiver();

		// Subscribe to the channel.
		String channelName = "channel"
				+ ParseUser.getCurrentUser().getObjectId();
		channelName ="channel";
		PushService.subscribe(this, channelName, ChatActivity.class);

		updateData();

	}

	public void registerReceiver() {
		// Registering receiver
		receiver = new MyCustomReceiver(new MyHandler()); 
		registerReceiver(receiver, new IntentFilter(
				"com.toprecur.android.fitsome.UPDATE_STATUS")); // Register
		
	}
	
	public void unRegisterReceiver(){
		unregisterReceiver(receiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unRegisterReceiver();
	}

	@Override
	protected void onPause() {
		super.onPause();
		//unRegisterReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//registerReceiver();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	public void sendMessage(View v) {
		sendMessage();
	}

	void sendMessage() {
		if (chatInput.getText().length() > 0) {
			ChatMessage t = new ChatMessage();
			String message = chatInput.getText().toString();

			// Save chat message
			t.setMessage(message);
			t.setFrom(currentUserId);
			t.setTo(otherUserId);
			t.saveEventually();

			Log.d(TAG, message + "message");

			// Insert into adapter
			mAdapter.insert(t, 0);

			// send push notfication
			ParsePush push = new ParsePush();
			push.setChannel(otherChannelName);
			push.setMessage(message);

			JSONObject data = null;
			try {
				data = new JSONObject(
						"{\"action\": \"com.toprecur.android.fitsome.UPDATE_STATUS\""
								+ ",\"from\": \"" + currentUserId + "\""
								+ ",\"to\": \"" + otherUserId + "\""
								+ ",\"alert\" : \"" + message + "\""
								+ ",\"title\" : \"" + message + "\"" + "}");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			push.setData(data);
			push.sendInBackground();

			Log.d(TAG, "push sent");

			chatInput.setText("");
		}
	}

	public void updateData() {
		ParseQuery<ChatMessage> query = ParseQuery.getQuery(ChatMessage.class);
		query.whereContainedIn("to", Arrays.asList(userIds));
		query.setCachePolicy(CachePolicy.CACHE_THEN_NETWORK);
		// query.orderByDescending("createdAt");
		query.findInBackground(new FindCallback<ChatMessage>() {
			@Override
			public void done(List<ChatMessage> tasks,
					com.parse.ParseException error) {
				if (tasks != null) {
					mAdapter.clear();
					mAdapter.addAll(tasks);
				}
			}
		});
	}

	public void logout() {
		ParseUser.getCurrentUser().logOut();
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_logout:
			logout();
			break;
		case R.id.action_contacts:
			goToContacts();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void goToContacts() {
		Intent contactIntent = new Intent(this, MainActivity.class);
		startActivity(contactIntent);
		finish();
	}
	
	public class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			String message = msg.getData().getString("message");

			ChatMessage t = new ChatMessage();
			t.setMessage(message);
			mAdapter.insert(t, 0);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

	}
	


}