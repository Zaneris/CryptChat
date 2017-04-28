package ca.valacware.cryptchat;

import java.io.File;

interface NativeInterface {
	void setInputBox(String str);
	void setInputPassword();
	void setInputTextAutoComplete();
	void setInputText();
	void setInputNumeric();
	String copyClipboard();
	void shareText(String share);
	File getFile(String file);
	boolean isMobile();
}