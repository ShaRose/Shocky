package pl.shockah.shocky.paste;

import java.util.ArrayList;
import pl.shockah.HTTPQuery;

public class ServicePastebinCom implements PasteService {
	
	private final String apiKey;
	
	public ServicePastebinCom(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public String paste(CharSequence data) {
		HTTPQuery q = new HTTPQuery("http://pastebin.com/api/api_post.php",HTTPQuery.Method.POST);
		
		StringBuilder sb = new StringBuilder(data.length()+150);
		sb.append("api_option=paste");
		sb.append("&api_dev_key="+apiKey);
		sb.append("&api_paste_private=1");
		sb.append("&api_paste_format=text");
		sb.append("&api_paste_code=");
		sb.append(data);
		
		q.connect(true,true);
		q.write(sb.toString());
		ArrayList<String> list = q.read();
		q.close();
		
		return list.get(0);
	}
}