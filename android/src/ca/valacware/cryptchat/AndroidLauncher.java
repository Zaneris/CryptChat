package ca.valacware.cryptchat;

import android.content.Intent;
import android.os.Bundle;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.badlogic.gdx.backends.android.AndroidApplication;

import java.io.File;

public class AndroidLauncher extends AndroidApplication implements NativeInterface {
	private EditText editText;
	private String text;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cryptchat);
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.cryptchat);
		final CryptChat cryptChat = new CryptChat(this);
		View chatView = initializeForView(cryptChat);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.ABOVE, R.id.editText);
		chatView.setLayoutParams(params);
		layout.addView(chatView);

		editText = (SendEditText)findViewById(R.id.editText);
		editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
				if (event==null) {
					if (actionId== EditorInfo.IME_ACTION_DONE);
					else if (actionId== EditorInfo.IME_ACTION_NEXT);
					else if (actionId==EditorInfo.IME_ACTION_SEND);
					else return false;
				} else if (actionId==EditorInfo.IME_NULL) {
					if (event.getAction()==KeyEvent.ACTION_DOWN);
					else return true;
				} else  return false;

				synchronized (CryptChat.textLock) {
					cryptChat.textBox = editText.getText().toString();
				}
				editText.getText().clear();
				return true;   // Consume the event
			}
		});
		editText.setFocusableInTouchMode(true);
		editText.setFocusable(true);
		editText.requestFocus();
	}

	@Override
	public void setInputBox(String str) {
		text = str;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				editText.setText(text);
				editText.setSelection(editText.length());
				editText.requestFocus();
			}
		});
	}

	@Override
	public void setInputTextAutoComplete() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
						| InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
				editText.requestFocus();
			}
		});
	}

	@Override
	public void setInputText() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
						| InputType.TYPE_TEXT_FLAG_MULTI_LINE);
				editText.requestFocus();
			}
		});
	}

	@Override
	public void setInputNumeric() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				editText.setInputType(InputType.TYPE_CLASS_NUMBER);
				editText.requestFocus();
			}
		});
	}

	@Override
	public String copyClipboard() {
		return null;
	}

	@Override
	public void shareText(String share) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, share);
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent, "Share with..."));
	}

	@Override
	public void setInputPassword() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				editText.setText("");
				editText.setInputType(InputType.TYPE_CLASS_TEXT |
						InputType.TYPE_TEXT_VARIATION_PASSWORD);
				editText.requestFocus();
			}
		});
	}

	@Override
	public File getFile(String file) {
		return new File(this.getFilesDir(), file);
	}

	@Override
	public boolean isMobile() {
		return true;
	}
}
