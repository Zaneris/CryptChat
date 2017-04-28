package ca.valacware.cryptchat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * Created by Ted on 2017-01-16.
 */
public class SendEditText extends EditText {
	public SendEditText(Context context) {
		super(context);
	}

	public SendEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SendEditText(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs)
	{
		InputConnection conn = super.onCreateInputConnection(outAttrs);
		outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
		return conn;
	}
}
